package dev.candycup.lifestealutils.features.qol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import dev.candycup.lifestealutils.FeatureFlagController;

public final class PoiRepository {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/poi");

   private PoiRepository() {
   }

   public static List<Poi> loadPois() {
      List<Poi> list = new ArrayList<>();
      try {
         List<FeatureFlagController.PoiDefinition> defs = FeatureFlagController.getPois();
         for (FeatureFlagController.PoiDefinition d : defs) {
            list.add(new Poi(d.id(), d.name(), d.x(), d.y(), d.z(), d.dimension(), d.disabled()));
         }
         LOGGER.info("[lsu-poi] loaded {} POIs from feature flags", list.size());
      } catch (Exception e) {
         LOGGER.warn("[lsu-poi] failed to load POIs from feature flags; returning empty list", e);
      }
      return list;
   }

   public static List<Poi> loadPoisIncludingDisabled() {
      List<Poi> list = new ArrayList<>();
      try {
         List<FeatureFlagController.PoiDefinition> defs = FeatureFlagController.getPoisIncludingDisabled();
         for (FeatureFlagController.PoiDefinition d : defs) {
            list.add(new Poi(d.id(), d.name(), d.x(), d.y(), d.z(), d.dimension(), d.disabled()));
         }
         LOGGER.info("[lsu-poi] loaded {} POIs (including disabled) from feature flags", list.size());
      } catch (Exception e) {
         LOGGER.warn("[lsu-poi] failed to load POIs from feature flags; returning empty list", e);
      }
      return list;
   }

   /**
    * @param dimension may be null
    */
   public record Poi(String id, String name, double x, double y, double z, String dimension, boolean disabled) {

      @Override
      public String toString() {
         return "Poi{" + id + "," + name + " (" + x + "," + y + "," + z + ")" + (dimension != null ? "@" + dimension : "") + (disabled ? ":disabled" : "") + "}";
      }
   }
}
