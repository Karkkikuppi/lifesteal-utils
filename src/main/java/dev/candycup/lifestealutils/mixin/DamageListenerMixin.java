package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealServerDetector;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class DamageListenerMixin {

   @Inject(method = "handleDamageEvent", at = @At("TAIL"))
   private void onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
      if (!LifestealServerDetector.isOnLifestealServer()) return;
      if (!Config.isChainCounterEnabled()) return;
      if (Minecraft.getInstance().player == null) return;

      int entityId = packet.entityId();

      // check if the local player was damaged - reset chain
      if (entityId == Minecraft.getInstance().player.getId()) {
         UnbrokenChainTracker.onPlayerDamaged();
         return;
      }

      Entity entity = Minecraft.getInstance().level != null
              ? Minecraft.getInstance().level.getEntity(entityId)
              : null;
      if (!(entity instanceof Player)) {
         return;
      }

      // otherwise, this is damage to another player - confirm the hit
      UnbrokenChainTracker.onServerDamageConfirmed(entityId);
   }
}
