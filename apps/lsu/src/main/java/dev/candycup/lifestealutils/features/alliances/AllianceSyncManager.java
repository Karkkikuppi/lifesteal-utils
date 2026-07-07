package dev.candycup.lifestealutils.features.alliances;


import dev.candycup.lifestealutils.gaia.GaiaConsentController;
import com.google.gson.JsonObject;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.gaia.GaiaApiClient;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AllianceSyncManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/alliances/sync");

   private AllianceSyncManager() {
   }

   public static void syncSubscriptionsAsync() {
      if (!GaiaConsentController.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }
      CompletableFuture.runAsync(AllianceSyncManager::syncSubscriptionsNow);
   }

   /**
    * Runs a full alliance sync pass after gateway ready:
    * 1) publish local alliances to Gaia as backup
    * 2) refresh remote subscriptions into client state
    */
   public static void syncAllOnGatewayConnectedAsync() {
      if (!GaiaConsentController.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }
      CompletableFuture.runAsync(() -> {
         backupLocalAlliancesNow();
         syncSubscriptionsNow();
      });
   }

   public static void syncSubscriptionsNow() {
      if (!GaiaConsentController.isGaiaAdvancedFeaturesEnabled() || !LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      // null = API call failed (network/auth/disabled); don't touch local state
      // empty list = server responded with no subscriptions; safe to remove stale local entries
      List<AllianceModels.AllianceRecord> remote = GaiaApiClient.getInstance().alliances().listSubscriptions();
      if (remote == null) {
         LOGGER.debug("syncSubscriptionsNow: skipping - API call failed");
         return;
      }

      Set<String> seenServerIds = new HashSet<>();
      for (AllianceModels.AllianceRecord remoteAlliance : remote) {
         if (remoteAlliance.serverId == null || remoteAlliance.serverId.isBlank()) {
            continue;
         }
         seenServerIds.add(remoteAlliance.serverId);
         upsertRemote(remoteAlliance);
      }

      List<AllianceModels.AllianceRecord> current = AllianceService.listAll();
      for (AllianceModels.AllianceRecord local : current) {
         if (!"remote".equalsIgnoreCase(local.source)) {
            continue;
         }
         if (local.serverId == null || local.serverId.isBlank()) {
            continue;
         }
         if (!seenServerIds.contains(local.serverId)) {
            AllianceService.delete(local.clientId);
         }
      }
   }

   public static void backupLocalAlliancesNow() {
      if (!GaiaConsentController.isGaiaAdvancedFeaturesEnabled() || !LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      List<AllianceModels.AllianceRecord> snapshot = AllianceService.listAll();
      int attempted = 0;
      int synced = 0;

      for (AllianceModels.AllianceRecord alliance : snapshot) {
         if (alliance == null) {
            continue;
         }
         if ("remote".equalsIgnoreCase(alliance.source)) {
            continue;
         }

         attempted++;
         if (publishOrUpdateNow(alliance)) {
            synced++;
         }
      }

      if (attempted > 0) {
         LOGGER.info("Gaia backup pass complete: synced {}/{} local alliances", synced, attempted);
      }
   }

   public static void publishOrUpdateAsync(AllianceModels.AllianceRecord alliance) {
      if (alliance == null || !alliance.canEdit || !GaiaConsentController.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }
      CompletableFuture.runAsync(() -> publishOrUpdateNow(alliance));
   }

   public static boolean publishOrUpdateNow(AllianceModels.AllianceRecord alliance) {
      if (alliance == null || !alliance.canEdit || !GaiaConsentController.isGaiaAdvancedFeaturesEnabled()) {
         return false;
      }

      boolean wasLocal = !"remote".equalsIgnoreCase(alliance.source) && !alliance.published;
      boolean hasServerId = alliance.serverId != null && !alliance.serverId.isBlank();

      if (!hasServerId) {
         AllianceModels.AllianceRecord created = GaiaApiClient.getInstance().alliances().createAlliance(alliance);
         if (created == null || created.serverId == null || created.serverId.isBlank()) {
            LOGGER.debug("failed to publish alliance {}", alliance.clientId);
            alliance.syncState = "ERROR";
            if (wasLocal) {
               alliance.source = "local";
               alliance.published = false;
            }
            AllianceService.save(alliance);
            return false;
         }
         alliance.serverId = created.serverId;
      }

      boolean ok = GaiaApiClient.getInstance().alliances().replaceAllianceData(alliance);
      if (ok) {
         alliance.published = true;
         alliance.source = "remote";
         alliance.lastSyncedAt = System.currentTimeMillis();
         alliance.syncState = "SYNCED";
      } else {
         alliance.syncState = "ERROR";
         if (wasLocal) {
            alliance.source = "local";
            alliance.published = false;
         }
      }
      AllianceService.save(alliance);
      return ok;
   }

   public static void applyGatewayUpdate(JsonObject data) {
      if (data == null) {
         return;
      }
      String eventType = data.has("eventType") ? data.get("eventType").getAsString() : "";
      String serverId = data.has("allianceId") ? data.get("allianceId").getAsString() : "";
      if (serverId.isBlank() && data.has("id")) {
         serverId = data.get("id").getAsString();
      }
      if (serverId.isBlank()) {
         return;
      }

      final String id = serverId;
      CompletableFuture.runAsync(() -> {
         AllianceModels.AllianceRecord fetched = GaiaApiClient.getInstance().alliances().fetchById(id);
         if (fetched == null) {
            return;
         }
         upsertRemote(fetched);
      });
   }

   private static void upsertRemote(AllianceModels.AllianceRecord remoteAlliance) {
      AllianceModels.AllianceRecord existing = AllianceService.findByServerId(remoteAlliance.serverId);
      if (existing != null) {
         remoteAlliance.clientId = existing.clientId;
         remoteAlliance.order = existing.order;
      } else {
         remoteAlliance.clientId = AllianceIdGenerator.newClientId();
         remoteAlliance.order = AllianceService.listAll().size();
      }
      remoteAlliance.published = true;
      remoteAlliance.source = "remote";
      remoteAlliance.syncState = "SYNCED";
      remoteAlliance.lastSyncedAt = System.currentTimeMillis();
      remoteAlliance.canEdit = isCurrentUserOwner(remoteAlliance.ownerUuid);

      if (remoteAlliance.data != null && remoteAlliance.data.lists != null && !remoteAlliance.data.lists.isEmpty()) {
         if (remoteAlliance.lastUsedListId == null || remoteAlliance.lastUsedListId.isBlank()) {
            remoteAlliance.lastUsedListId = remoteAlliance.data.lists.get(0).id;
         }
      }

      AllianceService.upsertRemote(remoteAlliance);
   }

   private static boolean isCurrentUserOwner(String ownerUuid) {
      if (ownerUuid == null || ownerUuid.isBlank()) {
         return false;
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         return false;
      }
      return ownerUuid.replace("-", "").equalsIgnoreCase(minecraft.player.getUUID().toString().replace("-", ""));
   }
}


