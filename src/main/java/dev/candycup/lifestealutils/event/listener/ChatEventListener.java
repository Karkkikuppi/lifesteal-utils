package dev.candycup.lifestealutils.event.listener;

import dev.candycup.lifestealutils.event.events.ChatMessageReceivedEvent;
import dev.candycup.lifestealutils.event.events.ChatMessageSentEvent;

/**
 * listener interface for chat-related events.
 * override methods to handle specific events.
 */
public interface ChatEventListener extends LifestealEventListener {

   /**
    * called when a chat message is received from the server.
    * can cancel or modify the message.
    *
    * @param event the chat message event
    */
   default void onChatMessageReceived(ChatMessageReceivedEvent event) {
   }

   /**
    * called when the local player sends a chat message.
    * can cancel the message.
    *
    * @param event the chat message sent event
    */
   default void onChatMessageSent(ChatMessageSentEvent event) {
   }
}
