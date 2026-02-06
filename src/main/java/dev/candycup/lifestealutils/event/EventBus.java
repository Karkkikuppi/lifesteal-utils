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
 */
public class EventBus {
   private static final Logger LOGGER = LoggerFactory.getLogger("LifestealUtils/EventBus");
   private static final EventBus INSTANCE = new EventBus();

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
      // discover which event types this listener handles
      List<Class<? extends LSUEvent>> eventTypes = discoverEventTypes(listener);

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
      listeners.values().forEach(list -> list.remove(listener));
      LOGGER.debug("Unregistered listener: {}", listener.getClass().getSimpleName());
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
            if (iface.getPackage() != null &&
                    iface.getPackage().getName().equals("dev.candycup.lifestealutils.event.listener")) {
               eventTypes.addAll(getEventTypesForInterface(iface));
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

      // map based on interface name
      if (interfaceName.equals("CombatEventListener")) {
         eventTypes.add(ClientAttackEvent.class);
         eventTypes.add(DamageConfirmedEvent.class);
         eventTypes.add(PlayerDamagedEvent.class);
      } else if (interfaceName.equals("ChatEventListener")) {
         eventTypes.add(ChatMessageReceivedEvent.class);
         eventTypes.add(ChatMessageSentEvent.class);
      } else if (interfaceName.equals("TickEventListener")) {
         eventTypes.add(ClientTickEvent.class);
      } else if (interfaceName.equals("ServerEventListener")) {
         eventTypes.add(ServerChangeEvent.class);
         eventTypes.add(LifestealShardSwapEvent.class);
      } else if (interfaceName.equals("RenderEventListener")) {
         eventTypes.add(ItemRenderEvent.class);
         eventTypes.add(PlayerNameRenderEvent.class);
      } else if (interfaceName.equals("UIEventListener")) {
         eventTypes.add(TitleScreenInitEvent.class);
         eventTypes.add(SplashTextRequestEvent.class);
      }

      return eventTypes;
   }

   /**
    * clear all registered listeners. useful for testing.
    */
   public void clearAllListeners() {
      listeners.clear();
      LOGGER.debug("Cleared all listeners");
   }
}
