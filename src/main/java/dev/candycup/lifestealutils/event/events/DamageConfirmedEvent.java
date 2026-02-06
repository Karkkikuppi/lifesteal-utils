package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

/**
 * fired when the server confirms damage to an entity.
 * this event occurs after a ClientAttackEvent when the server validates the hit.
 */
public class DamageConfirmedEvent extends LSUEvent {
   private final int entityId;
   private final ClientboundDamageEventPacket packet;

   public DamageConfirmedEvent(int entityId, ClientboundDamageEventPacket packet) {
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
