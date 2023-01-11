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
    private GenericItem wrappedItem;

    private Optional<Thing> itemThing;

    public OfflineItemDecorator(GenericItem wrappedItem, ItemChannelLinkRegistry itemChannelLinkRegistry,
            ThingRegistry thingRegistry) {
        super(wrappedItem.getName(), wrappedItem.getType());
        // TODO - probably need to lazily do this during subscribe? IDK... super hacky
        var itemChannelLink = itemChannelLinkRegistry.stream().filter(link -> link.getItemName() == wrappedItem.getName())
                .findFirst();

        this.itemThing = itemChannelLink.map(link -> thingRegistry.get(link.getLinkedUID().getThingUID()));
        this.wrappedItem = wrappedItem;
    }

    @Override
    public State getState() {
        var isOnline = this.itemThing.map(thing -> thing.getStatus() == ThingStatus.ONLINE).orElse(true);
        if (isOnline) {
            return wrappedItem.getState();
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
        return wrappedItem.getUID();
    }

    @Override
    public @NonNull String getName() {
        return wrappedItem.getName();
    }

    @Override
    public @NonNull String getType() {
        return wrappedItem.getType();
    }

    @Override
    public List<@NonNull String> getGroupNames() {
        return wrappedItem.getGroupNames();
    }

    @Override
    public void addGroupName(String groupItemName) {
        wrappedItem.addGroupName(groupItemName);
    }

    @Override
    public void addGroupNames(String... groupItemNames) {
        wrappedItem.addGroupNames(groupItemNames);
    }

    @Override
    public void addGroupNames(List<@NonNull String> groupItemNames) {
        wrappedItem.addGroupNames(groupItemNames);
    }

    @Override
    public void removeGroupName(String groupItemName) {
        wrappedItem.removeGroupName(groupItemName);
    }

    @Override
    public void dispose() {
        wrappedItem.dispose();
    }

    @Override
    public void setEventPublisher(@Nullable EventPublisher eventPublisher) {
        wrappedItem.setEventPublisher(eventPublisher);
    }

    @Override
    public void setStateDescriptionService(@Nullable StateDescriptionService stateDescriptionService) {
        wrappedItem.setStateDescriptionService(stateDescriptionService);
    }

    @Override
    public void setCommandDescriptionService(@Nullable CommandDescriptionService commandDescriptionService) {
        wrappedItem.setCommandDescriptionService(commandDescriptionService);
    }

    @Override
    public void setUnitProvider(@Nullable UnitProvider unitProvider) {
        wrappedItem.setUnitProvider(unitProvider);
    }

    @Override
    public void setItemStateConverter(@Nullable ItemStateConverter itemStateConverter) {
        wrappedItem.setItemStateConverter(itemStateConverter);
    }

    @Override
    public void setState(State state) {
        wrappedItem.setState(state);
    }

    @Override
    public void send(RefreshType command) {
        wrappedItem.send(command);
    }

    @Override
    public String toString() {
        return wrappedItem.toString();
    }

    @Override
    public void addStateChangeListener(StateChangeListener listener) {
        wrappedItem.addStateChangeListener(listener);
    }

    @Override
    public void removeStateChangeListener(StateChangeListener listener) {
        wrappedItem.removeStateChangeListener(listener);
    }

    @Override
    public int hashCode() {
        return wrappedItem.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return wrappedItem.equals(obj);
    }

    @Override
    public Set<@NonNull String> getTags() {
        return wrappedItem.getTags();
    }

    @Override
    public boolean hasTag(String tag) {
        return wrappedItem.hasTag(tag);
    }

    @Override
    public void addTag(String tag) {
        wrappedItem.addTag(tag);
    }

    @Override
    public void addTags(Collection<@NonNull String> tags) {
        wrappedItem.addTags(tags);
    }

    @Override
    public void addTags(String... tags) {
        wrappedItem.addTags(tags);
    }

    @Override
    public void removeTag(String tag) {
        wrappedItem.removeTag(tag);
    }

    @Override
    public void removeAllTags() {
        wrappedItem.removeAllTags();
    }

    @Override
    public @Nullable String getLabel() {
        return wrappedItem.getLabel();
    }

    @Override
    public void setLabel(@Nullable String label) {
        wrappedItem.setLabel(label);
    }

    @Override
    public @Nullable String getCategory() {
        return wrappedItem.getCategory();
    }

    @Override
    public void setCategory(@Nullable String category) {
        wrappedItem.setCategory(category);
    }

    @Override
    public @Nullable StateDescription getStateDescription() {
        return wrappedItem.getStateDescription();
    }

    @Override
    public @Nullable StateDescription getStateDescription(@Nullable Locale locale) {
        return wrappedItem.getStateDescription(locale);
    }

    @Override
    public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
        return wrappedItem.getCommandDescription(locale);
    }

    @Override
    public boolean isAcceptedState(List<@NonNull Class<? extends @NonNull State>> acceptedDataTypes, State state) {
        return wrappedItem.isAcceptedState(acceptedDataTypes, state);
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return this.wrappedItem.getAcceptedCommandTypes();
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return this.wrappedItem.getAcceptedDataTypes();
    }
}
