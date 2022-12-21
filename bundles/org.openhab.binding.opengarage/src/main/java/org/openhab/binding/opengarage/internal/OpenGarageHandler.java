/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.opengarage.internal;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.opengarage.internal.api.ControllerVariables;
import org.openhab.binding.opengarage.internal.api.Enums.OpenGarageCommand;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenGarageHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Paul Smedley - Initial contribution
 * @author Dan Cunningham - Minor improvements to vehicle state and invert option
 */
@NonNullByDefault
public class OpenGarageHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OpenGarageHandler.class);

    private long refreshInterval;
    private long transitionTimeSecs;

    private @NonNullByDefault({}) OpenGarageWebTargets webTargets;
    private @Nullable VariableDelayPoller poller;
    private Instant lastTransition;
    private String lastTransitionText;

    public @NonNullByDefault({}) String doorOpeningState;
    public @NonNullByDefault({}) String doorOpenState;
    public @NonNullByDefault({}) String doorClosedState;
    public @NonNullByDefault({}) String doorClosingState;

    public OpenGarageHandler(Thing thing) {
        super(thing);
        this.lastTransition = Instant.MIN;
        this.lastTransitionText = "";
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            logger.debug("Received command {} for thing '{}' on channel {}", command, thing.getUID().getAsString(),
                    channelUID.getId());
            var maybeInvert = getInverter(channelUID.getId());
            switch (channelUID.getId()) {
                case OpenGarageBindingConstants.CHANNEL_OG_STATUS:
                case OpenGarageBindingConstants.CHANNEL_OG_STATUS_SWITCH:
                case OpenGarageBindingConstants.CHANNEL_OG_STATUS_ROLLERSHUTTER:
                    if (command.equals(StopMoveType.STOP) || command.equals(StopMoveType.MOVE)) {
                        changeStatus(OpenGarageCommand.CLICK);
                    } else {
                        var doorOpen = command.equals(OnOffType.ON) || command.equals(UpDownType.UP);
                        changeStatus(maybeInvert.apply(doorOpen) ? OpenGarageCommand.OPEN : OpenGarageCommand.CLOSE);
                        this.lastTransition = Instant.now();
                        this.lastTransitionText = doorOpen ? this.doorOpeningState : this.doorClosingState;
                        this.poller.reschedule(0, true);
                    }
                    break;
                default:
            }
        } catch (OpenGarageCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        }
    }

    @Override
    public void initialize() {
        var config = getConfigAs(OpenGarageConfiguration.class);
        logger.debug("config.hostname = {}, refresh = {}, port = {}", config.hostname, config.refresh, config.port);
        if (config.hostname == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Hostname/IP address must be set");
        } else {
            webTargets = new OpenGarageWebTargets(config.hostname, config.port, config.password);
            this.refreshInterval = config.refresh;
            this.transitionTimeSecs = config.door_transition_time_seconds;

            this.doorClosedState = config.door_closed_state;
            this.doorClosingState = config.door_closing_state;
            this.doorOpeningState = config.door_opening_state;
            this.doorOpenState = config.door_open_state;

            this.poller = new VariableDelayPoller(scheduler, this::poll, refreshInterval);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.poller != null) {
            this.poller.stop(true);
        }
    }

    private long poll() {
        var pollAgainIn = this.refreshInterval;
        try {
            logger.debug("Polling for state");
            pollAgainIn = pollStatus();
        } catch (IOException e) {
            logger.warn("Could not connect to OpenGarage controller", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Unexpected error connecting to OpenGarage controller", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
        return pollAgainIn;
    }

    private long pollStatus() throws IOException {
        ControllerVariables controllerVariables = webTargets.getControllerVariables();
        var lastTransitionAgoSecs = Duration.between(lastTransition, Instant.now()).getSeconds();
        var inTransition = lastTransitionAgoSecs < this.transitionTimeSecs;
        if (controllerVariables != null) {
            updateStatus(ThingStatus.ONLINE);
            updateState(OpenGarageBindingConstants.CHANNEL_OG_DISTANCE,
                    new QuantityType<>(controllerVariables.dist, MetricPrefix.CENTI(SIUnits.METRE)));
            var maybeInvert = getInverter(OpenGarageBindingConstants.CHANNEL_OG_STATUS_SWITCH);

            if ((controllerVariables.door != 0) && (controllerVariables.door != 1)) {
                logger.warn("Received unknown door value: {}", controllerVariables.door);
            } else {
                var doorOpen = controllerVariables.door == 1;
                var onOff = maybeInvert.apply(doorOpen) ? OnOffType.ON : OnOffType.OFF;
                var upDown = doorOpen ? UpDownType.UP : UpDownType.DOWN;
                var contact = doorOpen ? OpenClosedType.OPEN : OpenClosedType.CLOSED;

                String transitionText;
                if (inTransition) {
                    transitionText = this.lastTransitionText;
                } else {
                    transitionText = doorOpen ? this.doorOpenState : this.doorClosedState;
                }
                if (!inTransition) {
                    updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS, onOff); // deprecated channel
                    updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS_SWITCH, onOff);
                }
                updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS_ROLLERSHUTTER, upDown);
                updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS_CONTACT, contact);
                updateState(OpenGarageBindingConstants.CHANNEL_OG_STATUS_TEXT, new StringType(transitionText));
            }

            switch (controllerVariables.vehicle) {
                case 0:
                    updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE, new StringType("No vehicle detected"));
                    break;
                case 1:
                    updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE, new StringType("Vehicle detected"));
                    break;
                case 2:
                    updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE,
                            new StringType("Vehicle status unknown"));
                    break;
                default:
                    logger.warn("Received unknown vehicle value: {}", controllerVariables.vehicle);
            }
            updateState(OpenGarageBindingConstants.CHANNEL_OG_VEHICLE_STATUS,
                    new DecimalType(controllerVariables.vehicle));
        }

        if (inTransition) {
            return Math.min(this.refreshInterval, this.transitionTimeSecs);
        } else {
            return this.refreshInterval;
        }
    }

    private void changeStatus(OpenGarageCommand status) throws OpenGarageCommunicationException {
        webTargets.setControllerVariables(status);
    }

    private Function<Boolean, Boolean> getInverter(String channelUID) {
        Channel channel = getThing().getChannel(channelUID);
        var invert = channel != null && channel.getConfiguration().as(OpenGarageChannelConfiguration.class).invert;
        if (invert) {
            return onOff -> !onOff;
        } else {
            return Function.identity();
        }
    }
}
