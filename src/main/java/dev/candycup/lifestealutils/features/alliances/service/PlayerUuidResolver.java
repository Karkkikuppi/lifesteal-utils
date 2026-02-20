package dev.candycup.lifestealutils.features.alliances.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PlayerUuidResolver {
   private static final String GEYSER_API_URL = "https://api.geysermc.org/v2/utils/uuid/bedrock_or_java/";
   private static final long ONLINE_CACHE_TTL_MS = 500;
   private static final long API_REQUEST_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(5);
   private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

   private static Map<String, UUID> onlineNameCache = new HashMap<>();
   private static long onlineNameCacheExpiresAt = 0;

   private static final Map<String, Long> lastApiRequestTime = new ConcurrentHashMap<>();

   private PlayerUuidResolver() {
   }

   public record ResolveResult(UUID uuid, String username, boolean fromCache) {
      public boolean isSuccess() {
         return uuid != null;
      }
   }

   public static UUID resolveUuid(String usernameOrUuid) {
      if (usernameOrUuid == null || usernameOrUuid.isBlank()) return null;

      try {
         return UUID.fromString(usernameOrUuid.trim());
      } catch (IllegalArgumentException ignored) {
      }

      String username = usernameOrUuid.trim();

      UUID onlineUuid = resolveOnlineUuid(username);
      if (onlineUuid != null) {
         updateCache(onlineUuid, username);
         return onlineUuid;
      }

      UUID cachedUuid = getCachedUuid(username);
      if (cachedUuid != null) {
         return cachedUuid;
      }

      ResolveResult result = resolveFromApiBlocking(username);
      if (result != null && result.isSuccess()) {
         return result.uuid();
      }

      return null;
   }

   public static void resolveUuidAsync(String usernameOrUuid, Consumer<UUID> callback) {
      if (usernameOrUuid == null || usernameOrUuid.isBlank()) {
         callback.accept(null);
         return;
      }

      try {
         UUID uuid = UUID.fromString(usernameOrUuid.trim());
         callback.accept(uuid);
         return;
      } catch (IllegalArgumentException ignored) {
      }

      String username = usernameOrUuid.trim();

      UUID onlineUuid = resolveOnlineUuid(username);
      if (onlineUuid != null) {
         updateCache(onlineUuid, username);
         callback.accept(onlineUuid);
         return;
      }

      UUID cachedUuid = getCachedUuid(username);
      if (cachedUuid != null) {
         callback.accept(cachedUuid);
         return;
      }

      CompletableFuture.supplyAsync(() -> {
         ResolveResult result = resolveFromApiBlocking(username);
         return result != null && result.isSuccess() ? result.uuid() : null;
      }).thenAccept(uuid -> {
         Minecraft.getInstance().execute(() -> callback.accept(uuid));
      });
   }

   public static UUID resolveOnlineUuidCached(String username) {
      if (username == null || username.isBlank()) return null;
      long now = System.currentTimeMillis();
      if (now >= onlineNameCacheExpiresAt) {
         rebuildOnlineNameCache(now);
      }
      return onlineNameCache.get(username.toLowerCase(Locale.ROOT));
   }

   public static String getCachedUsername(UUID uuid) {
      if (uuid == null) return null;
      return Config.getUuidUsernameCache().get(uuid.toString());
   }

   public static String getCachedUsername(String uuidString) {
      if (uuidString == null || uuidString.isBlank()) return null;
      return Config.getUuidUsernameCache().get(uuidString);
   }

   public static void updateCache(UUID uuid, String username) {
      if (uuid == null || username == null || username.isBlank()) return;
      Map<String, String> cache = new HashMap<>(Config.getUuidUsernameCache());
      cache.put(uuid.toString(), username);
      Config.setUuidUsernameCache(cache);
   }

   private static UUID getCachedUuid(String username) {
      if (username == null || username.isBlank()) return null;
      String lowerUsername = username.toLowerCase(Locale.ROOT);
      Map<String, String> cache = Config.getUuidUsernameCache();
      for (Map.Entry<String, String> entry : cache.entrySet()) {
         if (entry.getValue() != null && entry.getValue().toLowerCase(Locale.ROOT).equals(lowerUsername)) {
            try {
               return UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException ignored) {
            }
         }
      }
      return null;
   }

   private static ResolveResult resolveFromApiBlocking(String username) {
      if (username == null || username.isBlank()) return null;

      String lowerUsername = username.toLowerCase(Locale.ROOT);

      Long lastRequest = lastApiRequestTime.get(lowerUsername);
      if (lastRequest != null && System.currentTimeMillis() - lastRequest < API_REQUEST_COOLDOWN_MS) {
         return null;
      }

      lastApiRequestTime.put(lowerUsername, System.currentTimeMillis());

      ResolveResult result = fetchFromApi(username);
      if (result != null && result.isSuccess()) {
         updateCache(result.uuid(), result.username());
      }
      return result;
   }

   private static ResolveResult fetchFromApi(String username) {
      if (username == null || username.isBlank()) return null;

      try {
         String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
         String url = GEYSER_API_URL + encodedUsername + "?prefix=.";

         NetworkUtilsController.HttpResult result = NetworkUtilsController.get(url, API_TIMEOUT, API_REQUEST_COOLDOWN_MS);
         if (!result.success() || result.body() == null) {
            return null;
         }

         JsonObject json = JsonParser.parseString(result.body()).getAsJsonObject();
         String id = json.has("id") ? json.get("id").getAsString() : null;
         String name = json.has("name") ? json.get("name").getAsString() : null;

         if (id == null || id.isBlank()) return null;

         UUID uuid = parseUuidWithoutDashes(id);
         if (uuid == null) return null;

         return new ResolveResult(uuid, name != null ? name : username, false);
      } catch (Exception ignored) {
         return null;
      }
   }

   private static UUID parseUuidWithoutDashes(String id) {
      if (id == null) return null;

      String clean = id.replace("-", "");
      if (clean.length() != 32) return null;

      try {
         String withDashes = clean.substring(0, 8) + "-" +
                 clean.substring(8, 12) + "-" +
                 clean.substring(12, 16) + "-" +
                 clean.substring(16, 20) + "-" +
                 clean.substring(20, 32);
         return UUID.fromString(withDashes);
      } catch (Exception ignored) {
         return null;
      }
   }

   private static UUID resolveOnlineUuid(String username) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection == null) return null;
      String target = username.toLowerCase(Locale.ROOT);
      for (PlayerInfo info : connection.getOnlinePlayers()) {
         if (info == null || info.getProfile() == null) continue;
         String name = getProfileName(info);
         if (name != null && name.toLowerCase(Locale.ROOT).equals(target)) {
            return getProfileId(info);
         }
      }
      return null;
   }

   private static void rebuildOnlineNameCache(long now) {
      Map<String, UUID> next = new HashMap<>();
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection != null) {
         for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (info == null || info.getProfile() == null) continue;
            String name = getProfileName(info);
            UUID id = getProfileId(info);
            if (name != null && id != null) {
               next.put(name.toLowerCase(Locale.ROOT), id);
            }
         }
      }
      onlineNameCache = next;
      onlineNameCacheExpiresAt = now + ONLINE_CACHE_TTL_MS;
   }

   private static String getProfileName(PlayerInfo info) {
      Object profile = info.getProfile();
      if (profile == null) return null;
      try {
         Method method = profile.getClass().getMethod("getName");
         Object value = method.invoke(profile);
         return value != null ? value.toString() : null;
      } catch (Exception ignored) {
      }
      try {
         Method method = profile.getClass().getMethod("name");
         Object value = method.invoke(profile);
         return value != null ? value.toString() : null;
      } catch (Exception ignored) {
      }
      return null;
   }

   private static UUID getProfileId(PlayerInfo info) {
      Object profile = info.getProfile();
      if (profile == null) return null;
      try {
         Method method = profile.getClass().getMethod("getId");
         Object value = method.invoke(profile);
         return value instanceof UUID ? (UUID) value : null;
      } catch (Exception ignored) {
      }
      try {
         Method method = profile.getClass().getMethod("id");
         Object value = method.invoke(profile);
         return value instanceof UUID ? (UUID) value : null;
      } catch (Exception ignored) {
      }
      return null;
   }
}
