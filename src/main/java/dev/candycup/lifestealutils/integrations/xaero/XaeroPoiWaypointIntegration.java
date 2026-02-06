package dev.candycup.lifestealutils.integrations.xaero;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealTablistAPI;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.event.events.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.event.events.ServerChangeEvent;
import dev.candycup.lifestealutils.event.listener.ServerEventListener;
import dev.candycup.lifestealutils.event.listener.TickEventListener;
import dev.candycup.lifestealutils.features.qol.PoiRepository;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * syncs lifesteal utils poi definitions into xaero's minimap waypoint sets.
 */
public final class XaeroPoiWaypointIntegration implements TickEventListener, ServerEventListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/xaero");

   private static final String WAYPOINT_SET_TRANSLATION_KEY = "lsu.xaero.poi_set";
   private static final String WAYPOINT_INITIALS = "P";
   private static final String FALLBACK_POI_NAME = "POI";
   private static final int DEFAULT_COLOR_INDEX = 0;
   private static final int SYNC_DELAY_TICKS = 40;
   private static final String SHARD_KEYWORD_NETHER = "nether";
   private static final String SHARD_KEYWORD_HUB = "hub";
   private static final String SHARD_KEYWORD_SPAWN = "spawn";
   private static final String POI_DIMENSION_NETHER = "the_nether";

   private int pendingSyncTicks = -1;
   private int pendingRemoveTicks = -1;
   private String lastSyncedWorldKey = null;
   private String lastRemovedWorldKey = null;
   private Boolean lastEnabledState = null;
   private Set<PoiCoords> cachedPoiCoords = new HashSet<>();

   /**
    * checks whether xaero poi syncing should run.
    *
    * @return true when the feature is enabled
    */
   public XaeroPoiWaypointIntegration() {
      scheduleSync();
   }

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onServerChange(ServerChangeEvent event) {
      scheduleReconcile();
   }

   @Override
   public void onShardSwap(LifestealShardSwapEvent event) {
      scheduleReconcile();
   }

   @Override
   public void onClientTick(ClientTickEvent event) {
      boolean integrationEnabled = Config.isXaeroPoiWaypointsEnabled();
      if (lastEnabledState == null || lastEnabledState != integrationEnabled) {
         if (integrationEnabled) {
            scheduleSync();
         } else {
            scheduleRemoval();
         }
         lastEnabledState = integrationEnabled;
      }

      if (!integrationEnabled) {
         handleRemovalTick();
         return;
      }

      if (pendingSyncTicks == SYNC_DELAY_TICKS) {
         LOGGER.info("[lsu-xaero] scheduled poi sync");
      }
      if (pendingSyncTicks < 0) {
         return;
      }
      if (pendingSyncTicks > 0) {
         pendingSyncTicks--;
         return;
      }
      if (isIndicatorsSuppressedForShard()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         return;
      }

      MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
      if (session == null) {
         LOGGER.debug("[lsu-xaero] xaero minimap session not ready yet");
         return;
      }

      MinimapWorld world = session.getWorldManager().getCurrentWorld();
      if (world == null) {
         LOGGER.debug("[lsu-xaero] xaero world not ready yet");
         return;
      }

      String worldKey = world.getFullPath().toString();
      if (worldKey.equals(lastSyncedWorldKey)) {
         LOGGER.debug("[lsu-xaero] poi sync already completed for {}", worldKey);
         pendingSyncTicks = -1;
         return;
      }

      boolean changed = syncPoisToWorld(session, world);
      if (changed) {
         try {
            session.getWorldManagerIO().saveWorld(world);
            LOGGER.info("[lsu-xaero] saved poi waypoints to xaero world {}", worldKey);
         } catch (IOException e) {
            LOGGER.warn("failed to save xaero waypoint data", e);
         }
      } else {
         LOGGER.info("[lsu-xaero] no poi changes needed for xaero world {}", worldKey);
      }

      lastSyncedWorldKey = worldKey;
      pendingSyncTicks = -1;
   }

   /**
    * schedules a waypoint sync on the next stable tick.
    */
   private void scheduleSync() {
      pendingSyncTicks = SYNC_DELAY_TICKS;
      lastSyncedWorldKey = null;
   }

   /**
    * schedules a waypoint removal on the next stable tick.
    */
   private void scheduleRemoval() {
      pendingRemoveTicks = SYNC_DELAY_TICKS;
      lastRemovedWorldKey = null;
   }

   /**
    * schedules a sync or removal based on the current config.
    */
   private void scheduleReconcile() {
      if (Config.isXaeroPoiWaypointsEnabled()) {
         scheduleSync();
      } else {
         scheduleRemoval();
      }
   }

   /**
    * processes removal ticks when the integration is disabled.
    */
   private void handleRemovalTick() {
      if (pendingRemoveTicks < 0) {
         return;
      }
      if (pendingRemoveTicks > 0) {
         pendingRemoveTicks--;
         return;
      }

      MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
      if (session == null) {
         LOGGER.debug("[lsu-xaero] xaero minimap session not ready for removal");
         return;
      }

      MinimapWorld world = session.getWorldManager().getCurrentWorld();
      if (world == null) {
         LOGGER.debug("[lsu-xaero] xaero world not ready for removal");
         return;
      }

      String worldKey = world.getFullPath().toString();
      if (worldKey.equals(lastRemovedWorldKey)) {
         pendingRemoveTicks = -1;
         return;
      }

      boolean changed = removePoisFromWorld(world);
      if (changed) {
         try {
            session.getWorldManagerIO().saveWorld(world);
            LOGGER.info("[lsu-xaero] removed poi waypoints from xaero world {}", worldKey);
         } catch (IOException e) {
            LOGGER.warn("failed to save xaero waypoint removals", e);
         }
      }

      lastRemovedWorldKey = worldKey;
      pendingRemoveTicks = -1;
   }

   /**
    * applies poi waypoints to the current xaero world.
    *
    * @param session the xaero minimap session
    * @param world   the current xaero world
    * @return true if waypoints were added or sets created
    */
   private boolean syncPoisToWorld(MinimapSession session, MinimapWorld world) {
      List<PoiRepository.Poi> pois = PoiRepository.loadPoisIncludingDisabled();
      if (pois.isEmpty()) {
         LOGGER.info("[lsu-xaero] no pois available to sync");
         return false;
      }

      cachedPoiCoords = collectPoiCoords(pois, false);

      WaypointSet targetSet = world.getCurrentWaypointSet();
      boolean changed = false;
      if (targetSet == null) {
         LOGGER.info("[lsu-xaero] current waypoint set missing, creating lsu set");
         targetSet = WaypointSet.Builder.begin().setName(WAYPOINT_SET_TRANSLATION_KEY).build();
         world.addWaypointSet(targetSet);
         world.setCurrentWaypointSetId(WAYPOINT_SET_TRANSLATION_KEY);
         changed = true;
      }

      LOGGER.info("[lsu-xaero] syncing {} pois into xaero set {}", pois.size(), targetSet.getName());

      boolean removedDisabled = removeDisabledPoisFromWorld(world, pois);
      if (removedDisabled) {
         changed = true;
      }

      for (PoiRepository.Poi poi : pois) {
         if (poi == null) {
            continue;
         }
         if (poi.disabled()) {
            continue;
         }
         if (!isPoiVisibleOnCurrentShard(poi)) {
            LOGGER.debug("[lsu-xaero] skipping poi {} due to shard mismatch", poi.id());
            continue;
         }

         int x = (int) Math.round(poi.x());
         int y = (int) Math.round(poi.y());
         int z = (int) Math.round(poi.z());

         if (hasWaypoint(targetSet, x, y, z)) {
            continue;
         }

         String name = resolvePoiName(poi);
         WaypointColor color = WaypointColor.fromIndex(DEFAULT_COLOR_INDEX);
         Waypoint waypoint = new Waypoint(x, y, z, name, WAYPOINT_INITIALS, color, WaypointPurpose.NORMAL, false, true);
         targetSet.add(waypoint);
         changed = true;
      }

      return changed;
   }

   /**
    * checks if a waypoint already exists in a set.
    *
    * @param set the waypoint set
    * @param x   the x coordinate
    * @param y   the y coordinate
    * @param z   the z coordinate
    * @return true when a matching waypoint exists
    */
   private boolean hasWaypoint(WaypointSet set, int x, int y, int z) {
      for (Waypoint waypoint : set.getWaypoints()) {
         if (waypoint.getX() == x && waypoint.getY() == y && waypoint.getZ() == z) {
            return true;
         }
      }
      return false;
   }

   /**
    * removes poi waypoints from all sets using exact integer coordinate matches.
    *
    * @param world the xaero world to update
    * @return true if any waypoints were removed
    */
   private boolean removePoisFromWorld(MinimapWorld world) {
      Set<PoiCoords> coords = collectPoiCoords(PoiRepository.loadPoisIncludingDisabled(), true);
      if (coords.isEmpty()) {
         coords = cachedPoiCoords;
      }
      if (coords.isEmpty()) {
         return false;
      }

      int removed = 0;
      for (WaypointSet set : world.getIterableWaypointSets()) {
         Iterator<Waypoint> iterator = set.getWaypoints().iterator();
         while (iterator.hasNext()) {
            Waypoint waypoint = iterator.next();
            if (coords.contains(new PoiCoords(waypoint.getX(), waypoint.getY(), waypoint.getZ()))) {
               iterator.remove();
               removed++;
            }
         }
      }

      if (removed > 0) {
         LOGGER.info("[lsu-xaero] removed {} poi waypoints", removed);
      }
      return removed > 0;
   }

   /**
    * collects integer coordinates for all pois.
    *
    * @return set of poi coordinates
    */
   private Set<PoiCoords> collectPoiCoords(List<PoiRepository.Poi> pois, boolean includeDisabled) {
      Set<PoiCoords> coords = new HashSet<>();
      for (PoiRepository.Poi poi : pois) {
         if (poi == null) {
            continue;
         }
         if (!includeDisabled && poi.disabled()) {
            continue;
         }
         coords.add(new PoiCoords(
                 (int) Math.round(poi.x()),
                 (int) Math.round(poi.y()),
                 (int) Math.round(poi.z())
         ));
      }
      return coords;
   }

   /**
    * removes disabled poi waypoints using exact integer coordinate matches.
    *
    * @param world the xaero world to update
    * @param pois  all poi entries (including disabled)
    * @return true if any waypoints were removed
    */
   private boolean removeDisabledPoisFromWorld(MinimapWorld world, List<PoiRepository.Poi> pois) {
      Set<PoiCoords> disabledCoords = new HashSet<>();
      for (PoiRepository.Poi poi : pois) {
         if (poi == null || !poi.disabled()) {
            continue;
         }
         disabledCoords.add(new PoiCoords(
                 (int) Math.round(poi.x()),
                 (int) Math.round(poi.y()),
                 (int) Math.round(poi.z())
         ));
      }

      if (disabledCoords.isEmpty()) {
         return false;
      }

      int removed = 0;
      for (WaypointSet set : world.getIterableWaypointSets()) {
         Iterator<Waypoint> iterator = set.getWaypoints().iterator();
         while (iterator.hasNext()) {
            Waypoint waypoint = iterator.next();
            if (disabledCoords.contains(new PoiCoords(waypoint.getX(), waypoint.getY(), waypoint.getZ()))) {
               iterator.remove();
               removed++;
            }
         }
      }

      if (removed > 0) {
         LOGGER.info("[lsu-xaero] removed {} disabled poi waypoints", removed);
      }
      return removed > 0;
   }

   /**
    * integer poi coordinates for exact matching.
    */
   private record PoiCoords(int x, int y, int z) {
   }

   /**
    * resolves a safe display name for a poi.
    *
    * @param poi the poi definition
    * @return a non-empty name
    */
   private String resolvePoiName(PoiRepository.Poi poi) {
      if (poi.name() == null || poi.name().isBlank()) {
         return FALLBACK_POI_NAME;
      }
      return poi.name();
   }

   /**
    * checks whether the poi should be added for the current shard.
    *
    * @param poi the poi definition
    * @return true if the poi should be added in the current shard
    */
   private boolean isPoiVisibleOnCurrentShard(PoiRepository.Poi poi) {
      if (poi.dimension() == null || poi.dimension().isBlank()) {
         return !isShardNether();
      }
      boolean poiNether = poi.dimension().contains(POI_DIMENSION_NETHER);
      boolean shardNether = isShardNether();
      return poiNether == shardNether;
   }

   /**
    * checks whether poi indicators should be hidden due to the current shard.
    *
    * @return true when on hub/spawn shards
    */
   private boolean isIndicatorsSuppressedForShard() {
      String shardName = LifestealTablistAPI.getCurrentShard();
      if (shardName == null || shardName.isBlank()) {
         return false;
      }
      String shardLower = shardName.toLowerCase();
      return shardLower.contains(SHARD_KEYWORD_HUB) || shardLower.contains(SHARD_KEYWORD_SPAWN);
   }

   /**
    * checks if the current shard is a nether shard.
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
}
