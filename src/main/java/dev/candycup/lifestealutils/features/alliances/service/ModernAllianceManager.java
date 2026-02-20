package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceType;
import dev.candycup.lifestealutils.gaia.AlliancesAPIClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class ModernAllianceManager implements AllianceManager {
   private static final List<String> DEFAULT_PERMISSIONS = List.of("*");

   @Override
   public AllianceType type() {
      return AllianceType.MODERN;
   }

   @Override
   public CompletableFuture<List<Alliance>> fetchPlayerAlliances() {
      return AlliancesAPIClient.fetchPlayerAlliances();
   }

   @Override
   public CompletableFuture<Alliance> fetchAlliance(String allianceId) {
      return AlliancesAPIClient.fetchAlliance(allianceId);
   }

   @Override
   public CompletableFuture<Alliance> createAlliance(String name, String prefix, String color, String description, String motd) {
      return AlliancesAPIClient.createAlliance(name, prefix, color, description, motd);
   }

   @Override
   public CompletableFuture<Alliance> updateAlliance(String allianceId, String name, String prefix, String color, String description, String motd) {
      return AlliancesAPIClient.updateAlliance(allianceId, name, prefix, color, description, motd);
   }

   @Override
   public CompletableFuture<Boolean> addMember(String allianceId, String uuid, String cachedName) {
      return AlliancesAPIClient.inviteMember(allianceId, uuid, cachedName, DEFAULT_PERMISSIONS);
   }

   @Override
   public CompletableFuture<Boolean> removeMember(String memberId) {
      return AlliancesAPIClient.removeMember(memberId);
   }

   @Override
   public CompletableFuture<Boolean> acceptInvitation(String allianceId) {
      return AlliancesAPIClient.acceptInvitation(allianceId);
   }

   @Override
   public CompletableFuture<Boolean> rejectInvitation(String allianceId) {
      return AlliancesAPIClient.rejectInvitation(allianceId);
   }

   @Override
   public CompletableFuture<Boolean> deleteAlliance(String allianceId) {
      return AlliancesAPIClient.deleteAlliance(allianceId);
   }
}
