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
package org.openhab.binding.boschshc.internal.devices.plug;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.boschshc.internal.devices.AbstractPowerSwitchHandlerTest;
import org.openhab.binding.boschshc.internal.devices.BoschSHCBindingConstants;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Unit tests for {@link PlugHandler}.
 *
 * @author David Pace - Initial contribution
 *
 */
@NonNullByDefault
public class PlugHandlerTest extends AbstractPowerSwitchHandlerTest<PlugHandler> {

    @Override
    protected PlugHandler createFixture() {
        return new PlugHandler(getThing());
    }

    @Override
    protected String getDeviceID() {
        return "hdm:ZigBee:50325ffffe61d7b9c6e";
    }

    @Override
    protected ThingTypeUID getThingTypeUID() {
        return BoschSHCBindingConstants.THING_TYPE_SMART_PLUG_COMPACT;
    }
}
