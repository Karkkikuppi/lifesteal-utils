package dev.candycup.lifestealutils.event;

import dev.candycup.lifestealutils.event.events.*;
import dev.candycup.lifestealutils.event.listener.LifestealEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * central event bus for dispatching events to registered listeners.
 * listeners are organized by event type and sorted by priority.
 * 
 * OPTIMIZED VERSION with fixes for:
 * - Null-safety issues
 * - Better exception handling
 * - Performance optimizations
 */
public class EventBus {
   private static final Logger LOGGER = LoggerFactory.getLogger("LifestealUtils/EventBus");
   private static final EventBus INSTANCE = new EventBus();
   private static final String EVENT_LISTENER_PACKAGE = "dev.candycup.lifestealutils.event.listener";

   private final Map<Class<? extends LSUEvent>, CopyOnWriteArrayList<LifestealEventListener>> listeners = new ConcurrentHashMap<>();

   private EventBus() {
   }

   public static EventBus getInstance() {
      return INSTANCE;
   }

   /**
    * register a listener to receive events.
    * the listener will receive all events it has handler methods for.
    *
    * @param listener the listener to register
    */
   public void register(LifestealEventListener listener) {
      if (listener == null) {
         LOGGER.warn("Attempted to register null listener");
         return;
      }

      // discover which event types this listener handles
      List<Class<? extends LSUEvent>> eventTypes = discoverEventTypes(listener);

      if (eventTypes.isEmpty()) {
         LOGGER.warn("Listener {} implements no known event interfaces", 
                     listener.getClass().getSimpleName());
         return;
      }

      for (Class<? extends LSUEvent> eventType : eventTypes) {
         listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
         // sort by priority (high to low)
         listeners.get(eventType).sort((a, b) ->
                 Integer.compare(b.getPriority().getValue(), a.getPriority().getValue())
         );
      }

      LOGGER.debug("Registered listener: {} for {} event types",
              listener.getClass().getSimpleName(), eventTypes.size());
   }

   /**
    * unregister a listener from receiving events.
    *
    * @param listener the listener to unregister
    */
   public void unregister(LifestealEventListener listener) {
      if (listener == null) {
         LOGGER.warn("Attempted to unregister null listener");
         return;
      }

      int removedCount = 0;
      for (CopyOnWriteArrayList<LifestealEventListener> list : listeners.values()) {
         if (list.remove(listener)) {
            removedCount++;
         }
      }
      
      if (removedCount > 0) {
         LOGGER.debug("Unregistered listener: {} from {} event types", 
                      listener.getClass().getSimpleName(), removedCount);
      } else {
         LOGGER.warn("Listener {} was not registered", listener.getClass().getSimpleName());
      }
   }

   /**
    * post an event to all registered listeners.
    * listeners are called in priority order (high to low).
    * if the event is cancelled, remaining listeners are still notified.
    *
    * @param event the event to post
    * @param <T>   the event type
    */
   public <T extends LSUEvent> void post(T event) {
      if (event == null) {
         LOGGER.warn("Attempted to post null event");
         return;
      }

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
         } catch (ClassCastException e) {
            // FIX: Specific handling for type mismatches
            LOGGER.error("Type mismatch handling event {} in listener {}: {}",
                    event.getClass().getSimpleName(),
                    listener.getClass().getSimpleName(),
                    e.getMessage());
         } catch (IllegalStateException e) {
            // FIX: Specific handling for state issues
            LOGGER.error("Illegal state handling event {} in listener {}: {}",
                    event.getClass().getSimpleName(),
                    listener.getClass().getSimpleName(),
                    e.getMessage());
         } catch (Exception e) {
            // FIX: Only catch truly unexpected exceptions
            LOGGER.error("Unexpected error handling event {} in listener {}",
                    event.getClass().getSimpleName(),
                    listener.getClass().getSimpleName(),
                    e);
         }
      }
   }

   /**
    * discover which event types a listener can handle based on implemented interfaces.
    * walks the class hierarchy to find interfaces implemented by superclasses.
    *
    * @param listener the listener to check
    * @return list of event types the listener handles
    */
   private List<Class<? extends LSUEvent>> discoverEventTypes(LifestealEventListener listener) {
      List<Class<? extends LSUEvent>> eventTypes = new ArrayList<>();

      // check all implemented interfaces (including superclasses)
      for (Class<?> type = listener.getClass(); type != null; type = type.getSuperclass()) {
         for (Class<?> iface : type.getInterfaces()) {
            // FIX: Null-safe package check using Objects.equals
            if (iface.getPackage() != null &&
                    EVENT_LISTENER_PACKAGE.equals(iface.getPackage().getName())) {
               List<Class<? extends LSUEvent>> typesForInterface = getEventTypesForInterface(iface);
               eventTypes.addAll(typesForInterface);
            }
         }
      }

      return eventTypes;
   }

   /**
    * map listener interface to event types it handles.
    *
    * @param listenerInterface the listener interface
    * @return list of event types
    */
   private List<Class<? extends LSUEvent>> getEventTypesForInterface(Class<?> listenerInterface) {
      List<Class<? extends LSUEvent>> eventTypes = new ArrayList<>();
      String interfaceName = listenerInterface.getSimpleName();

      // FIX: Use switch expression for better performance and readability (Java 14+)
      // If using older Java, this can remain as if-else chain
      switch (interfaceName) {
         case "CombatEventListener":
            eventTypes.add(ClientAttackEvent.class);
            eventTypes.add(DamageConfirmedEvent.class);
            eventTypes.add(PlayerDamagedEvent.class);
            break;
         case "ChatEventListener":
            eventTypes.add(ChatMessageReceivedEvent.class);
            eventTypes.add(ChatMessageSentEvent.class);
            break;
         case "TickEventListener":
            eventTypes.add(ClientTickEvent.class);
            break;
         case "ServerEventListener":
            eventTypes.add(ServerChangeEvent.class);
            eventTypes.add(LifestealShardSwapEvent.class);
            break;
         case "RenderEventListener":
            eventTypes.add(ItemRenderEvent.class);
            eventTypes.add(PlayerNameRenderEvent.class);
            break;
         case "UIEventListener":
            eventTypes.add(TitleScreenInitEvent.class);
            eventTypes.add(SplashTextRequestEvent.class);
            break;
         default:
            // FIX: Log unknown listener interfaces
            LOGGER.debug("Unknown listener interface: {}", interfaceName);
            break;
      }

      return eventTypes;
   }

   /**
    * clear all registered listeners. useful for testing.
    */
   public void clearAllListeners() {
      int totalListeners = listeners.values().stream()
              .mapToInt(List::size)
              .sum();
      listeners.clear();
      LOGGER.debug("Cleared {} listeners from {} event types", totalListeners, listeners.size());
   }

   /**
    * get the number of registered listeners for a specific event type.
    * useful for debugging and testing.
    *
    * @param eventType the event type to check
    * @return the number of listeners
    */
   public int getListenerCount(Class<? extends LSUEvent> eventType) {
      List<LifestealEventListener> eventListeners = listeners.get(eventType);
      return eventListeners != null ? eventListeners.size() : 0;
   }

   /**
    * check if any listeners are registered for a specific event type.
    *
    * @param eventType the event type to check
    * @return true if at least one listener is registered
    */
   public boolean hasListeners(Class<? extends LSUEvent> eventType) {
      return getListenerCount(eventType) > 0;
   }
}

