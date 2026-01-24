package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.client.Minecraft;

/**
 * fired every client tick (20 times per second).
 * used for periodic updates and state management.
 */
public class ClientTickEvent extends LSUEvent {
    private final Minecraft client;

    /**
     * Creates a new ClientTickEvent for the given Minecraft client.
     *
     * @param client the Minecraft game client associated with this event
     */
    public ClientTickEvent(Minecraft client) {
        this.client = client;
    }

    /**
     * Return the Minecraft client instance associated with this event.
     *
     * @return the Minecraft client instance stored in this event
     */
    public Minecraft getClient() {
        return client;
    }

    /**
     * Indicates whether this event supports cancellation.
     *
     * @return {@code true} if the event can be cancelled, {@code false} otherwise.
     */
    @Override
    public boolean isCancellable() {
        return false;
    }
}