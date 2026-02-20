package dev.candycup.lifestealutils.gaia;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * handles storage and validation of Gaia authentication tokens.
 */
public final class GaiaAuthTokenStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/gaia-auth");
   private static final String GAIA_ROOT_DIRECTORY = "lifestealutils";
   private static final String GAIA_DIRECTORY_NAME = "gaia";
   private static final String AUTH_DIRECTORY_NAME = "authentication";
   private static final String TOKEN_SUFFIX = "-gaia-token-do-not-share";
   private static final int JWT_PART_COUNT = 3;
   private static final String JWT_EXP_FIELD = "exp";
   private static final String JWT_NAME_FIELD = "name";
   private static final String JWT_UUID_FIELD = "uuid";

   private GaiaAuthTokenStore() {
   }

   /**
    * returns whether the cached token is valid for the specified user.
    *
    * @param username the Minecraft username
    * @param playerId the player uuid
    * @return true when a cached token exists and is not expired
    */
   public static boolean hasValidToken(String username, UUID playerId) {
      String token = readToken(username);
      if (token == null || token.isBlank()) {
         return false;
      }

      JwtPayload payload = parsePayload(token);
      if (payload == null) {
         return false;
      }

      if (!payload.isValidForUser(username, playerId)) {
         return false;
      }

      return !payload.isExpired();
   }

   /**
    * saves the Gaia token for the given user.
    *
    * @param username the Minecraft username
    * @param token    the JWT token issued by Gaia
    */
   public static void saveToken(String username, String token) {
      Path path = getTokenPath(username);
      try {
         Files.createDirectories(path.getParent());
         Files.writeString(path, token, StandardCharsets.UTF_8);
      } catch (Exception e) {
         LOGGER.debug("failed to save gaia token: {}", e.getMessage());
      }
   }

   /**
    * reads the cached token for the specified user.
    *
    * @param username the Minecraft username
    * @return the token string or null if not found
    */
   public static String readToken(String username) {
      Path path = getTokenPath(username);
      if (!Files.exists(path)) {
         return null;
      }
      try {
         return Files.readString(path, StandardCharsets.UTF_8).trim();
      } catch (Exception e) {
         LOGGER.debug("failed to read gaia token: {}", e.getMessage());
         return null;
      }
   }

   private static Path getTokenPath(String username) {
      return FabricLoader.getInstance().getGameDir()
              .resolve(GAIA_ROOT_DIRECTORY)
              .resolve(GAIA_DIRECTORY_NAME)
              .resolve(AUTH_DIRECTORY_NAME)
              .resolve(username + TOKEN_SUFFIX);
   }

   private static JwtPayload parsePayload(String token) {
      String[] parts = token.split("\\.");
      if (parts.length != JWT_PART_COUNT) {
         return null;
      }

      try {
         byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
         String json = new String(decoded, StandardCharsets.UTF_8);
         JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
         if (payload == null) {
            return null;
         }

         long exp = payload.has(JWT_EXP_FIELD) ? payload.get(JWT_EXP_FIELD).getAsLong() : 0;
         String name = payload.has(JWT_NAME_FIELD) ? payload.get(JWT_NAME_FIELD).getAsString() : null;
         String uuid = payload.has(JWT_UUID_FIELD) ? payload.get(JWT_UUID_FIELD).getAsString() : null;

         return new JwtPayload(exp, name, uuid);
      } catch (Exception e) {
         LOGGER.debug("failed to parse gaia token payload: {}", e.getMessage());
         return null;
      }
   }

   private record JwtPayload(long exp, String name, String uuid) {
      private boolean isExpired() {
         long now = Instant.now().getEpochSecond();
         return exp <= 0 || now >= exp;
      }

      private boolean isValidForUser(String username, UUID playerId) {
         if (name == null || uuid == null) {
            return false;
         }
         boolean nameMatches = name.equalsIgnoreCase(username);
         boolean uuidMatches = uuid.equalsIgnoreCase(playerId.toString());
         return nameMatches && uuidMatches;
      }
   }
}
