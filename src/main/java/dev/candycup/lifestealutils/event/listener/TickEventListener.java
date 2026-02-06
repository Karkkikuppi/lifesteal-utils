package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ClientTickEvent;

/**
 * listener interface for tick events.
 * override methods to handle periodic updates.
 */
public interface TickEventListener extends LifestealEventListener {

   /**
    * called every client tick (20 times per second).
    * use for periodic updates and state management.
    *
    * @param event the tick event
    */
   default void onClientTick(ClientTickEvent event) {
   }
}
