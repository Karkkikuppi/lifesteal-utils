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

   public ChatMessageReceivedEvent(Component message) {
      this.message = message;
      this.modifiedMessage = message;
   }

   public Component getMessage() {
      return message;
   }

   public Component getModifiedMessage() {
      return modifiedMessage;
   }

   /**
    * modify the chat message that will be displayed.
    *
    * @param modifiedMessage the new message to display
    */
   public void setModifiedMessage(Component modifiedMessage) {
      this.modifiedMessage = modifiedMessage != null ? modifiedMessage : message;
   }

   @Override
   public boolean isCancellable() {
      return true;
   }
}
