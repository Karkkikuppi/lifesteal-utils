package dev.candycup.lifestealutils.features.qol;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealTablistAPI;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.event.events.ServerChangeEvent;
import dev.candycup.lifestealutils.event.listener.ServerEventListener;
import dev.candycup.lifestealutils.event.listener.TickEventListener;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class PoiWaypointTracker implements TickEventListener, ServerEventListener {
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

   private PoiRepository.Poi currentTarget = null;

   public PoiWaypointTracker() {
      Config.ensurePoiWaypointFormat(DEFAULT_FORMAT);
      this.pois = PoiRepository.loadPois();

      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_text"),
              "POI Waypoint",
              this::getDisplayText,
              HudPosition.clamp(DEFAULT_TEXT_X, DEFAULT_TEXT_Y)
      );

      LOGGER.info("[lsu-poi] initialized with {} POIs", pois.size());
   }

   public HudElementDefinition getHudDefinition() {
      return hudDefinition;
   }

   @Override
   public boolean isEnabled() {
      return Config.getPoiWaypointsEnabled();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onClientTick(ClientTickEvent event) {
      if (!isEnabled()) return;
      if (isIndicatorsSuppressedForShard()) {
         currentTarget = null;
         return;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) return;

      // choose target: configured id > closest if allowed > none
      String configuredId = Config.getPoiTrackedId();
      if (configuredId != null && !configuredId.isBlank()) {
         Optional<PoiRepository.Poi> match = pois.stream()
                 .filter(p -> p.id().equals(configuredId))
                 .findFirst();
         currentTarget = match.orElse(null);
         return;
      }

      if (!Config.isPoiAlwaysShowClosest()) {
         // no configured target and not showing closest
         currentTarget = null;
         return;
      }

      // pick closest POI in the same dimension (or POIs without dimension set)
      String currentDimension = null;
      try {
         if (client.level != null && client.level.dimension() != null) {
            // registry key may not expose a direct location method across mappings; fall back to toString()
            // ^ this was causing some weird behavior in testing. TODO: dont make this hacky
            currentDimension = client.level.dimension().toString();
         }
      } catch (Exception ignore) {
      }

      double px = client.player.getX();
      double pz = client.player.getZ();

      PoiRepository.Poi best = null;
      double bestDist = Double.MAX_VALUE;
      for (PoiRepository.Poi poi : pois) {
         if (poi.dimension() != null && currentDimension != null) {
            // compare heuristically: either exact match or contained in registry key string
            if (!poi.dimension().equals(currentDimension) && !currentDimension.contains(poi.dimension())) {
               continue;
            }
         }

         double dx = poi.x() - px;
         double dz = poi.z() - pz;
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist < bestDist) {
            bestDist = dist;
            best = poi;
         }
      }

      this.currentTarget = best;
   }

   @Override
   public void onServerChange(ServerChangeEvent event) {
      // clear selection when server changes
      this.currentTarget = null;
   }

   private String getDisplayText() {
      if (!Config.isPoiHudTextEnabled()) {
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

      String format = Config.getPoiWaypointFormat(DEFAULT_FORMAT);
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
      String shardName = LifestealTablistAPI.getCurrentShard();
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
      String shardName = LifestealTablistAPI.getCurrentShard();
      if (shardName == null || shardName.isBlank()) {
         return false;
      }
      return shardName.toLowerCase().contains(SHARD_KEYWORD_NETHER);
   }

   /**
    * Helper: expose current target for tests or other code.
    */
   public PoiRepository.Poi getCurrentTarget() {
      return currentTarget;
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
