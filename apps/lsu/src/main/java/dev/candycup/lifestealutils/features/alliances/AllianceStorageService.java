package dev.candycup.lifestealutils.features.alliances;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.persistence.PersistentDiskManager;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads and saves alliance JSON files. All disk paths are resolved through
 * {@link PersistentDiskManager} so files live under the current Minecraft
 * account's per-user folder ({@code lifestealutils/<uuid>/alliances/}).
 */
public final class AllianceStorageService {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliances/storage");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String ALLIANCES_DIR = "alliances";
   private static final String JSON_SUFFIX = ".json";

    @Setter
    @SerialEntry(comment = "Locally stored alliances")
    private static List<LocalAllianceConfigEntry> localAlliances = new ArrayList<>();

   private AllianceStorageService() {
   }

    public static List<LocalAllianceConfigEntry> getLocalAlliances() {
        return localAlliances == null ? new ArrayList<>() : new ArrayList<>(localAlliances);
    }

    public static class LocalAllianceConfigEntry {
        public String id = "";
        public String name = "";
        public String prefix = "";
        public String color = "";
        public long createdAt = 0L;
        public long updatedAt = 0L;
        public List<LocalAllianceMemberConfigEntry> members = new ArrayList<>();
    }

    public static class LocalAllianceMemberConfigEntry {
        public String id = "";
        public String uuid = "";
        public String cachedName = "";
        public long addedAt = 0L;
        public String addedBy = "";
    }

   public static void ensureInitialized() {
      migrateLegacyConfigEntriesIfNeeded();
      try {
         PersistentDiskManager.resolveUserDir(ALLIANCES_DIR);
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
                        if (record.published || "remote".equalsIgnoreCase(record.source)) {
                           // Remote alliances are Gaia-backed and now in-memory only.
                           // If an old disk cache exists, drop it and skip loading.
                           PersistentDiskManager.deleteIfExists(path);
                           return;
                        }
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

      Path target = getStorageDir().resolve(record.clientId + JSON_SUFFIX);
      StringWriter buffer = new StringWriter();
      try (Writer writer = buffer) {
         GSON.toJson(record, writer);
      } catch (Exception e) {
         LOGGER.warn("failed to serialize alliance {}", record.clientId, e);
         return;
      }
      if (!PersistentDiskManager.writeAtomic(target, buffer.toString())) {
         LOGGER.warn("failed to save alliance {}", record.clientId);
      }
   }

   public static void delete(String clientId) {
      if (clientId == null || clientId.isBlank()) {
         return;
      }
      PersistentDiskManager.deleteIfExists(getStorageDir().resolve(clientId + JSON_SUFFIX));
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
      return PersistentDiskManager.resolveUserDir(ALLIANCES_DIR);
   }

   /**
    * Migrates Configura-stored alliance entries (the pre-JSON format) into
    * JSON files. Unrelated to the per-account migration handled by
    * {@code MigrationController} - this one converts an even older config
    * representation into the on-disk format we use today.
    */
   private static void migrateLegacyConfigEntriesIfNeeded() {
      if (hasAnyAllianceFiles()) {
         return;
      }

       List<LocalAllianceConfigEntry> oldEntries = getLocalAlliances();
      if (oldEntries.isEmpty()) {
         return;
      }

      int order = 0;
       for (LocalAllianceConfigEntry old : oldEntries) {
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

           for (LocalAllianceMemberConfigEntry oldMember : old.members) {
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
