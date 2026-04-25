package dev.candycup.lifestealutils.features.alliances;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.candycup.lifestealutils.Config;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AllianceStorageService {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliances/storage");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String ROOT_DIR = "lifestealutils";
   private static final String ALLIANCES_DIR = "alliances";
   private static final String JSON_SUFFIX = ".json";

   private AllianceStorageService() {
   }

   public static void ensureInitialized() {
      migrateLegacyIfNeeded();
      try {
         Files.createDirectories(getStorageDir());
      } catch (Exception e) {
         LOGGER.warn("failed to initialize alliances storage directory", e);
      }
   }

   public static List<AllianceModels.AllianceRecord> loadAll() {
      ensureInitialized();
      ArrayList<AllianceModels.AllianceRecord> out = new ArrayList<>();
      try (var stream = Files.list(getStorageDir())) {
         stream.filter(path -> path.getFileName().toString().endsWith(JSON_SUFFIX))
                 .forEach(path -> {
                    AllianceModels.AllianceRecord record = loadByPath(path);
                    if (record != null) {
                       out.add(record);
                    }
                 });
      } catch (Exception e) {
         LOGGER.warn("failed to load alliances", e);
      }

      out.sort(Comparator.comparingInt(a -> a.order));
      return out;
   }

   public static AllianceModels.AllianceRecord loadByClientId(String clientId) {
      if (clientId == null || clientId.isBlank()) {
         return null;
      }
      return loadByPath(getStorageDir().resolve(clientId + JSON_SUFFIX));
   }

   public static void save(AllianceModels.AllianceRecord record) {
      if (record == null) {
         return;
      }
      if (record.clientId == null || record.clientId.isBlank()) {
         record.clientId = AllianceIdGenerator.newClientId();
      }
      if (record.data == null) {
         record.data = new AllianceModels.AllianceData();
      }
      if (record.data.lists == null) {
         record.data.lists = new ArrayList<>();
      }
      long now = System.currentTimeMillis();
      if (record.createdAt <= 0L) {
         record.createdAt = now;
      }
      record.updatedAt = now;

      normalizeLists(record);

      try {
         Files.createDirectories(getStorageDir());
         Path target = getStorageDir().resolve(record.clientId + JSON_SUFFIX);
         Path temp = getStorageDir().resolve(record.clientId + ".tmp");
         try (Writer writer = Files.newBufferedWriter(temp)) {
            GSON.toJson(record, writer);
         }
         Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (Exception e) {
         LOGGER.warn("failed to save alliance {}", record.clientId, e);
      }
   }

   public static void delete(String clientId) {
      if (clientId == null || clientId.isBlank()) {
         return;
      }
      try {
         Files.deleteIfExists(getStorageDir().resolve(clientId + JSON_SUFFIX));
      } catch (Exception e) {
         LOGGER.warn("failed to delete alliance {}", clientId, e);
      }
   }

   private static void normalizeLists(AllianceModels.AllianceRecord record) {
      for (AllianceModels.AlliancePlayerList list : record.data.lists) {
         if (list.id == null || list.id.isBlank()) {
            list.id = AllianceIdGenerator.newListId();
         }
         if (list.members == null) {
            list.members = new ArrayList<>();
         }
         if (list.prefixColor < 0 || list.prefixColor > 0xFFFFFF) {
            list.prefixColor = 0x55FF55;
         }
         if (list.nameColor < 0 || list.nameColor > 0xFFFFFF) {
            list.nameColor = 0xFFFFFF;
         }
      }
      if (record.lastUsedListId == null || record.lastUsedListId.isBlank()) {
         if (!record.data.lists.isEmpty()) {
            record.lastUsedListId = record.data.lists.get(record.data.lists.size() - 1).id;
         }
      }
   }

   private static AllianceModels.AllianceRecord loadByPath(Path path) {
      if (path == null || !Files.exists(path)) {
         return null;
      }
      try (Reader reader = Files.newBufferedReader(path)) {
         AllianceModels.AllianceRecord record = GSON.fromJson(reader, AllianceModels.AllianceRecord.class);
         if (record == null) {
            return null;
         }
         if (record.clientId == null || record.clientId.isBlank()) {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(JSON_SUFFIX)) {
               record.clientId = fileName.substring(0, fileName.length() - JSON_SUFFIX.length());
            }
         }
         if (record.data == null) {
            record.data = new AllianceModels.AllianceData();
         }
         if (record.data.lists == null) {
            record.data.lists = new ArrayList<>();
         }
         normalizeLists(record);
         return record;
      } catch (Exception e) {
         LOGGER.warn("failed to read alliance file {}", path, e);
         return null;
      }
   }

   private static Path getStorageDir() {
      return FabricLoader.getInstance().getGameDir()
              .resolve(ROOT_DIR)
              .resolve(ALLIANCES_DIR);
   }

   private static void migrateLegacyIfNeeded() {
      if (hasAnyAllianceFiles()) {
         return;
      }

      List<Config.LocalAllianceConfigEntry> oldEntries = Config.getLocalAlliances();
      if (oldEntries.isEmpty()) {
         return;
      }

      int order = 0;
      for (Config.LocalAllianceConfigEntry old : oldEntries) {
         AllianceModels.AllianceRecord record = new AllianceModels.AllianceRecord();
         record.clientId = (old.id == null || old.id.isBlank()) ? AllianceIdGenerator.newClientId() : old.id;
         record.canEdit = true;
         record.source = "local";
         record.syncState = "LOCAL";
         record.createdAt = old.createdAt;
         record.updatedAt = old.updatedAt;
         record.order = order++;
         record.data = new AllianceModels.AllianceData();
         record.data.name = old.name == null ? "" : old.name;
         record.data.description = "";
         record.data.color = parseLegacyColor(old.color);

         AllianceModels.AlliancePlayerList list = new AllianceModels.AlliancePlayerList();
         list.id = AllianceIdGenerator.newListId();
         list.name = "Members";
         list.prefix = old.prefix == null ? "" : old.prefix;
         list.prefixColor = parseLegacyColor(old.color);
         list.nameColor = 0xFFFFFF;

         for (Config.LocalAllianceMemberConfigEntry oldMember : old.members) {
            AllianceModels.AllianceMember member = new AllianceModels.AllianceMember();
            member.uuid = oldMember.uuid == null ? "" : oldMember.uuid;
            member.addedAt = oldMember.addedAt;
            list.members.add(member);
         }
         record.data.lists.add(list);
         record.lastUsedListId = list.id;
         save(record);
      }

      LOGGER.info("migrated {} legacy local alliances", oldEntries.size());
   }

   private static boolean hasAnyAllianceFiles() {
      try {
         Path storageDir = getStorageDir();
         if (!Files.exists(storageDir)) {
            return false;
         }
         try (var stream = Files.list(storageDir)) {
            return stream.anyMatch(path -> path.getFileName().toString().endsWith(JSON_SUFFIX));
         }
      } catch (Exception ignored) {
         return false;
      }
   }

   private static int parseLegacyColor(String legacyColor) {
      if (legacyColor == null || legacyColor.isBlank()) {
         return 0x55FF55;
      }
      String c = legacyColor.trim();
      if (c.startsWith("#")) {
         c = c.substring(1);
      }
      if (c.length() != 6) {
         return 0x55FF55;
      }
      try {
         return Integer.parseInt(c, 16);
      } catch (NumberFormatException e) {
         return 0x55FF55;
      }
   }
}
