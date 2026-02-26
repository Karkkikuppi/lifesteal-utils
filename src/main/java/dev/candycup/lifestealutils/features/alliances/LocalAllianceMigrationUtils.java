package dev.candycup.lifestealutils.features.alliances;

import dev.candycup.lifestealutils.Config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class LocalAllianceMigrationUtils {
   public static void ensureLocalAllianceMigration() {
      if (Config.isLocalAllianceMigrationDone()) {
         return;
      }

      Config.setLocalAllianceMigrationDone(true);

      if (Config.getAllianceUuids() == null || Config.getAllianceUuids().isEmpty()) {
         Config.HANDLER.save();
         return;
      }

      Config.getLocalAlliances();
      if (!Config.getLocalAlliances().isEmpty()) {
         Config.HANDLER.save();
         return;
      }

      long now = System.currentTimeMillis();
      Config.LocalAllianceConfigEntry migrated = new Config.LocalAllianceConfigEntry();
      migrated.id = "local-" + UUID.randomUUID();
      migrated.name = "Migrated Alliance";
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
      Config.HANDLER.save();
   }

}
