package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.events.ChatMessageSentEvent;

/**
 * listener interface for chat-related events.
 * override methods to handle specific events.
 */
public interface ChatEventListener extends LifestealEventListener {

    /**
 * Invoked when a chat message is received from the server; implementors may cancel or modify the message.
 *
 * @param event the received chat message event which can be inspected, modified, or cancelled
 */
    default void onChatMessageReceived(ChatMessageReceivedEvent event) {}

    /**
 * Invoked when the local player sends a chat message.
 *
 * Implementations may inspect or cancel the outgoing message via the event.
 *
 * @param event the event representing the outgoing chat message
 */
    default void onChatMessageSent(ChatMessageSentEvent event) {}
}