package dev.candycup.lifestealutils.gaia;

import dev.candycup.lifestealutils.features.alliances.models.Alliance;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * compatibility wrapper for alliances Gaia APIs.
 */
public final class AlliancesAPIClient {
   private AlliancesAPIClient() {
   }

   public static CompletableFuture<List<Alliance>> fetchPlayerAlliances() {
      return GaiaApiClient.getInstance().alliances().fetchPlayerAlliances();
   }

   public static CompletableFuture<List<Alliance>> fetchPlayerInvites() {
      return GaiaApiClient.getInstance().alliances().fetchPlayerInvites();
   }

   public static CompletableFuture<Alliance> fetchAlliance(String allianceId) {
      return GaiaApiClient.getInstance().alliances().fetchAlliance(allianceId);
   }

   public static CompletableFuture<Alliance> createAlliance(String name, String prefix, String color, String description, String motd) {
      return GaiaApiClient.getInstance().alliances().createAlliance(name, prefix, color, description, motd);
   }

   public static CompletableFuture<Alliance> updateAlliance(String allianceId, String name, String prefix, String color, String description, String motd) {
      return GaiaApiClient.getInstance().alliances().updateAlliance(allianceId, name, prefix, color, description, motd);
   }

   public static CompletableFuture<Boolean> inviteMember(String allianceId, String uuid, String cachedName, List<String> permissions) {
      return GaiaApiClient.getInstance().alliances().inviteMember(allianceId, uuid, cachedName, permissions);
   }

   public static CompletableFuture<Boolean> removeMember(String memberId) {
      return GaiaApiClient.getInstance().alliances().removeMember(memberId);
   }

   public static CompletableFuture<Boolean> acceptInvitation(String allianceId) {
      return GaiaApiClient.getInstance().alliances().acceptInvitation(allianceId);
   }

   public static CompletableFuture<Boolean> rejectInvitation(String allianceId) {
      return GaiaApiClient.getInstance().alliances().rejectInvitation(allianceId);
   }

   public static CompletableFuture<Boolean> deleteAlliance(String allianceId) {
      return GaiaApiClient.getInstance().alliances().deleteAlliance(allianceId);
   }
}
