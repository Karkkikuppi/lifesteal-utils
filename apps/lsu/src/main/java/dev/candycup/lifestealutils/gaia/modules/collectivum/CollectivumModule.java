package dev.candycup.lifestealutils.gaia.modules.collectivum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import dev.candycup.lifestealutils.gaia.collectivum.CollectivumBaltopClient;
import dev.candycup.lifestealutils.interapi.NetworkUtilsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CollectivumModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/collectivum-baltop");
   private static final Gson GSON = new GsonBuilder().create();
   private static final Duration API_TIMEOUT = Duration.ofSeconds(10);
   private static final String COLLECTIVUM_BASE_PATH = "/v1/collectivum";
   private static final String BALTOP_PATH = "/baltop";
   private static final int NO_CONTENT_STATUS = 204;

   private final GaiaApiClient apiClient;

   public CollectivumModule(GaiaApiClient apiClient) {
      this.apiClient = apiClient;
   }

   public CompletableFuture<Boolean> submitBaltopEntries(List<CollectivumBaltopClient.BaltopEntryPayload> entries) {
      return CompletableFuture.supplyAsync(() -> {
         if (entries == null || entries.isEmpty()) {
            LOGGER.debug("no baltop entries to submit");
            return false;
         }

         JsonArray payload = new JsonArray();
         for (CollectivumBaltopClient.BaltopEntryPayload entry : entries) {
            JsonObject entryObject = new JsonObject();
            entryObject.addProperty("username", entry.username());
            entryObject.addProperty("amount", entry.amount());
            payload.add(entryObject);
         }

         NetworkUtilsController.HttpResult result = apiClient.postJsonWithAuth(
                 COLLECTIVUM_BASE_PATH + BALTOP_PATH,
                 GSON.toJson(payload),
                 API_TIMEOUT,
                 0
         );

         if (!result.success()) {
            LOGGER.warn("failed to submit baltop entries: {}", result.error());
            return false;
         }

         return parseSuccessResponse(result);
      });
   }

   private static boolean parseSuccessResponse(NetworkUtilsController.HttpResult result) {
      if (result.statusCode() == NO_CONTENT_STATUS) {
         return true;
      }

      String body = result.body();
      if (body == null || body.isBlank()) {
         return false;
      }

      try {
         JsonObject root = GSON.fromJson(body, JsonObject.class);
         if (root == null) {
            return false;
         }
         if (!root.has("success")) {
            return false;
         }
         return root.get("success").getAsBoolean();
      } catch (Exception e) {
         LOGGER.warn("failed to parse collectivum response: {}", e.getMessage());
         return false;
      }
   }
}