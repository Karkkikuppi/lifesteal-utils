package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class LocalAllianceMigrationUtils {
   private static final String MIGRATED_ALLIANCE_NAME = "Migrated Alliance";

   public static void ensureLocalAllianceMigration() {
      if (Config.isLocalAllianceMigrationDone()) {
         repairLocalAllianceState();
         return;
      }

      Config.setLocalAllianceMigrationDone(true);

      if (Config.getAllianceUuids() == null || Config.getAllianceUuids().isEmpty()) {
         repairLocalAllianceState();
         Config.HANDLER.save();
         return;
      }

      Config.getLocalAlliances();
      if (!Config.getLocalAlliances().isEmpty()) {
         repairLocalAllianceState();
         Config.HANDLER.save();
         return;
      }

      long now = System.currentTimeMillis();
      Config.LocalAllianceConfigEntry migrated = new Config.LocalAllianceConfigEntry();
      migrated.id = "local-" + UUID.randomUUID();
      migrated.name = MIGRATED_ALLIANCE_NAME;
      migrated.prefix = "";
      migrated.color = "";
      migrated.createdAt = now;
      migrated.updatedAt = now;

      LinkedHashSet<String> deduplicated = new LinkedHashSet<>(Config.getAllianceUuids());
      for (String uuid : deduplicated) {
         if (uuid == null || uuid.isBlank()) {
            continue;
         }

         Config.LocalAllianceMemberConfigEntry member = new Config.LocalAllianceMemberConfigEntry();
         member.id = "local-member-" + UUID.randomUUID();
         member.uuid = uuid;
         member.cachedName = Config.getUuidUsernameCache().getOrDefault(uuid, uuid);
         member.addedAt = now;
         member.addedBy = "";
         migrated.members.add(member);
      }

      List<Config.LocalAllianceConfigEntry> migratedAlliances = Config.getLocalAlliances();
      migratedAlliances.add(migrated);

      Config.setLocalAlliances(migratedAlliances);
      repairLocalAllianceState();
      Config.HANDLER.save();
   }

   private static void repairLocalAllianceState() {
      List<Config.LocalAllianceConfigEntry> localAlliances = new ArrayList<>(Config.getLocalAlliances());
      List<String> preferredNames = Config.getAlliancePrefixPriority().stream()
              .map(LocalAllianceMigrationUtils::safeText)
              .filter(name -> !name.isBlank())
              .distinct()
              .toList();
      Set<String> legacyAllianceUuids = Config.getAllianceUuids() == null
              ? Set.of()
              : Config.getAllianceUuids().stream()
              .map(LocalAllianceMigrationUtils::normalizeUuid)
              .filter(uuid -> !uuid.isBlank())
              .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

      boolean changed = false;
      long now = System.currentTimeMillis();

      if (!preferredNames.isEmpty()) {
         Config.LocalAllianceConfigEntry migratedEntry = localAlliances.stream()
                 .filter(LocalAllianceMigrationUtils::isMigratedPlaceholder)
                 .findFirst()
                 .orElse(null);
         String firstPreferredName = preferredNames.getFirst();
         if (migratedEntry != null && !containsAllianceNamed(localAlliances, firstPreferredName)) {
            migratedEntry.name = firstPreferredName;
            migratedEntry.updatedAt = now;
            changed = true;
         }

         for (String preferredName : preferredNames) {
            if (containsAllianceNamed(localAlliances, preferredName)) {
               continue;
            }

            Config.LocalAllianceConfigEntry created = new Config.LocalAllianceConfigEntry();
            created.id = "local-" + UUID.randomUUID();
            created.name = preferredName;
            created.prefix = "";
            created.color = "";
            created.createdAt = now;
            created.updatedAt = now;
            localAlliances.add(created);
            changed = true;
         }
      }

      if (!legacyAllianceUuids.isEmpty()) {
         Set<String> localMemberUuids = getLocalAllianceMemberUuids(localAlliances);
         Set<String> missingLegacyUuids = new LinkedHashSet<>(legacyAllianceUuids);
         missingLegacyUuids.removeAll(localMemberUuids);
         if (missingLegacyUuids.isEmpty()) {
            if (changed) {
               Config.setLocalAlliances(localAlliances);
            }
            return;
         }

         Config.LocalAllianceConfigEntry target = chooseRepairTarget(localAlliances, preferredNames, now);
         if (target.members == null) {
            target.members = new ArrayList<>();
         }

         Set<String> existingMemberUuids = target.members.stream()
                 .map(member -> normalizeUuid(member.uuid))
                 .filter(uuid -> !uuid.isBlank())
                 .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

         for (String legacyUuid : missingLegacyUuids) {
            if (!existingMemberUuids.add(legacyUuid)) {
               continue;
            }

            Config.LocalAllianceMemberConfigEntry member = new Config.LocalAllianceMemberConfigEntry();
            member.id = "local-member-" + UUID.randomUUID();
            member.uuid = findOriginalUuidString(legacyUuid);
            member.cachedName = Config.getUuidUsernameCache().getOrDefault(member.uuid, member.uuid);
            member.addedAt = now;
            member.addedBy = "";
            target.members.add(member);
            changed = true;
         }

         if (changed) {
            target.updatedAt = now;
         }
      }

      if (changed) {
         Config.setLocalAlliances(localAlliances);
      }
   }

   private static Config.LocalAllianceConfigEntry chooseRepairTarget(List<Config.LocalAllianceConfigEntry> localAlliances,
                                                                     List<String> preferredNames,
                                                                     long now) {
      if (!preferredNames.isEmpty()) {
         String firstPreferredName = preferredNames.getFirst();
         for (Config.LocalAllianceConfigEntry alliance : localAlliances) {
            if (namesMatch(alliance.name, firstPreferredName)) {
               return alliance;
            }
         }
      }

      if (!localAlliances.isEmpty()) {
         return localAlliances.getFirst();
      }

      Config.LocalAllianceConfigEntry created = new Config.LocalAllianceConfigEntry();
      created.id = "local-" + UUID.randomUUID();
      created.name = preferredNames.isEmpty() ? MIGRATED_ALLIANCE_NAME : preferredNames.getFirst();
      created.prefix = "";
      created.color = "";
      created.createdAt = now;
      created.updatedAt = now;
      localAlliances.add(created);
      return created;
   }

   private static Set<String> getLocalAllianceMemberUuids(List<Config.LocalAllianceConfigEntry> localAlliances) {
      Set<String> memberUuids = new LinkedHashSet<>();
      for (Config.LocalAllianceConfigEntry alliance : localAlliances) {
         if (alliance.members == null || alliance.members.isEmpty()) {
            continue;
         }

         for (Config.LocalAllianceMemberConfigEntry member : alliance.members) {
            if (member != null) {
               String normalizedUuid = normalizeUuid(member.uuid);
               if (!normalizedUuid.isBlank()) {
                  memberUuids.add(normalizedUuid);
               }
            }
         }
      }
      return memberUuids;
   }

   private static boolean containsAllianceNamed(List<Config.LocalAllianceConfigEntry> localAlliances, String name) {
      for (Config.LocalAllianceConfigEntry alliance : localAlliances) {
         if (namesMatch(alliance.name, name)) {
            return true;
         }
      }
      return false;
   }

   private static boolean namesMatch(String left, String right) {
      String leftText = safeText(left);
      String rightText = safeText(right);
      return !leftText.isBlank() && leftText.equalsIgnoreCase(rightText);
   }

   private static boolean isMigratedPlaceholder(Config.LocalAllianceConfigEntry entry) {
      return entry != null && namesMatch(entry.name, MIGRATED_ALLIANCE_NAME);
   }

   private static String normalizeUuid(String uuid) {
      return safeText(uuid).replace("-", "").toLowerCase(Locale.ROOT);
   }

   private static String findOriginalUuidString(String normalizedUuid) {
      if (Config.getAllianceUuids() == null) {
         return normalizedUuid;
      }
      return Config.getAllianceUuids().stream()
              .filter(uuid -> normalizeUuid(uuid).equals(normalizedUuid))
              .findFirst()
              .orElse(normalizedUuid);
   }

   private static String safeText(String value) {
      return value == null ? "" : value.trim();
   }
}
