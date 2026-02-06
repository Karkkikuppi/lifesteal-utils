package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;

/**
 * fired when the local player sends a chat message.
 * can be cancelled to prevent the message from being sent.
 */
public class ChatMessageSentEvent extends LSUEvent {
   private final String message;

   public ChatMessageSentEvent(String message) {
      this.message = message;
   }

   public String getMessage() {
      return message;
   }

   @Override
   public boolean isCancellable() {
      return true;
   }
}
