package dev.candycup.lifestealutils.features.qol;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.TablistDataController;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class PoiWaypointTracker {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/poi");

   public static final String CONFIG_ID = "poi_waypoint";
   public static final String DEFAULT_FORMAT = "<gray><bold>{{poi}}</bold>: {{distance}} blocks away";
   private static final float DEFAULT_TEXT_X = 0.023F;
   private static final float DEFAULT_TEXT_Y = 0.974F;
   private static final int UNKNOWN_DISTANCE = -1;
   private static final String INFINITY_SYMBOL = "∞";
   private static final String SHARD_KEYWORD_NETHER = "nether";
   private static final String SHARD_KEYWORD_HUB = "hub";
   private static final String SHARD_KEYWORD_SPAWN = "spawn";
   private static final String POI_DIMENSION_NETHER = "the_nether";

   private final List<PoiRepository.Poi> pois;
   private final HudElementDefinition hudDefinition;

   @Getter
   private PoiRepository.Poi currentTarget = null;

   public PoiWaypointTracker() {
      this.pois = PoiRepository.loadPois();

      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_text"),
              "POI Waypoint",
              this::getDisplayText,
              HudPosition.clamp(DEFAULT_TEXT_X, DEFAULT_TEXT_Y)
      );

      LifestealUtilsEvents.CLIENT_TICK.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onClientTick(event);
      });
      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });

      LOGGER.info("[lsu-poi] initialized with {} POIs", pois.size());
   }

   public HudElementDefinition getHudDefinition() {
      return hudDefinition;
   }

   public boolean isEnabled() {
      return Config.isPoiWaypointsEnabled();
   }

   public void onClientTick(ClientTickEvent event) {
      if (!isEnabled()) return;
      if (isIndicatorsSuppressedForShard()) {
         currentTarget = null;
         return;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) return;

      PoiRepository.Poi configuredTarget = resolveConfiguredTarget();
      if (configuredTarget != null) {
         currentTarget = configuredTarget;
         return;
      }

      if (!Config.isPoiAlwaysShowClosest()) {
         currentTarget = null;
         return;
      }

      this.currentTarget = findClosestVisiblePoi(client);
   }

   private PoiRepository.Poi resolveConfiguredTarget() {
      String configuredId = Config.getPoiTrackedId();
      if (configuredId == null || configuredId.isBlank()) {
         return null;
      }

      Optional<PoiRepository.Poi> match = pois.stream()
              .filter(poi -> poi.id().equals(configuredId))
              .findFirst();
      return match.orElse(null);
   }

   private PoiRepository.Poi findClosestVisiblePoi(Minecraft client) {
      String currentDimension = resolveCurrentDimension(client);
      double playerX = client.player.getX();
      double playerZ = client.player.getZ();

      PoiRepository.Poi best = null;
      double bestDistance = Double.MAX_VALUE;
      for (PoiRepository.Poi poi : pois) {
         if (!isPoiInCurrentDimension(poi, currentDimension)) {
            continue;
         }

         double distance = distanceToPoi(poi, playerX, playerZ);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = poi;
         }
      }
      return best;
   }

   private String resolveCurrentDimension(Minecraft client) {
      try {
         if (client.level == null || client.level.dimension() == null) {
            return null;
         }
         return client.level.dimension().toString();
      } catch (Exception ignore) {
         return null;
      }
   }

   private boolean isPoiInCurrentDimension(PoiRepository.Poi poi, String currentDimension) {
      if (poi.dimension() == null || currentDimension == null) {
         return true;
      }
      return poi.dimension().equals(currentDimension) || currentDimension.contains(poi.dimension());
   }

   private double distanceToPoi(PoiRepository.Poi poi, double playerX, double playerZ) {
      double deltaX = poi.x() - playerX;
      double deltaZ = poi.z() - playerZ;
      return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
   }

   public void onServerChange(ServerChangeEvent event) {
      // clear selection when server changes
      this.currentTarget = null;
   }

   private String getDisplayText() {
      if (!PoiDirectionalIndicator.isPoiHudTextEnabled()) {
         return "";
      }
      if (isIndicatorsSuppressedForShard()) {
         return "";
      }

      PoiRepository.Poi t = this.currentTarget;
      if (t == null) return "";

      Minecraft client = Minecraft.getInstance();
      if (client.player == null) return "";

      int distance = calculateDistance(t, client);

      String format = Config.getPoiWaypointFormat();
      if (format == null || format.isBlank()) format = DEFAULT_FORMAT;

      String distanceText = distance == UNKNOWN_DISTANCE
              ? INFINITY_SYMBOL
              : String.valueOf(distance);

      return format
              .replace("{{poi}}", t.name())
              .replace("{{distance}}", distanceText);
   }

   /**
    * Checks whether POI indicators should be hidden due to the current shard.
    *
    * @return true when on hub/spawn shards
    */
   public boolean isIndicatorsSuppressedForShard() {
      String shardName = LifestealAPI.getCurrentShard();
      if (shardName == null || shardName.isBlank()) {
         return false;
      }
      String shardLower = shardName.toLowerCase();
      return shardLower.contains(SHARD_KEYWORD_HUB) || shardLower.contains(SHARD_KEYWORD_SPAWN);
   }

   /**
    * Calculates the distance to a POI, accounting for cross-dimension tracking.
    *
    * @param target the target poi
    * @param client the minecraft client
    * @return the distance in blocks, or -1 when not in the same dimension
    */
   private int calculateDistance(PoiRepository.Poi target, Minecraft client) {
      if (!isSameDimension(target)) {
         return UNKNOWN_DISTANCE;
      }

      double dx = target.x() - client.player.getX();
      double dz = target.z() - client.player.getZ();
      return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
   }

   /**
    * Checks whether the player is in the same dimension as the poi.
    *
    * @param target the target poi
    * @return true when dimensions match or no dimension is set
    */
   private boolean isSameDimension(PoiRepository.Poi target) {
      if (target.dimension() == null || target.dimension().isBlank()) {
         return true;
      }

      boolean poiNether = target.dimension().contains(POI_DIMENSION_NETHER);
      boolean shardNether = isShardNether();
      return poiNether == shardNether;
   }

   /**
    * Checks if the current shard is a nether shard.
    *
    * @return true if shard name contains "nether"
    */
   private boolean isShardNether() {
      String shardName = LifestealAPI.getCurrentShard();
      if (shardName == null || shardName.isBlank()) {
         return false;
      }
      return shardName.toLowerCase().contains(SHARD_KEYWORD_NETHER);
   }

   /**
    * Exposes the display text for use by the directional indicator positioning.
    *
    * @return the current display text, or empty string if no target
    */
   public String getDisplayTextForIndicator() {
      return getDisplayText();
   }
}
