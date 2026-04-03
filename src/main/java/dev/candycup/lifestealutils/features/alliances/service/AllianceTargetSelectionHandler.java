package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
      if (client.screen != null || client.player == null || client.level == null) {
         return;
      }

      Player targetPlayer = resolveTargetPlayer(client);
      if (targetPlayer == null) {
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

   private static Player resolveTargetPlayer(Minecraft client) {
      HitResult hitResult = client.hitResult;
      if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player targetPlayer) {
         return targetPlayer;
      }

      EntityHitResult fallbackHit = findPlayerFallbackHit(client);
      if (fallbackHit == null || !(fallbackHit.getEntity() instanceof Player targetPlayer)) {
         return null;
      }
      return targetPlayer;
   }

   private static EntityHitResult findPlayerFallbackHit(Minecraft client) {
      Player player = client.player;
      if (player == null || client.level == null) {
         return null;
      }

      double maxDistance = player.entityInteractionRange();
      HitResult hitResult = client.hitResult;
      if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
         maxDistance = Math.min(maxDistance, hitResult.distanceTo(player));
      }
      if (maxDistance <= 0.0D) {
         return null;
      }

      Vec3 eyePosition = player.getEyePosition();
      Vec3 viewVector = player.getViewVector(1.0F);
      Vec3 reachVector = viewVector.scale(maxDistance);
      Vec3 targetPosition = eyePosition.add(reachVector);
      AABB searchBox = player.getBoundingBox().expandTowards(reachVector).inflate(1.0D);

      // Vanilla hit results ignore invisible players, so manually raycast them here.
      return ProjectileUtil.getEntityHitResult(
              player,
              eyePosition,
              targetPosition,
              searchBox,
              entity -> entity instanceof Player targetPlayer
                      && targetPlayer != player
                      && isTargetEligible(targetPlayer),
              maxDistance * maxDistance
      );
   }

   /**
    * determines whether the target player can be selected.
    *
    * @param targetPlayer the targeted player entity
    * @return true when the player is selectable
    */
   private static boolean isTargetEligible(Player targetPlayer) {
      if (targetPlayer.isCreative() || targetPlayer.isSpectator()) {
         return false;
      }

      return true;
   }
}
