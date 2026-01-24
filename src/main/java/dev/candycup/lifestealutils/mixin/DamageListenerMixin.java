package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.LifestealServerDetector;
import dev.candycup.lifestealutils.event.EventBus;
import dev.candycup.lifestealutils.event.events.DamageConfirmedEvent;
import dev.candycup.lifestealutils.event.events.PlayerDamagedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class DamageListenerMixin {

   /**
    * Processes a server damage packet and posts lifesteal-specific events to the global event bus.
    *
    * <p>If the packet targets the local player, a {@link PlayerDamagedEvent} is posted.
    * If it targets another player entity, a {@link DamageConfirmedEvent} is posted.
    * No events are posted when not connected to a Lifesteal server or when the local player or target entity is unavailable.</p>
    *
    * @param packet the incoming damage event packet from the server
    */
   @Inject(method = "handleDamageEvent", at = @At("TAIL"))
   private void onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
      if (!LifestealServerDetector.isOnLifestealServer()) return;
      if (Minecraft.getInstance().player == null) return;

      int entityId = packet.entityId();

      // check if the local player was damaged - post player damaged event
      if (entityId == Minecraft.getInstance().player.getId()) {
         EventBus.getInstance().post(new PlayerDamagedEvent(entityId, packet));
         return;
      }

      Entity entity = Minecraft.getInstance().level != null
              ? Minecraft.getInstance().level.getEntity(entityId)
              : null;
      if (!(entity instanceof Player)) {
         return;
      }

      // otherwise, this is damage to another player - post damage confirmed event
      EventBus.getInstance().post(new DamageConfirmedEvent(entityId, packet));
   }
}