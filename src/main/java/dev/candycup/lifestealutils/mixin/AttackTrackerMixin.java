package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.CustomEnchantUtilities;
import dev.candycup.lifestealutils.LifestealServerDetector;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
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

   @Inject(method = "attack", at = @At("HEAD"))
   private void onAttack(Player player, Entity target, CallbackInfo ci) {
      if (!LifestealServerDetector.isOnLifestealServer()) return;
      if (!Config.isChainCounterEnabled()) return;
      if (minecraft.player == null) return;
      // only track attacks by the local player
      if (player != minecraft.player) return;
      if (!(target instanceof Player)) return;
      if (!CustomEnchantUtilities.hasCustomEnchant(player.getMainHandItem(), "enchants:unbroken_chain")) return;

      UnbrokenChainTracker.onClientAttack(target.getId());
   }
}
