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

   public ServerChangeEvent(Type type, String serverAddress) {
      this.type = type;
      this.serverAddress = serverAddress;
   }

   public Type getType() {
      return type;
   }

   public String getServerAddress() {
      return serverAddress;
   }

   public boolean isConnected() {
      return type == Type.CONNECTED;
   }

   public boolean isDisconnected() {
      return type == Type.DISCONNECTED;
   }

   @Override
   public boolean isCancellable() {
      return false;
   }
}
