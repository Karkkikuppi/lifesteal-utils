package dev.candycup.lifestealutils.persistence;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single source of truth for every non-Configura disk read/write LSU performs.
 */
public final class PersistentDiskManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/persistence");
    private static final String ROOT_DIR_NAME = "lifestealutils";

    /**
     * Cached once on first use. Minecraft sessions are immutable per-launch,
     * so we never need to invalidate this.
     */
    private static volatile UUID cachedSessionUuid;

    /**
     * Flips to true when either (a) no legacy data was detected at startup,
     * so the per-account layout is already correct, or (b) {@link MigrationController}
     * finished moving legacy files into {@code lifestealutils/<uuid>/}.
     */
    private static final AtomicBoolean userStorageReady = new AtomicBoolean(false);

    private PersistentDiskManager() {
    }

    /**
     * Returns the LSU root folder ({@code <gamedir>/lifestealutils/}).
     * <p>
     * Intended only for the migration controller's legacy-detection logic and
     * for {@link PersistenceVersionStamp} when it needs to enumerate per-user
     * directories. Feature code must never call this directly - use
     * {@link #resolveUserPath(String...)} or {@link #resolveUserDir(String...)}
     * so writes land under the per-account subfolder.
     *
     * @return the path to {@code <gamedir>/lifestealutils/}
     */
    public static Path getRootDir() {
        return FabricLoader.getInstance().getGameDir().resolve(ROOT_DIR_NAME);
    }

    /**
     * Returns the current user's LSU folder ({@code lifestealutils/<sessionUuid>/}),
     * creating it if missing.
     *
     * @return the per-account folder for the current Minecraft session
     * @throws IllegalStateException if storage is not yet ready (migration pending)
     */
    public static Path getCurrentUserDir() {
        if (!userStorageReady.get()) {
            // Fail loud: callers reading user data before migration would see
            // empty data and silently corrupt the user's experience.
            throw new IllegalStateException(
                    "PersistentDiskManager: user storage is not ready yet. "
                            + "This call must happen after LsuStartupController.runDataBootstrap()."
            );
        }
        Path dir = getRootDir().resolve(getCurrentSessionUuidString());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.warn("failed to create user directory {}", dir, e);
        }
        return dir;
    }

    /**
     * Resolves a path inside the current user's folder. Parent directories
     * are NOT created (use {@link #resolveUserDir(String...)} if you need
     * the directory to exist).
     *
     * @param segments path segments under the per-user folder
     * @return resolved path under {@code lifestealutils/<sessionUuid>/}
     * @throws IllegalStateException if storage is not yet ready
     */
    public static Path resolveUserPath(String... segments) {
        Path path = getCurrentUserDir();
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path;
    }

    /**
     * Resolves a path inside the current user's folder and ensures every
     * directory in the chain exists. Use for the directory itself, not for
     * a target file (the file's parent is what we'd want - call
     * {@link #resolveUserPath(String...)} for that).
     *
     * @param segments path segments under the per-user folder; the resolved
     *                 path is treated as a directory and created
     * @return resolved directory path
     * @throws IllegalStateException if storage is not yet ready
     */
    public static Path resolveUserDir(String... segments) {
        Path dir = resolveUserPath(segments);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.warn("failed to create directory {}", dir, e);
        }
        return dir;
    }

    /**
     * Atomically writes a UTF-8 string to {@code target}.
     *
     * @param target   the final destination file
     * @param contents the UTF-8 string to write
     * @return true on successful write, false if the IO failed entirely
     */
    public static boolean writeAtomic(Path target, String contents) {
        return writeAtomic(target, contents.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Byte-level overload of {@link #writeAtomic(Path, String)}.
     *
     * @param target   the final destination file
     * @param contents the bytes to write
     * @return true on successful write, false if the IO failed entirely
     */
    public static boolean writeAtomic(Path target, byte[] contents) {
        Path parent = target.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                LOGGER.warn("failed to create parent directory {}", parent, e);
                return false;
            }
        }

        Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try {
            Files.write(temp, contents);
        } catch (IOException e) {
            LOGGER.warn("failed to write temp file {}", temp, e);
            return false;
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException atomicFailure) {
            // ATOMIC_MOVE is not always supported (Windows + special volumes,
            // for example). Fall back to a plain replace and continue.
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debug("atomic move not supported for {}, used non-atomic replace", target);
                return true;
            } catch (IOException nonAtomicFailure) {
                LOGGER.warn("failed to move {} -> {}", temp, target, nonAtomicFailure);
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
                return false;
            }
        }
    }

    /**
     * Reads {@code target} as a UTF-8 string. Returns empty if the file does
     * not exist; logs a warning and returns empty on any other IO error.
     *
     * @param target the file to read
     * @return the string contents, or empty if missing/unreadable
     */
    public static Optional<String> readString(Path target) {
        try {
            return Optional.of(Files.readString(target, StandardCharsets.UTF_8));
        } catch (NoSuchFileException missing) {
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.warn("failed to read file {}", target, e);
            return Optional.empty();
        }
    }

    /**
     * Wraps {@link Files#deleteIfExists(Path)} with consistent logging.
     *
     * @param target the file to delete
     */
    public static void deleteIfExists(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            LOGGER.warn("failed to delete file {}", target, e);
        }
    }

    /**
     * Returns whether per-user storage is initialized and safe to read/write.
     * Only flips to true after either (a) startup observed no legacy data
     * (nothing to migrate) or (b) the migration controller finished moving
     * files. Until then, the user-dir helpers throw.
     *
     * @return true when user storage is ready
     */
    public static boolean isUserStorageReady() {
        return userStorageReady.get();
    }

    /**
     * Marks per-user storage as ready. Called from
     * {@link dev.candycup.lifestealutils.LsuStartupController} once it has
     * confirmed that either no migration is needed or that one has just
     * completed. Idempotent.
     */
    public static void markUserStorageReady() {
        userStorageReady.set(true);
    }

    /**
     * Returns the current Minecraft session's profile UUID as a dashed string,
     * caching it on first call. Used as the per-user folder name.
     *
     * @return the dashed UUID string for the current session
     */
    public static String getCurrentSessionUuidString() {
        return getCurrentSessionUuid().toString();
    }

    /**
     * Returns the current Minecraft session's profile UUID.
     *
     * @return the cached session UUID
     */
    public static UUID getCurrentSessionUuid() {
        UUID cached = cachedSessionUuid;
        if (cached != null) {
            return cached;
        }
        synchronized (PersistentDiskManager.class) {
            if (cachedSessionUuid == null) {
                cachedSessionUuid = Minecraft.getInstance().getUser().getProfileId();
            }
            return cachedSessionUuid;
        }
    }
}
