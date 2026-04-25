package dev.candycup.lifestealutils;

import dev.candycup.configura.core.ConfiguraMigration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigMigrations {
   private static boolean migrationTouched;

   private ConfigMigrations() {
   }

   public static void beginSession() {
      migrationTouched = false;
   }

   public static boolean consumeTouched() {
      boolean touched = migrationTouched;
      migrationTouched = false;
      return touched;
   }

   public static void applyMigration1(Map<String, Object> map) {
      Map<String, Object> before = new LinkedHashMap<>(map);
      ConfiguraMigration.invertBoolean(map, "removeEmojis", "enableEmojis");
      ConfiguraMigration.invertBoolean(map, "removeAllShieldOverrides", "enableShieldSkins");
      ConfiguraMigration.invertBoolean(map, "removeSwordSkins", "enableSwordSkins");
      ConfiguraMigration.invertBoolean(map, "removeAxeSkins", "enableAxeSkins");
      ConfiguraMigration.invertBoolean(map, "removePickaxeSkins", "enablePickaxeSkins");
      ConfiguraMigration.invertBoolean(map, "removeShovelSkins", "enableShovelSkins");
      ConfiguraMigration.invertBoolean(map, "removeMaceSkins", "enableMaceSkins");
      ConfiguraMigration.invertBoolean(map, "removeBowSkins", "enableBowSkins");
      ConfiguraMigration.invertBoolean(map, "removeCrossbowSkins", "enableCrossbowSkins");
      markTouchedIfChanged(before, map);
   }

   public static void applyMigration2(Map<String, Object> map) {
      Map<String, Object> before = new LinkedHashMap<>(map);
      Map<String, Object> group = new LinkedHashMap<>();
      String[] keys = {"enableEmojis", "enableShieldSkins", "enableSwordSkins",
              "enableAxeSkins", "enablePickaxeSkins", "enableShovelSkins",
              "enableMaceSkins", "enableBowSkins", "enableCrossbowSkins"};
      for (String key : keys) {
         Object val = map.remove(key);
         group.put(key, val instanceof Boolean b ? b : true);
      }
      map.put("resourcePackToggles", group);
      markTouchedIfChanged(before, map);
   }

   public static void applyMigration3(Map<String, Object> map) {
      Map<String, Object> before = new LinkedHashMap<>(map);
      Object legacyEnabled = map.remove("ghostedChatEnabled");
      Object legacyPatterns = map.remove("ghostedChatPatterns");

      if (legacyEnabled instanceof Boolean && !map.containsKey("desloppifierEnabled")) {
         map.put("desloppifierEnabled", legacyEnabled);
      }

      if (legacyPatterns instanceof List<?> && !map.containsKey("desloppifiedPatterns")) {
         map.put("desloppifiedPatterns", legacyPatterns);
      }

      markTouchedIfChanged(before, map);
   }

   private static void markTouchedIfChanged(Map<String, Object> before, Map<String, Object> after) {
      if (!before.equals(after)) {
         migrationTouched = true;
      }
   }
}
