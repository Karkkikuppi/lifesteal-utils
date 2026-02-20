package dev.candycup.lifestealutils.gaia.modules.imperium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.candycup.lifestealutils.features.alliances.models.Alliance;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMember;
import dev.candycup.lifestealutils.features.alliances.models.AllianceMembershipState;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AlliancesModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliances-api");
   private static final Gson GSON = new GsonBuilder().create();
   private static final Duration API_TIMEOUT = Duration.ofSeconds(10);
   private static final String ALLIANCES_BASE_PATH = "/v1/imperium/alliances";

   private final GaiaApiClient apiClient;

   public AlliancesModule(GaiaApiClient apiClient) {
      this.apiClient = apiClient;
   }

   public CompletableFuture<List<Alliance>> fetchPlayerAlliances() {
      return CompletableFuture.supplyAsync(() -> {
         NetworkUtilsController.HttpResult result = apiClient.getWithAuth(ALLIANCES_BASE_PATH + "/@self", API_TIMEOUT, 0);
         if (!result.success()) {
            LOGGER.warn("Failed to fetch player alliances: {}", result.error());
            return new ArrayList<>();
         }

         LOGGER.info("Received response from alliances API: {}", result.body());
         return parseAlliancesList(result.body());
      });
   }

   public CompletableFuture<List<Alliance>> fetchPlayerInvites() {
      return CompletableFuture.supplyAsync(() -> {
         NetworkUtilsController.HttpResult result = apiClient.getWithAuth(ALLIANCES_BASE_PATH + "/@self/invites", API_TIMEOUT, 0);
         if (!result.success()) {
            LOGGER.warn("Failed to fetch player alliance invites: {}", result.error());
            return new ArrayList<>();
         }

         return parseAlliancesList(result.body());
      });
   }

   public CompletableFuture<Alliance> fetchAlliance(String allianceId) {
      return CompletableFuture.supplyAsync(() -> {
         NetworkUtilsController.HttpResult result = apiClient.getWithAuth(ALLIANCES_BASE_PATH + "/" + allianceId, API_TIMEOUT, 0);
         if (!result.success()) {
            LOGGER.debug("failed to fetch alliance {}: {}", allianceId, result.error());
            return null;
         }

         return parseAllianceResponse(result.body());
      });
   }

   public CompletableFuture<Alliance> createAlliance(String name, String prefix, String color, String description, String motd) {
      return CompletableFuture.supplyAsync(() -> {
         JsonObject payload = new JsonObject();
         payload.addProperty("name", name);
         if (prefix != null && !prefix.trim().isEmpty()) {
            payload.addProperty("prefix", prefix.trim());
         }
         if (color != null && !color.trim().isEmpty()) {
            payload.addProperty("color", color.trim());
         }
         payload.addProperty("description", description);
         payload.addProperty("motd", motd);

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/create",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to create alliance: {}", result.error());
            return null;
         }

         return parseAllianceResponse(result.body());
      });
   }

   public CompletableFuture<Alliance> updateAlliance(String allianceId, String name, String prefix, String color, String description, String motd) {
      return CompletableFuture.supplyAsync(() -> {
         if (name == null && prefix == null && color == null && description == null && motd == null) {
            LOGGER.debug("no alliance update fields provided");
            return null;
         }

         JsonObject payload = new JsonObject();
         payload.addProperty("allianceId", allianceId);
         if (name != null) {
            payload.addProperty("name", name);
         }
         if (prefix != null) {
            payload.addProperty("prefix", prefix);
         }
         if (color != null) {
            payload.addProperty("color", color);
         }
         if (description != null) {
            payload.addProperty("description", description);
         }
         if (motd != null) {
            payload.addProperty("motd", motd);
         }

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/update",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to update alliance {}: {}", allianceId, result.error());
            return null;
         }

         return parseAllianceResponse(result.body());
      });
   }

   public CompletableFuture<Boolean> inviteMember(String allianceId, String uuid, String cachedName, List<String> permissions) {
      return CompletableFuture.supplyAsync(() -> {
         JsonObject payload = new JsonObject();
         payload.addProperty("allianceId", allianceId);
         payload.addProperty("uuid", uuid);
         payload.addProperty("cachedName", cachedName);
         JsonArray permsArray = new JsonArray();
         for (String perm : permissions) {
            permsArray.add(perm);
         }
         payload.add("permissions", permsArray);

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/invite-member",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to invite member: {}", result.error());
            return false;
         }

         return parseSuccessResponse(result.body());
      });
   }

   public CompletableFuture<Boolean> removeMember(String memberId) {
      return CompletableFuture.supplyAsync(() -> {
         JsonObject payload = new JsonObject();
         payload.addProperty("memberId", memberId);

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/remove-member",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to remove member: {}", result.error());
            return false;
         }

         return parseSuccessResponse(result.body());
      });
   }

   public CompletableFuture<Boolean> acceptInvitation(String allianceId) {
      return CompletableFuture.supplyAsync(() -> {
         JsonObject payload = new JsonObject();
         payload.addProperty("allianceId", allianceId);

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/@self/join",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to accept invitation: {}", result.error());
            return false;
         }

         return parseSuccessResponse(result.body());
      });
   }

   public CompletableFuture<Boolean> rejectInvitation(String allianceId) {
      return CompletableFuture.supplyAsync(() -> {
         JsonObject payload = new JsonObject();
         payload.addProperty("allianceId", allianceId);

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/@self/reject",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to reject invitation: {}", result.error());
            return false;
         }

         return parseSuccessResponse(result.body());
      });
   }

   public CompletableFuture<Boolean> deleteAlliance(String allianceId) {
      return CompletableFuture.supplyAsync(() -> {
         JsonObject payload = new JsonObject();
         payload.addProperty("allianceId", allianceId);

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 ALLIANCES_BASE_PATH + "/delete",
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );
         if (!result.success()) {
            LOGGER.debug("failed to delete alliance: {}", result.error());
            return false;
         }

         return parseSuccessResponse(result.body());
      });
   }

   private static List<Alliance> parseAlliancesList(String body) {
      try {
         JsonObject root = GSON.fromJson(body, JsonObject.class);
         if (root == null) {
            LOGGER.warn("Response body is null");
            return new ArrayList<>();
         }

         if (root.has("success") && !root.get("success").getAsBoolean()) {
            String error = root.has("error") ? root.get("error").getAsString() : "Unknown error";
            LOGGER.warn("API returned error: {}", error);
            return new ArrayList<>();
         }

         if (!root.has("alliances")) {
            LOGGER.warn("Response missing 'alliances' field");
            return new ArrayList<>();
         }

         JsonArray alliancesArray = root.getAsJsonArray("alliances");
         List<Alliance> alliances = new ArrayList<>();

         for (JsonElement element : alliancesArray) {
            Alliance alliance = parseAllianceObject(element.getAsJsonObject());
            if (alliance != null) {
               alliances.add(alliance);
            }
         }

         return alliances;
      } catch (Exception e) {
         LOGGER.warn("Failed to parse alliances list response", e);
         return new ArrayList<>();
      }
   }

   private static Alliance parseAllianceResponse(String body) {
      try {
         JsonObject root = GSON.fromJson(body, JsonObject.class);
         if (root == null) {
            return null;
         }

         if (root.has("success") && !root.get("success").getAsBoolean()) {
            String error = root.has("error") ? root.get("error").getAsString() : "Unknown error";
            LOGGER.warn("API returned error: {}", error);
            return null;
         }

         if (!root.has("alliance")) {
            return null;
         }

         return parseAllianceObject(root.getAsJsonObject("alliance"));
      } catch (Exception e) {
         LOGGER.debug("failed to parse alliance response: {}", e.getMessage());
         return null;
      }
   }

   private static Alliance parseAllianceObject(JsonObject obj) {
      try {
         String id = obj.get("id").getAsString();
         String name = obj.get("name").getAsString();
         String prefix = null;
         if (obj.has("prefix") && !obj.get("prefix").isJsonNull()) {
            prefix = obj.get("prefix").getAsString();
         }
         String color = null;
         if (obj.has("color") && !obj.get("color").isJsonNull()) {
            color = obj.get("color").getAsString();
         }
         String description = obj.get("description").getAsString();
         String motd = obj.get("motd").getAsString();
         String ownedBy = obj.has("ownedBy") ? obj.get("ownedBy").getAsString() : "";
         Instant createdAt = Instant.parse(obj.get("createdAt").getAsString());
         Instant updatedAt = Instant.parse(obj.get("updatedAt").getAsString());

         List<AllianceMember> members = new ArrayList<>();
         if (obj.has("members")) {
            JsonArray membersArray = obj.getAsJsonArray("members");
            for (JsonElement memberElement : membersArray) {
               AllianceMember member = parseMemberObject(memberElement.getAsJsonObject());
               if (member != null) {
                  members.add(member);
               }
            }
         }

         return new Alliance(id, name, prefix, color, description, motd, ownedBy, members, createdAt, updatedAt);
      } catch (Exception e) {
         LOGGER.debug("failed to parse alliance object: {}", e.getMessage());
         return null;
      }
   }

   private static AllianceMember parseMemberObject(JsonObject obj) {
      try {
         String id = obj.get("id").getAsString();
         String uuid = obj.get("uuid").getAsString();
         String cachedName = obj.get("cachedName").getAsString();
         String membershipStateStr = obj.get("membershipState").getAsString();
         AllianceMembershipState membershipState = AllianceMembershipState.valueOf(membershipStateStr);
         Instant addedAt = Instant.parse(obj.get("addedAt").getAsString());
         String addedBy = obj.get("addedBy").getAsString();
         String allianceId = obj.get("allianceId").getAsString();

         List<String> permissions = new ArrayList<>();
         if (obj.has("permissions")) {
            JsonArray permsArray = obj.getAsJsonArray("permissions");
            for (JsonElement perm : permsArray) {
               permissions.add(perm.getAsString());
            }
         }

         return new AllianceMember(id, uuid, cachedName, membershipState, addedAt, addedBy, permissions, allianceId);
      } catch (Exception e) {
         LOGGER.debug("failed to parse member object: {}", e.getMessage());
         return null;
      }
   }

   private static boolean parseSuccessResponse(String body) {
      try {
         JsonObject root = GSON.fromJson(body, JsonObject.class);
         return root != null && root.has("success") && root.get("success").getAsBoolean();
      } catch (Exception e) {
         LOGGER.debug("failed to parse success response: {}", e.getMessage());
         return false;
      }
   }
}