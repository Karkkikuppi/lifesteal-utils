package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.client.Minecraft;

/**
 * fired every client tick (20 times per second).
 * used for periodic updates and state management.
 */
public class ClientTickEvent extends LSUEvent {
   private final Minecraft client;

   public ClientTickEvent(Minecraft client) {
      this.client = client;
   }

   public Minecraft getClient() {
      return client;
   }

   @Override
   public boolean isCancellable() {
      return false;
   }
}
