package dev.candycup.lifestealutils.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.candycup.lifestealutils.features.alliances.AllianceModels;
import dev.candycup.lifestealutils.gaia.GaiaAuthTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Performs LSU's narrowed legacy-alliance cleanup migration.
 */
public final class MigrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/persistence/migration");
    private static final Gson GSON = new GsonBuilder().create();
    private static final String JSON_SUFFIX = ".json";
    private static volatile boolean ranThisSession;

    private MigrationController() {
    }

    /**
     * Runs the legacy-root alliance migration once per client session.
     */
    public static void runLegacyAllianceMigrationIfNeeded() {
        if (ranThisSession) {
            return;
        }
        ranThisSession = true;

        Path root = PersistentDiskManager.getRootDir();
        Path legacyAlliancesDir = root.resolve("alliances");

        List<Path> allianceFiles = Files.isDirectory(legacyAlliancesDir)
                ? listAllianceFiles(legacyAlliancesDir)
                : List.of();
        List<AllianceCandidate> candidates = parseCandidates(allianceFiles);

        int moved = 0;
        int deleted = 0;
        int skipped = 0;
        int deletedCorrupt = 0;

        for (AllianceCandidate candidate : candidates) {
            try {
                if (candidate.record.published) {
                    Files.deleteIfExists(candidate.path);
                    deleted++;
                    continue;
                }

                String ownerUuid = resolveOwnerUuid(candidate.record.ownerUuid);
                Path destinationDir = root.resolve(ownerUuid).resolve("alliances");
                Files.createDirectories(destinationDir);
                Path destination = destinationDir.resolve(candidate.path.getFileName().toString());
                Files.move(candidate.path, destination, StandardCopyOption.REPLACE_EXISTING);
                moved++;
            } catch (Exception e) {
                skipped++;
                LOGGER.warn("failed to migrate legacy alliance file {}", candidate.path, e);
            }
        }

        for (Path legacyFile : allianceFiles) {
            boolean parsed = false;
            for (AllianceCandidate candidate : candidates) {
                if (candidate.path.equals(legacyFile)) {
                    parsed = true;
                    break;
                }
            }
            if (parsed) {
                continue;
            }
            if (Files.exists(legacyFile)) {
                try {
                    Files.deleteIfExists(legacyFile);
                    deletedCorrupt++;
                } catch (Exception e) {
                    skipped++;
                    LOGGER.warn("failed to delete corrupt legacy alliance file {}", legacyFile, e);
                }
            }
        }

        TokenMigrationResult tokenMigration = migrateLegacyGaiaTokens(root);
        cleanupLegacyRootArtifacts(root);

        tryDeleteEmptyDirectory(legacyAlliancesDir);
        boolean didWork = moved > 0
                || deleted > 0
                || deletedCorrupt > 0
                || tokenMigration.consolidated() > 0
                || tokenMigration.deletedLegacy() > 0
                || skipped > 0;

        if (didWork) {
            LOGGER.info(
                    "legacy migration complete: scanned={}, moved_unpublished={}, deleted_published_cache={}, deleted_corrupt={}, consolidated_tokens={}, deleted_legacy_tokens={}, skipped={}",
                    allianceFiles.size(),
                    moved,
                    deleted,
                    deletedCorrupt,
                    tokenMigration.consolidated(),
                    tokenMigration.deletedLegacy(),
                    skipped
            );
        }
    }

    private static List<Path> listAllianceFiles(Path legacyAlliancesDir) {
        try (var stream = Files.list(legacyAlliancesDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(JSON_SUFFIX))
                    .toList();
        } catch (IOException e) {
            LOGGER.warn("failed to list legacy alliances in {}", legacyAlliancesDir, e);
            return List.of();
        }
    }

    private static List<AllianceCandidate> parseCandidates(List<Path> allianceFiles) {
        ArrayList<AllianceCandidate> out = new ArrayList<>();
        for (Path path : allianceFiles) {
            try {
                String json = Files.readString(path);
                AllianceModels.AllianceRecord record = GSON.fromJson(json, AllianceModels.AllianceRecord.class);
                if (record == null) {
                    continue;
                }
                out.add(new AllianceCandidate(path, record));
            } catch (Exception e) {
                LOGGER.warn("failed to parse legacy alliance {}", path, e);
            }
        }
        return out;
    }

    private static String resolveOwnerUuid(String ownerUuid) {
        if (ownerUuid != null && !ownerUuid.isBlank()) {
            try {
                String normalized = ownerUuid.trim().replace("-", "");
                if (normalized.length() == 32) {
                    String dashed = normalized.substring(0, 8) + "-"
                            + normalized.substring(8, 12) + "-"
                            + normalized.substring(12, 16) + "-"
                            + normalized.substring(16, 20) + "-"
                            + normalized.substring(20);
                    return UUID.fromString(dashed).toString();
                }
                return UUID.fromString(ownerUuid.trim()).toString();
            } catch (Exception ignored) {
            }
        }
        // Recommended fallback: keep data safe by assigning unknown-owner legacy
        // alliances to the current session folder instead of dropping them.
        return PersistentDiskManager.getCurrentSessionUuidString();
    }

    private static void tryDeleteEmptyDirectory(Path directory) {
        try (var stream = Files.list(directory)) {
            if (stream.findAny().isEmpty()) {
                Files.deleteIfExists(directory);
            }
        } catch (Exception ignored) {
        }
    }

    private static void cleanupLegacyRootArtifacts(Path root) {
        deleteRecursively(root.resolve("alliances"));
        deleteRecursively(root.resolve("gaia"));
        deleteIfExists(root.resolve("knowledge.json"));
        deleteIfExists(root.resolve("player_profile_cache.json"));
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            LOGGER.warn("failed to delete legacy path {}", path, e);
        }
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            LOGGER.warn("failed to recursively delete legacy path {}", path, e);
        }
    }

    private static TokenMigrationResult migrateLegacyGaiaTokens(Path root) {
        int deleted = 0;
        int consolidated = 0;
        try (var stream = Files.list(root)) {
            List<Path> roots = stream.filter(Files::isDirectory).toList();
            for (Path userRoot : roots) {
                String folderName = userRoot.getFileName().toString();
                if ("alliances".equals(folderName) || "gaia".equals(folderName)) {
                    continue;
                }
                Path authDir = userRoot.resolve("gaia").resolve("authentication");
                if (!Files.isDirectory(authDir)) {
                    continue;
                }
                Path newTokenPath = authDir.resolve(GaiaAuthTokenStore.TOKEN_FILE_NAME);
                try (var authFiles = Files.list(authDir)) {
                    List<Path> legacyFiles = new ArrayList<>();
                    for (Path file : authFiles.toList()) {
                        String fileName = file.getFileName().toString();
                        if (!fileName.endsWith(GaiaAuthTokenStore.LEGACY_TOKEN_SUFFIX)) {
                            continue;
                        }
                        legacyFiles.add(file);
                    }

                    if (!Files.exists(newTokenPath)) {
                        for (Path legacyFile : legacyFiles) {
                            try {
                                String token = Files.readString(legacyFile).trim();
                                if (token.isBlank()) {
                                    continue;
                                }
                                Files.writeString(newTokenPath, token);
                                consolidated++;
                                break;
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    for (Path legacyFile : legacyFiles) {
                        if (Files.deleteIfExists(legacyFile)) {
                            deleted++;
                        }
                    }
                }
                tryDeleteEmptyDirectory(authDir);
            }
        } catch (Exception e) {
            LOGGER.warn("failed to invalidate legacy Gaia token files", e);
        }
        return new TokenMigrationResult(consolidated, deleted);
    }

    private record TokenMigrationResult(int consolidated, int deletedLegacy) {
    }

    private record AllianceCandidate(Path path, AllianceModels.AllianceRecord record) {
    }
}
