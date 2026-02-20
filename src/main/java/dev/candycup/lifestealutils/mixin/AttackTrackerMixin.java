package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientAttackEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class AttackTrackerMixin {

   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void onAttack(Player player, Entity target, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;
      if (minecraft.player == null) return;
      if (player != minecraft.player) return;
      if (!(target instanceof Player)) return;

      ClientAttackEvent event = new ClientAttackEvent(target, System.currentTimeMillis());
      LifestealUtilsEvents.CLIENT_ATTACK.invoker().onClientAttack(event);

      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
