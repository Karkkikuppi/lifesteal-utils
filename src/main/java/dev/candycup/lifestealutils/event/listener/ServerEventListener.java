package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ServerChangeEvent;

/**
 * listener interface for server connection events.
 * override methods to handle server lifecycle.
 */
public interface ServerEventListener extends LifestealEventListener {

    /**
 * Invoked when the player connects to or disconnects from a server to allow implementing listeners to manage feature lifecycle and clean up state.
 *
 * @param event the ServerChangeEvent describing the connection change (connect or disconnect)
 */
    default void onServerChange(ServerChangeEvent event) {}
}