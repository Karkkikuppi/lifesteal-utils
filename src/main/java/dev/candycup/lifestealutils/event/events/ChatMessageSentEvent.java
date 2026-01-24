package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * fired when the local player sends a chat message.
 * can be cancelled to prevent the message from being sent.
 */
public class ChatMessageSentEvent extends LSUEvent {
    private final String message;

    /**
     * Creates a ChatMessageSentEvent representing the given chat message.
     *
     * @param message the chat message content sent by the local player
     */
    public ChatMessageSentEvent(String message) {
        this.message = message;
    }

    /**
     * Retrieves the chat message content.
     *
     * @return the message that will be sent by the local player
     */
    public String getMessage() {
        return message;
    }

    /**
     * Indicates that this event can be cancelled.
     *
     * @return `true` if the event can be cancelled, `false` otherwise.
     */
    @Override
    public boolean isCancellable() {
        return true;
    }
}