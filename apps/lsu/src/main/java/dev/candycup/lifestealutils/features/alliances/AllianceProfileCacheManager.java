package dev.candycup.lifestealutils.features.alliances;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import dev.candycup.lifestealutils.persistence.PersistentDiskManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AllianceProfileCacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliances/profile-cache");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(6);
    private static final String CACHE_FILE = "player_profile_cache.json";
    private static final int BULK_LOOKUP_BATCH_SIZE = 100;
    private static final int MAX_ENTRIES = 5_000;
    private static final long MAX_AGE_MS = 30L * 24 * 60 * 60 * 1_000; // 30 days

    private static final Map<String, CachedProfile> UUID_TO_PROFILE = new ConcurrentHashMap<>();
    private static final Map<String, String> NAME_TO_UUID = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Void>> PENDING_UUID_LOOKUPS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Void>> PENDING_NAME_LOOKUPS = new ConcurrentHashMap<>();

    private static volatile boolean initialized = false;
    private static volatile long lastPersistedAt = 0L;

    private AllianceProfileCacheManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        loadFromDisk();
        observeWorldPlayers();
    }

    public static void observeWorldPlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) {
            return;
        }
        for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
            try {
                //? if >1.21.8 {
                String uuid = normalizeUuid(playerInfo.getProfile().id().toString());
                String name = playerInfo.getProfile().name();
                //?} else {
            /*String uuid = normalizeUuid(playerInfo.getProfile().getId().toString());
            String name = playerInfo.getProfile().getName();
            *///?}
                cache(name, uuid);
            } catch (Exception ignored) {
            }
        }
    }

    public static String getCachedNameByUuid(String uuid) {
        String normalized = normalizeUuid(uuid);
        if (normalized == null) {
            return null;
        }
        CachedProfile profile = UUID_TO_PROFILE.get(normalized);
        return profile == null ? null : profile.name;
    }

    public static String getCachedUuidByName(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return NAME_TO_UUID.get(username.trim().toLowerCase(Locale.ROOT));
    }

    public static void cache(String username, String uuid) {
        String normalized = normalizeUuid(uuid);
        if (normalized == null || username == null || username.isBlank()) {
            return;
        }
        String cleanedName = username.trim();
        UUID_TO_PROFILE.put(normalized, new CachedProfile(normalized, cleanedName, System.currentTimeMillis()));
        NAME_TO_UUID.put(cleanedName.toLowerCase(Locale.ROOT), normalized);
        persistSoon();
    }

    public static String resolveUuidFromInput(String usernameOrUuid) {
        if (usernameOrUuid == null || usernameOrUuid.isBlank()) {
            return null;
        }
        initialize();
        observeWorldPlayers();

        String normalized = normalizeUuid(usernameOrUuid);
        if (normalized != null) {
            return normalized;
        }

        String cached = getCachedUuidByName(usernameOrUuid);
        if (cached != null) {
            return cached;
        }

        resolveUuidsBulk(List.of(usernameOrUuid.trim()));
        return getCachedUuidByName(usernameOrUuid);
    }

    public static void resolveUuidsBulk(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return;
        }
        initialize();
        observeWorldPlayers();

        ArrayList<String> unresolved = new ArrayList<>();
        for (String username : usernames) {
            if (username == null || username.isBlank()) {
                continue;
            }
            String key = username.trim().toLowerCase(Locale.ROOT);
            if (!NAME_TO_UUID.containsKey(key)) {
                unresolved.add(username.trim());
            }
        }
        if (unresolved.isEmpty()) {
            return;
        }

        int index = 0;
        while (index < unresolved.size()) {
            int end = Math.min(index + BULK_LOOKUP_BATCH_SIZE, unresolved.size());
            List<String> batch = unresolved.subList(index, end);
            lookupUuidsBatch(batch);
            index = end;
        }
    }

    public static void queueUuidLookupForName(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        initialize();
        observeWorldPlayers();

        String key = username.trim().toLowerCase(Locale.ROOT);
        if (NAME_TO_UUID.containsKey(key) || PENDING_NAME_LOOKUPS.containsKey(key)) {
            return;
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> resolveUuidsBulk(List.of(username.trim())))
                .whenComplete((unused, error) -> PENDING_NAME_LOOKUPS.remove(key));
        PENDING_NAME_LOOKUPS.put(key, future);
    }

    public static void queueNameLookupForUuid(String uuid) {
        String normalized = normalizeUuid(uuid);
        if (normalized == null) {
            return;
        }
        initialize();
        observeWorldPlayers();

        if (UUID_TO_PROFILE.containsKey(normalized) || PENDING_UUID_LOOKUPS.containsKey(normalized)) {
            return;
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + normalized.replace("-", "");
            NetworkUtilsController.HttpResult result = NetworkUtilsController.get(url, LOOKUP_TIMEOUT, 0);
            if (!result.success() || result.body() == null || result.body().isBlank()) {
                return;
            }
            try {
                JsonObject root = JsonParser.parseString(result.body()).getAsJsonObject();
                if (root == null || !root.has("name")) {
                    return;
                }
                String username = root.get("name").getAsString();
                cache(username, normalized);
            } catch (Exception ignored) {
            }
        }).whenComplete((unused, error) -> PENDING_UUID_LOOKUPS.remove(normalized));

        PENDING_UUID_LOOKUPS.put(normalized, future);
    }

    private static void lookupUuidsBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return;
        }
        JsonArray payload = new JsonArray();
        for (String username : batch) {
            payload.add(username);
        }

        NetworkUtilsController.HttpResult result = NetworkUtilsController.postJson(
                "https://api.mojang.com/profiles/minecraft",
                payload.toString(),
                LOOKUP_TIMEOUT,
                0
        );
        if (!result.success() || result.body() == null || result.body().isBlank()) {
            return;
        }

        try {
            JsonElement parsed = JsonParser.parseString(result.body());
            if (!parsed.isJsonArray()) {
                return;
            }
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                if (!object.has("id") || !object.has("name")) {
                    continue;
                }
                String uuid = normalizeUuid(object.get("id").getAsString());
                String name = object.get("name").getAsString();
                cache(name, uuid);
            }
        } catch (Exception ignored) {
        }
    }

    private static synchronized void loadFromDisk() {
        Path path = cacheFilePath();
        if (!Files.exists(path)) {
            return;
        }
        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
        try (Reader reader = Files.newBufferedReader(path)) {
            CachedProfileFile file = GSON.fromJson(reader, CachedProfileFile.class);
            if (file == null || file.entries == null) {
                return;
            }
            for (CachedProfile entry : file.entries) {
                if (entry == null) {
                    continue;
                }
                if (entry.uuid == null || entry.name == null) {
                    continue;
                }
                if (entry.lastUpdatedAt < cutoff) {
                    continue;
                }
                String normalized = normalizeUuid(entry.uuid);
                if (normalized == null || entry.name.isBlank()) {
                    continue;
                }
                CachedProfile profile = new CachedProfile(normalized, entry.name, entry.lastUpdatedAt);
                UUID_TO_PROFILE.put(normalized, profile);
                NAME_TO_UUID.put(entry.name.toLowerCase(Locale.ROOT), normalized);
            }
        } catch (Exception e) {
            LOGGER.warn("failed to read alliance profile cache", e);
        }
    }

    private static synchronized void evictIfNeeded() {
        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;

        // Remove entries not seen in 30 days
        UUID_TO_PROFILE.entrySet().removeIf(e -> {
            if (e.getValue().lastUpdatedAt < cutoff) {
                NAME_TO_UUID.values().remove(e.getKey());
                return true;
            }
            return false;
        });

        // If still over cap, drop the oldest entries
        int excess = UUID_TO_PROFILE.size() - MAX_ENTRIES;
        if (excess <= 0) {
            return;
        }
        LOGGER.warn("profile cache exceeds {} entries ({} over cap), evicting oldest", MAX_ENTRIES, excess);
        List<String> toEvict = UUID_TO_PROFILE.values().stream()
                .sorted((a, b) -> Long.compare(a.lastUpdatedAt, b.lastUpdatedAt))
                .limit(excess)
                .map(p -> p.uuid)
                .toList();
        for (String uuid : toEvict) {
            UUID_TO_PROFILE.remove(uuid);
            NAME_TO_UUID.values().remove(uuid);
        }
    }

    private static void persistSoon() {
        long now = System.currentTimeMillis();
        if (now - lastPersistedAt < 2_000L) {
            return;
        }
        lastPersistedAt = now;
        CompletableFuture.runAsync(AllianceProfileCacheManager::persistNow);
    }

    private static synchronized void persistNow() {
        evictIfNeeded();

        CachedProfileFile file = new CachedProfileFile();
        file.entries = new ArrayList<>(UUID_TO_PROFILE.values());

        StringWriter buffer = new StringWriter();
        try (Writer writer = buffer) {
            GSON.toJson(file, writer);
        } catch (Exception e) {
            return;
        }
        PersistentDiskManager.writeAtomic(cacheFilePath(), buffer.toString());
    }

    private static Path cacheFilePath() {
        return PersistentDiskManager.resolveUserPath(CACHE_FILE);
    }

    public static String normalizeUuid(String uuidLike) {
        if (uuidLike == null || uuidLike.isBlank()) {
            return null;
        }
        String compact = uuidLike.trim().replace("-", "");
        if (compact.length() != 32) {
            return null;
        }
        try {
            UUID parsed = UUID.fromString(
                    compact.substring(0, 8) + "-" +
                            compact.substring(8, 12) + "-" +
                            compact.substring(12, 16) + "-" +
                            compact.substring(16, 20) + "-" +
                            compact.substring(20)
            );
            return parsed.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String displayNameForUuid(String uuid) {
        String normalized = normalizeUuid(uuid);
        if (normalized == null) {
            return uuid;
        }
        observeWorldPlayers();
        String cached = getCachedNameByUuid(normalized);
        if (cached != null) {
            return cached;
        }
        queueNameLookupForUuid(normalized);
        return normalized.substring(0, 8);
    }

    public static Set<String> collectUnknownUuids(Collection<String> uuids) {
        HashSet<String> out = new HashSet<>();
        for (String uuid : uuids) {
            String normalized = normalizeUuid(uuid);
            if (normalized == null) {
                continue;
            }
            if (!UUID_TO_PROFILE.containsKey(normalized)) {
                out.add(normalized);
            }
        }
        return out;
    }

    private static class CachedProfileFile {
        List<CachedProfile> entries = new ArrayList<>();
    }

    public static class CachedProfile {
        public String uuid;
        public String name;
        public long lastUpdatedAt;

        public CachedProfile() {
        }

        public CachedProfile(String uuid, String name, long lastUpdatedAt) {
            this.uuid = uuid;
            this.name = name;
            this.lastUpdatedAt = lastUpdatedAt;
        }
    }
}
