package org.openhab.io.homekit.internal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.service.CommandDescriptionService;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;

@NonNullByDefault
public class OfflineItemDecorator extends GenericItem {
    private GenericItem proxy;

    private Optional<Thing> itemThing;

    public OfflineItemDecorator(GenericItem proxy, ItemChannelLinkRegistry itemChannelLinkRegistry,
            ThingRegistry thingRegistry) {
        super(proxy.getName(), proxy.getType());
        // TODO - probably need to lazily do this during subscribe? IDK... super hacky
        var itemChannelLink = itemChannelLinkRegistry.stream().filter(link -> link.getItemName() == proxy.getName())
                .findFirst();

        this.itemThing = itemChannelLink.map(link -> thingRegistry.get(link.getLinkedUID().getThingUID()));
        this.proxy = proxy;
    }

    @Override
    public State getState() {
        var isOnline = this.itemThing.map(thing -> thing.getStatus() == ThingStatus.ONLINE).orElse(true);
        if (isOnline) {
            return proxy.getState();
        } else {
            return UnDefType.UNDEF;
        }
    }

    @Override
    public <T extends @NonNull State> @Nullable T getStateAs(Class<T> typeClass) {
        return this.getState().as(typeClass);
    }

    @Override
    public String getUID() {
        return proxy.getUID();
    }

    @Override
    public @NonNull String getName() {
        return proxy.getName();
    }

    @Override
    public @NonNull String getType() {
        return proxy.getType();
    }

    @Override
    public List<@NonNull String> getGroupNames() {
        return proxy.getGroupNames();
    }

    @Override
    public void addGroupName(String groupItemName) {
        proxy.addGroupName(groupItemName);
    }

    @Override
    public void addGroupNames(String... groupItemNames) {
        proxy.addGroupNames(groupItemNames);
    }

    @Override
    public void addGroupNames(List<@NonNull String> groupItemNames) {
        proxy.addGroupNames(groupItemNames);
    }

    @Override
    public void removeGroupName(String groupItemName) {
        proxy.removeGroupName(groupItemName);
    }

    @Override
    public void dispose() {
        proxy.dispose();
    }

    @Override
    public void setEventPublisher(@Nullable EventPublisher eventPublisher) {
        proxy.setEventPublisher(eventPublisher);
    }

    @Override
    public void setStateDescriptionService(@Nullable StateDescriptionService stateDescriptionService) {
        proxy.setStateDescriptionService(stateDescriptionService);
    }

    @Override
    public void setCommandDescriptionService(@Nullable CommandDescriptionService commandDescriptionService) {
        proxy.setCommandDescriptionService(commandDescriptionService);
    }

    @Override
    public void setUnitProvider(@Nullable UnitProvider unitProvider) {
        proxy.setUnitProvider(unitProvider);
    }

    @Override
    public void setItemStateConverter(@Nullable ItemStateConverter itemStateConverter) {
        proxy.setItemStateConverter(itemStateConverter);
    }

    @Override
    public void setState(State state) {
        proxy.setState(state);
    }

    @Override
    public void send(RefreshType command) {
        proxy.send(command);
    }

    @Override
    public String toString() {
        return proxy.toString();
    }

    @Override
    public void addStateChangeListener(StateChangeListener listener) {
        proxy.addStateChangeListener(listener);
    }

    @Override
    public void removeStateChangeListener(StateChangeListener listener) {
        proxy.removeStateChangeListener(listener);
    }

    @Override
    public int hashCode() {
        return proxy.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return proxy.equals(obj);
    }

    @Override
    public Set<@NonNull String> getTags() {
        return proxy.getTags();
    }

    @Override
    public boolean hasTag(String tag) {
        return proxy.hasTag(tag);
    }

    @Override
    public void addTag(String tag) {
        proxy.addTag(tag);
    }

    @Override
    public void addTags(Collection<@NonNull String> tags) {
        proxy.addTags(tags);
    }

    @Override
    public void addTags(String... tags) {
        proxy.addTags(tags);
    }

    @Override
    public void removeTag(String tag) {
        proxy.removeTag(tag);
    }

    @Override
    public void removeAllTags() {
        proxy.removeAllTags();
    }

    @Override
    public @Nullable String getLabel() {
        return proxy.getLabel();
    }

    @Override
    public void setLabel(@Nullable String label) {
        proxy.setLabel(label);
    }

    @Override
    public @Nullable String getCategory() {
        return proxy.getCategory();
    }

    @Override
    public void setCategory(@Nullable String category) {
        proxy.setCategory(category);
    }

    @Override
    public @Nullable StateDescription getStateDescription() {
        return proxy.getStateDescription();
    }

    @Override
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale) {
        return proxy.getStateDescription(locale);
    }

    @Override
    public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
        return proxy.getCommandDescription(locale);
    }

    @Override
    public boolean isAcceptedState(List<@NonNull Class<? extends @NonNull State>> acceptedDataTypes, State state) {
        return proxy.isAcceptedState(acceptedDataTypes, state);
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return this.proxy.getAcceptedCommandTypes();
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return this.proxy.getAcceptedDataTypes();
    }
}
