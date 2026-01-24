package dev.candycup.lifestealutils.event;

import dev.candycup.lifestealutils.event.listener.LifestealEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * central event bus for dispatching events to registered listeners.
 * listeners are organized by event type and sorted by priority.
 */
public class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("LifestealUtils/EventBus");
    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<? extends LSUEvent>, List<LifestealEventListener>> listeners = new ConcurrentHashMap<>();

    /**
 * Prevents external instantiation to enforce the singleton pattern for EventBus.
 */
private EventBus() {}

    /**
     * Provide access to the global EventBus singleton.
     *
     * @return the singleton EventBus instance
     */
    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Register a LifestealEventListener so it receives all LSUEvent types it handles.
     *
     * Listeners registered for a given event type are invoked in priority order (high to low)
     * when that event is posted.
     *
     * @param listener the listener to register; the bus will discover which event types it handles
     */
    public void register(LifestealEventListener listener) {
        // discover which event types this listener handles
        List<Class<? extends LSUEvent>> eventTypes = discoverEventTypes(listener);
        
        for (Class<? extends LSUEvent> eventType : eventTypes) {
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
            // sort by priority (high to low)
            listeners.get(eventType).sort((a, b) -> 
                Integer.compare(b.getPriority().getValue(), a.getPriority().getValue())
            );
        }

        LOGGER.debug("Registered listener: {} for {} event types", 
            listener.getClass().getSimpleName(), eventTypes.size());
    }

    /**
     * Remove a listener so it no longer receives dispatched events.
     *
     * @param listener the LifestealEventListener to remove from all registered event lists
     */
    public void unregister(LifestealEventListener listener) {
        listeners.values().forEach(list -> list.remove(listener));
        LOGGER.debug("Unregistered listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Dispatches an LSUEvent to all registered listeners.
     *
     * Listeners are invoked in priority order (high to low). Disabled listeners are skipped.
     * Exceptions thrown by listeners are caught and logged and do not stop dispatch to remaining listeners.
     * If the event supports cancellation, remaining listeners are still notified.
     *
     * @param event the event to dispatch
     * @param <T>   the concrete event type
     */
    public <T extends LSUEvent> void post(T event) {
        List<LifestealEventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }

        for (LifestealEventListener listener : eventListeners) {
            if (!listener.isEnabled()) {
                continue;
            }

            try {
                listener.handleEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error handling event {} in listener {}", 
                    event.getClass().getSimpleName(), 
                    listener.getClass().getSimpleName(), 
                    e);
            }
        }
    }

    /**
     * Determine which LSUEvent types the given listener handles by inspecting its implemented interfaces.
     *
     * @param listener the listener to inspect
     * @return a list of LSUEvent classes the listener can handle; an empty list if none are discovered
     */
    private List<Class<? extends LSUEvent>> discoverEventTypes(LifestealEventListener listener) {
        List<Class<? extends LSUEvent>> eventTypes = new ArrayList<>();
        
        // check all implemented interfaces
        for (Class<?> iface : listener.getClass().getInterfaces()) {
            // map interface to event types
            if (iface.getPackage() != null && 
                iface.getPackage().getName().equals("dev.candycup.lifestealutils.event.listener")) {
                eventTypes.addAll(getEventTypesForInterface(iface));
            }
        }
        
        return eventTypes;
    }

    /**
     * Map a listener interface to the LSUEvent classes it handles.
     *
     * @param listenerInterface the listener interface Class to map (for example `CombatEventListener`)
     * @return a list of LSUEvent classes associated with the provided listener interface; empty if the interface has no mapped event types
     */
    private List<Class<? extends LSUEvent>> getEventTypesForInterface(Class<?> listenerInterface) {
        List<Class<? extends LSUEvent>> eventTypes = new ArrayList<>();
        String interfaceName = listenerInterface.getSimpleName();

        // map based on interface name
        if (interfaceName.equals("CombatEventListener")) {
            eventTypes.add(dev.candycup.lifestealutils.event.events.ClientAttackEvent.class);
            eventTypes.add(dev.candycup.lifestealutils.event.events.DamageConfirmedEvent.class);
            eventTypes.add(dev.candycup.lifestealutils.event.events.PlayerDamagedEvent.class);
        } else if (interfaceName.equals("ChatEventListener")) {
            eventTypes.add(dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent.class);
            eventTypes.add(dev.candycup.lifestealutils.event.events.ChatMessageSentEvent.class);
        } else if (interfaceName.equals("TickEventListener")) {
            eventTypes.add(dev.candycup.lifestealutils.event.events.ClientTickEvent.class);
        } else if (interfaceName.equals("ServerEventListener")) {
            eventTypes.add(dev.candycup.lifestealutils.event.events.ServerChangeEvent.class);
        } else if (interfaceName.equals("RenderEventListener")) {
            eventTypes.add(dev.candycup.lifestealutils.event.events.ItemRenderEvent.class);
            eventTypes.add(dev.candycup.lifestealutils.event.events.PlayerNameRenderEvent.class);
        } else if (interfaceName.equals("UIEventListener")) {
            eventTypes.add(dev.candycup.lifestealutils.event.events.TitleScreenInitEvent.class);
            eventTypes.add(dev.candycup.lifestealutils.event.events.SplashTextRequestEvent.class);
        }

        return eventTypes;
    }

    /**
     * Clears all registered listeners from the event bus.
     *
     * Primarily intended for tests to reset the global listener state.
     */
    public void clearAllListeners() {
        listeners.clear();
        LOGGER.debug("Cleared all listeners");
    }
}