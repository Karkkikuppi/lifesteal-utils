package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * handles selecting alliance targets from the player's current crosshair target.
 */
public final class AllianceTargetSelectionHandler {
   private AllianceTargetSelectionHandler() {
   }

   /**
    * toggles alliance selection for the looked-at player, when valid.
    *
    * @param client the minecraft client
    */
   public static void handleKeyClick(Minecraft client) {
      if (client.screen != null || client.player == null) {
         return;
      }

      HitResult hitResult = client.hitResult;
      if (!(hitResult instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof Player targetPlayer)) {
         MessagingUtils.showMiniMessage("<red>You're not looking at a player.</red>");
         return;
      }
      if (!isTargetEligible(targetPlayer)) {
         return;
      }

      String targetUuid = targetPlayer.getStringUUID();
      String targetName = targetPlayer.getName().getString();
      AllianceSelectionController.toggleSelectedAllianceMember(targetUuid, targetName);
   }

   /**
    * determines whether the target player can be selected.
    *
    * @param targetPlayer the targeted player entity
    * @return true when the player is selectable
    */
   private static boolean isTargetEligible(Player targetPlayer) {
      if (targetPlayer.isInvisible()) {
         return false;
      }
      return !targetPlayer.isCreative() && !targetPlayer.isSpectator();
   }
}
