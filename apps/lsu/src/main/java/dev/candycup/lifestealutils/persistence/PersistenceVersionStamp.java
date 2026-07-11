package dev.candycup.lifestealutils.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the per-user "schema stamp" file that records which
 * disk-layout migrations have been applied to a given user folder.
 */
public final class PersistenceVersionStamp {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/persistence/version");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * The current latest schema version. Bump alongside adding a new
     * forward-migration step in {@link MigrationController}.
     */
    public static final int LATEST_VERSION = 1;

    /**
     * Filename of the version stamp inside each user folder. Long on
     * purpose; see class javadoc.
     */
    public static final String FILE_NAME =
            "do-not-modify-or-delete-unless-you-delete-whole-folder-this-is-in-in-which-case-totally-fine.json";

    private PersistenceVersionStamp() {
    }

    /**
     * Returns the version recorded for the given user folder, or 0 if no
     * stamp file is present (treat as "fresh, never migrated").
     *
     * @param userDir the user folder to inspect
     * @return the recorded version, or 0 if missing/unreadable
     */
    public static int getCurrentVersion(Path userDir) {
        Path stamp = userDir.resolve(FILE_NAME);
        if (!Files.exists(stamp)) {
            return 0;
        }
        return PersistentDiskManager.readString(stamp)
                .map(PersistenceVersionStamp::parseVersion)
                .orElse(0);
    }

    /**
     * Appends a migration entry to the stamp file (creating it if absent),
     * bumping {@code version} to the supplied {@code newVersion}.
     * <p>
     * Written atomically via {@link PersistentDiskManager#writeAtomic(Path, String)}
     * so a partial crash cannot leave a half-written stamp that would
     * confuse the next launch.
     *
     * @param userDir    the user folder to stamp
     * @param newVersion the new schema version to record
     * @param note       short human-readable description of what changed
     */
    public static void appendMigration(Path userDir, int newVersion, String note) {
        Path stamp = userDir.resolve(FILE_NAME);
        StampFile current = PersistentDiskManager.readString(stamp)
                .map(PersistenceVersionStamp::parseStamp)
                .orElseGet(StampFile::new);

        MigrationEntry entry = new MigrationEntry();
        entry.id = newVersion;
        entry.appliedAt = Instant.now().toString();
        entry.note = note;
        if (current.migrations == null) {
            current.migrations = new ArrayList<>();
        }
        current.migrations.add(entry);
        current.version = newVersion;

        String json = GSON.toJson(current);
        if (!PersistentDiskManager.writeAtomic(stamp, json)) {
            LOGGER.warn("failed to append migration entry to {}", stamp);
        }
    }

    private static int parseVersion(String json) {
        StampFile parsed = parseStamp(json);
        return parsed == null ? 0 : parsed.version;
    }

    private static StampFile parseStamp(String json) {
        try {
            StampFile parsed = GSON.fromJson(json, StampFile.class);
            return parsed != null ? parsed : new StampFile();
        } catch (Exception e) {
            LOGGER.warn("failed to parse persistence version stamp", e);
            return new StampFile();
        }
    }

    /**
     * Serialized shape of the stamp file. Public for Gson reflection.
     */
    public static final class StampFile {
        public int version;
        public List<MigrationEntry> migrations = new ArrayList<>();
    }

    /**
     * One migration record inside {@link StampFile#migrations}.
     */
    public static final class MigrationEntry {
        public int id;
        public String appliedAt;
        public String note;
    }
}
