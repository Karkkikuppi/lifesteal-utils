package dev.candycup.lifestealutils.features.alliances.service;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMembershipState;
import dev.candycup.lifestealutils.features.alliances.models.AllianceType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class LocalAllianceManager implements AllianceManager {
   @Override
   public AllianceType type() {
      return AllianceType.LOCAL;
   }

   @Override
   public CompletableFuture<List<Alliance>> fetchPlayerAlliances() {
      return CompletableFuture.completedFuture(toAllianceModels(Config.getLocalAlliances()));
   }

   @Override
   public CompletableFuture<Alliance> fetchAlliance(String allianceId) {
      List<Alliance> alliances = toAllianceModels(Config.getLocalAlliances());
      Alliance alliance = alliances.stream().filter(entry -> entry.id().equals(allianceId)).findFirst().orElse(null);
      return CompletableFuture.completedFuture(alliance);
   }

   @Override
   public CompletableFuture<Alliance> createAlliance(String name, String prefix, String color, String description, String motd) {
      List<Config.LocalAllianceConfigEntry> localAlliances = new ArrayList<>(Config.getLocalAlliances());
      long now = System.currentTimeMillis();

      Config.LocalAllianceConfigEntry entry = new Config.LocalAllianceConfigEntry();
      entry.id = "local-" + UUID.randomUUID();
      entry.name = name;
      entry.prefix = safeText(prefix);
      entry.color = safeText(color);
      entry.createdAt = now;
      entry.updatedAt = now;

      localAlliances.add(entry);
      Config.setLocalAlliances(localAlliances);

      return CompletableFuture.completedFuture(toAllianceModel(entry));
   }

   @Override
   public CompletableFuture<Alliance> updateAlliance(String allianceId, String name, String prefix, String color, String description, String motd) {
      List<Config.LocalAllianceConfigEntry> localAlliances = new ArrayList<>(Config.getLocalAlliances());
      Config.LocalAllianceConfigEntry entry = localAlliances.stream().filter(candidate -> allianceId.equals(candidate.id)).findFirst().orElse(null);
      if (entry == null) {
         return CompletableFuture.completedFuture(null);
      }

      if (name != null) {
         entry.name = name;
      }
      if (prefix != null) {
         entry.prefix = safeText(prefix);
      }
      if (color != null) {
         entry.color = safeText(color);
      }
      entry.updatedAt = System.currentTimeMillis();

      Config.setLocalAlliances(localAlliances);
      return CompletableFuture.completedFuture(toAllianceModel(entry));
   }

   @Override
   public CompletableFuture<Boolean> addMember(String allianceId, String uuid, String cachedName) {
      List<Config.LocalAllianceConfigEntry> localAlliances = new ArrayList<>(Config.getLocalAlliances());
      Config.LocalAllianceConfigEntry entry = localAlliances.stream().filter(candidate -> allianceId.equals(candidate.id)).findFirst().orElse(null);
      if (entry == null) {
         return CompletableFuture.completedFuture(false);
      }

      String normalizedTarget = normalizeUuid(uuid);
      if (normalizedTarget.isBlank()) {
         return CompletableFuture.completedFuture(false);
      }

      if (entry.members == null) {
         entry.members = new ArrayList<>();
      }

      boolean exists = entry.members.stream().anyMatch(member -> normalizeUuid(member.uuid).equals(normalizedTarget));
      if (exists) {
         return CompletableFuture.completedFuture(false);
      }

      Config.LocalAllianceMemberConfigEntry member = new Config.LocalAllianceMemberConfigEntry();
      member.id = "local-member-" + UUID.randomUUID();
      member.uuid = uuid;
      member.cachedName = safeName(cachedName, uuid);
      member.addedAt = System.currentTimeMillis();
      member.addedBy = "";
      entry.members.add(member);
      entry.updatedAt = System.currentTimeMillis();

      Config.setLocalAlliances(localAlliances);
      return CompletableFuture.completedFuture(true);
   }

   @Override
   public CompletableFuture<Boolean> removeMember(String memberId) {
      List<Config.LocalAllianceConfigEntry> localAlliances = new ArrayList<>(Config.getLocalAlliances());
      for (Config.LocalAllianceConfigEntry alliance : localAlliances) {
         if (alliance.members == null || alliance.members.isEmpty()) {
            continue;
         }

         boolean removed = alliance.members.removeIf(member -> memberId.equals(member.id));
         if (removed) {
            alliance.updatedAt = System.currentTimeMillis();
            Config.setLocalAlliances(localAlliances);
            return CompletableFuture.completedFuture(true);
         }
      }

      return CompletableFuture.completedFuture(false);
   }

   @Override
   public CompletableFuture<Boolean> acceptInvitation(String allianceId) {
      return CompletableFuture.completedFuture(false);
   }

   @Override
   public CompletableFuture<Boolean> rejectInvitation(String allianceId) {
      return CompletableFuture.completedFuture(false);
   }

   @Override
   public CompletableFuture<Boolean> deleteAlliance(String allianceId) {
      List<Config.LocalAllianceConfigEntry> localAlliances = new ArrayList<>(Config.getLocalAlliances());
      boolean removed = localAlliances.removeIf(entry -> allianceId.equals(entry.id));
      if (removed) {
         Config.setLocalAlliances(localAlliances);
      }
      return CompletableFuture.completedFuture(removed);
   }

   private static List<Alliance> toAllianceModels(List<Config.LocalAllianceConfigEntry> entries) {
      if (entries == null || entries.isEmpty()) {
         return List.of();
      }

      return entries.stream()
              .sorted(Comparator.comparingLong(entry -> entry.createdAt))
              .map(LocalAllianceManager::toAllianceModel)
              .toList();
   }

   private static Alliance toAllianceModel(Config.LocalAllianceConfigEntry entry) {
      List<AllianceMember> members = new ArrayList<>();
      List<Config.LocalAllianceMemberConfigEntry> storedMembers = entry.members == null ? List.of() : entry.members;

      for (Config.LocalAllianceMemberConfigEntry member : storedMembers) {
         String cachedName = safeName(member.cachedName, member.uuid);
         members.add(new AllianceMember(
                 member.id,
                 member.uuid,
                 cachedName,
                 AllianceMembershipState.JOINED,
                 Instant.ofEpochMilli(safeEpoch(member.addedAt)),
                 safeText(member.addedBy),
                 List.of(),
                 entry.id
         ));
      }

      return new Alliance(
              entry.id,
              safeText(entry.name),
              emptyToNull(entry.prefix),
              emptyToNull(entry.color),
              "",
              "",
              "",
              members,
              Instant.ofEpochMilli(safeEpoch(entry.createdAt)),
              Instant.ofEpochMilli(safeEpoch(entry.updatedAt)),
              AllianceType.LOCAL
      );
   }

   private static long safeEpoch(long value) {
      return value <= 0L ? System.currentTimeMillis() : value;
   }

   private static String normalizeUuid(String uuid) {
      if (uuid == null || uuid.isBlank()) {
         return "";
      }
      return uuid.replace("-", "").toLowerCase(Locale.ROOT);
   }

   private static String safeName(String cachedName, String fallback) {
      String text = safeText(cachedName);
      if (!text.isBlank()) {
         return text;
      }
      return fallback == null ? "unknown" : fallback;
   }

   private static String safeText(String value) {
      return value == null ? "" : value.trim();
   }

   private static String emptyToNull(String value) {
      String safe = safeText(value);
      return safe.isEmpty() ? null : safe;
   }
}
