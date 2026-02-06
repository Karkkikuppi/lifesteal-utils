package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

/**
 * fired when the local player receives damage.
 * this typically resets combat tracking for the player.
 */
public class PlayerDamagedEvent extends LSUEvent {
   private final int entityId;
   private final ClientboundDamageEventPacket packet;

   public PlayerDamagedEvent(int entityId, ClientboundDamageEventPacket packet) {
      this.entityId = entityId;
      this.packet = packet;
   }

   public int getEntityId() {
      return entityId;
   }

   public ClientboundDamageEventPacket getPacket() {
      return packet;
   }

   @Override
   public boolean isCancellable() {
      return false;
   }
}
