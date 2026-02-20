package dev.candycup.lifestealutils.features.qol;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.FeatureFlagController;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class PoiTrackingController {
   private PoiTrackingController() {
   }

   public static CompletableFuture<Suggestions> suggestPois(String remaining, SuggestionsBuilder builder) {
      for (FeatureFlagController.PoiDefinition poi : FeatureFlagController.getPois()) {
         if (poi == null || poi.name() == null || poi.name().isBlank()) {
            continue;
         }
         String nameLower = poi.name().toLowerCase();
         if (remaining.isBlank() || nameLower.contains(remaining)) {
            builder.suggest(poi.name());
         }
      }
      return builder.buildFuture();
   }

   public static int trackPoiArgument(String poiArgRaw) {
      String poiArg = poiArgRaw == null ? "" : poiArgRaw.trim();
      if (poiArg.equalsIgnoreCase("none") || poiArg.equalsIgnoreCase("clear") || poiArg.equalsIgnoreCase("off")) {
         return untrackCurrentPoi();
      }

      FeatureFlagController.PoiDefinition matched = FeatureFlagController.getPois().stream()
              .filter(poi -> poi.name() != null && poi.name().equalsIgnoreCase(poiArg))
              .findFirst().orElse(null);
      if (matched == null) {
         MessagingUtils.showMiniMessage("<red>Unknown POI: <white>" + MiniMessage.miniMessage().escapeTags(poiArg) + "</white></red>");
         return 0;
      }

      FeatureFlagController.PoiDefinition currentTracked = resolveCurrentTrackedPoi();
      if (currentTracked != null && Objects.equals(currentTracked.id(), matched.id())) {
         return untrackCurrentPoi();
      }

      Config.setPoiTrackedId(matched.id());
      MessagingUtils.showMiniMessage("<green>Now tracking POI: <white>" + MiniMessage.miniMessage().escapeTags(matched.name()) + "</white></green>");
      return 1;
   }

   public static int untrackCurrentPoi() {
      FeatureFlagController.PoiDefinition currentTracked = resolveCurrentTrackedPoi();
      String trackedName = currentTracked != null && currentTracked.name() != null && !currentTracked.name().isBlank()
              ? currentTracked.name()
              : "POI";
      String escapedName = MiniMessage.miniMessage().escapeTags(trackedName);

      Config.setPoiTrackedId("");

      if (Config.isPoiAlwaysShowClosest()) {
         MessagingUtils.showMiniMessage("<yellow>No longer tracking <white>" + escapedName + "</white>! Reverting to tracking the closest POI.</yellow>");
      } else {
         MessagingUtils.showMiniMessage("<yellow>No longer tracking <white>" + escapedName + "</white>!</yellow>");
      }
      return 1;
   }

   private static FeatureFlagController.PoiDefinition resolveCurrentTrackedPoi() {
      String configuredId = Config.getPoiTrackedId();
      if (configuredId != null && !configuredId.isBlank()) {
         return resolvePoiById(configuredId);
      }
      if (!Config.isPoiAlwaysShowClosest()) {
         return null;
      }
      return resolveClosestPoi();
   }

   private static FeatureFlagController.PoiDefinition resolvePoiById(String id) {
      for (FeatureFlagController.PoiDefinition poi : FeatureFlagController.getPois()) {
         if (poi == null || poi.id() == null) {
            continue;
         }
         if (poi.id().equals(id)) {
            return poi;
         }
      }
      return null;
   }

   private static FeatureFlagController.PoiDefinition resolveClosestPoi() {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         return null;
      }

      String currentDimension = null;
      try {
         if (client.level.dimension() != null) {
            currentDimension = client.level.dimension().toString();
         }
      } catch (Exception ignored) {
      }

      double px = client.player.getX();
      double pz = client.player.getZ();

      FeatureFlagController.PoiDefinition best = null;
      double bestDist = Double.MAX_VALUE;
      for (FeatureFlagController.PoiDefinition poi : FeatureFlagController.getPois()) {
         if (poi == null) {
            continue;
         }
         if (poi.dimension() != null && currentDimension != null) {
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

      return best;
   }
}
