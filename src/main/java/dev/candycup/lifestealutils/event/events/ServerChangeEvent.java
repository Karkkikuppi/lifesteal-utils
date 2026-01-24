package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * fired when the player connects to or disconnects from a server.
 * used for feature lifecycle management and state cleanup.
 */
public class ServerChangeEvent extends LSUEvent {
    private final Type type;
    private final String serverAddress;

    public enum Type {
        CONNECTED,
        DISCONNECTED
    }

    /**
     * Create a ServerChangeEvent representing a change in server connection state.
     *
     * @param type          the event type indicating CONNECTED or DISCONNECTED
     * @param serverAddress the server address associated with the event
     */
    public ServerChangeEvent(Type type, String serverAddress) {
        this.type = type;
        this.serverAddress = serverAddress;
    }

    /**
     * Gets the event's type.
     *
     * @return the event type: CONNECTED if the player connected, DISCONNECTED if the player disconnected.
     */
    public Type getType() {
        return type;
    }

    /**
     * Retrieve the server address associated with this event.
     *
     * @return the server address associated with the event
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Indicates whether this event represents a connection to a server.
     *
     * @return `true` if the event type is CONNECTED, `false` otherwise.
     */
    public boolean isConnected() {
        return type == Type.CONNECTED;
    }

    /**
     * Determines whether this event represents a server disconnection.
     *
     * @return true if the event represents a disconnection, false otherwise.
     */
    public boolean isDisconnected() {
        return type == Type.DISCONNECTED;
    }

    /**
     * Indicates whether this event can be cancelled.
     *
     * @return true if the event can be cancelled, false otherwise.
     */
    @Override
    public boolean isCancellable() {
        return false;
    }
}