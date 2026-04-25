package dev.candycup.lifestealutils.features.alliances;

import com.google.gson.JsonObject;
import dev.candycup.lifestealutils.Config;
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
      if (!Config.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }
      CompletableFuture.runAsync(AllianceSyncManager::syncSubscriptionsNow);
   }

   public static void syncSubscriptionsNow() {
      if (!Config.isGaiaAdvancedFeaturesEnabled() || !LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      // null = API call failed (network/auth/disabled); don't touch local state
      // empty list = server responded with no subscriptions; safe to remove stale local entries
      List<AllianceModels.AllianceRecord> remote = GaiaApiClient.getInstance().alliances().listSubscriptions();
      if (remote == null) {
         LOGGER.debug("syncSubscriptionsNow: skipping — API call failed");
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

   public static void publishOrUpdateAsync(AllianceModels.AllianceRecord alliance) {
      if (alliance == null || !alliance.canEdit || !Config.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }
      CompletableFuture.runAsync(() -> publishOrUpdateNow(alliance));
   }

   public static void publishOrUpdateNow(AllianceModels.AllianceRecord alliance) {
      if (alliance == null || !alliance.canEdit || !Config.isGaiaAdvancedFeaturesEnabled()) {
         return;
      }
      if (!alliance.published || alliance.serverId == null || alliance.serverId.isBlank()) {
         AllianceModels.AllianceRecord created = GaiaApiClient.getInstance().alliances().createAlliance(alliance);
         if (created == null || created.serverId == null || created.serverId.isBlank()) {
            LOGGER.debug("failed to publish alliance {}", alliance.clientId);
            return;
         }
         alliance.serverId = created.serverId;
         alliance.published = true;
         alliance.source = "remote";
      }

      boolean ok = GaiaApiClient.getInstance().alliances().replaceAllianceData(alliance);
      if (ok) {
         alliance.lastSyncedAt = System.currentTimeMillis();
         alliance.syncState = "SYNCED";
      } else {
         alliance.syncState = "ERROR";
      }
      AllianceService.save(alliance);
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

      AllianceService.save(remoteAlliance);
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
