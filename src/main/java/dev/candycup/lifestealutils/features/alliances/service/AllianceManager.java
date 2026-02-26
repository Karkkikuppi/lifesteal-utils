package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AllianceManager {
   AllianceType type();

   CompletableFuture<List<Alliance>> fetchPlayerAlliances();

   CompletableFuture<Alliance> fetchAlliance(String allianceId);

   CompletableFuture<Alliance> createAlliance(String name, String prefix, String color, String description, String motd);

   CompletableFuture<Alliance> updateAlliance(String allianceId, String name, String prefix, String color, String description, String motd);

   CompletableFuture<Boolean> addMember(String allianceId, String uuid, String cachedName);

   CompletableFuture<Boolean> removeMember(String memberId);

   CompletableFuture<Boolean> acceptInvitation(String allianceId);

   CompletableFuture<Boolean> rejectInvitation(String allianceId);

   CompletableFuture<Boolean> deleteAlliance(String allianceId);
}
