package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.CustomEnchantUtilities;
import dev.candycup.lifestealutils.LifestealServerDetector;
import dev.candycup.lifestealutils.event.EventBus;
import dev.candycup.lifestealutils.event.events.ClientAttackEvent;
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

   /**
    * Intercepts client-side attack attempts, publishes a ClientAttackEvent to the EventBus,
    * and cancels the attack when the published event is cancelled.
    *
    * @param player the player performing the attack (only local player attacks are processed)
    * @param target the entity being attacked (processing proceeds only if this is a Player)
    * @param ci     callback info used to cancel the original attack method when needed
    */
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void onAttack(Player player, Entity target, CallbackInfo ci) {
      if (!LifestealServerDetector.isOnLifestealServer()) return;
      if (minecraft.player == null) return;
      // only track attacks by the local player
      if (player != minecraft.player) return;
      if (!(target instanceof Player)) return;

      ClientAttackEvent event = new ClientAttackEvent(target, System.currentTimeMillis());
      EventBus.getInstance().post(event);
      
      // allow features to cancel the attack
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}