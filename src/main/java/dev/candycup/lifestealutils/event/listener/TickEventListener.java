package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ClientTickEvent;

/**
 * listener interface for tick events.
 * override methods to handle periodic updates.
 */
public interface TickEventListener extends LifestealEventListener {

    /**
 * Handle a client tick event for periodic updates and state management.
 *
 * This method is invoked once per client tick (approximately 20 times per second).
 *
 * @param event the client tick event
 */
    default void onClientTick(ClientTickEvent event) {}
}