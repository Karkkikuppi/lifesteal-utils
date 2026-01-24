package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.chat.Component;

/**
 * fired when a chat message is received from the server.
 * can be cancelled to prevent the message from being displayed.
 */
public class ChatMessageReceivedEvent extends LSUEvent {
    private final Component message;
    private Component modifiedMessage;

    /**
     * Creates a new ChatMessageReceivedEvent for a received chat message.
     *
     * The original message is stored as the immutable source message and the
     * mutable display message is initialized to the same value.
     *
     * @param message the original chat message received from the server
     */
    public ChatMessageReceivedEvent(Component message) {
        this.message = message;
        this.modifiedMessage = message;
    }

    /**
     * Gets the original chat message received from the server.
     *
     * @return the original chat message as a Component
     */
    public Component getMessage() {
        return message;
    }

    /**
     * Retrieves the message currently configured to be displayed to the user.
     *
     * @return the message that will be displayed, which may differ from the original received message
     */
    public Component getModifiedMessage() {
        return modifiedMessage;
    }

    /**
     * Sets the chat message that will be displayed to the user.
     *
     * @param modifiedMessage the message to display instead of the original
     */
    public void setModifiedMessage(Component modifiedMessage) {
        this.modifiedMessage = modifiedMessage;
    }

    /**
     * Indicates that this event supports cancellation.
     *
     * @return `true` if the event can be cancelled, `false` otherwise.
     */
    @Override
    public boolean isCancellable() {
        return true;
    }
}