package dev.candycup.lifestealutils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {

   @Test
   void migrationPathFromStage0ProducesExpectedLayout() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("removeEmojis", true);
      map.put("removeAllShieldOverrides", false);
      map.put("removeSwordSkins", true);
      map.put("removeAxeSkins", false);
      map.put("removePickaxeSkins", true);
      map.put("removeShovelSkins", false);
      map.put("removeMaceSkins", true);
      map.put("removeBowSkins", false);
      map.put("removeCrossbowSkins", true);

      applyMigrationsFromStage(map, 0);

      Map<String, Object> toggles = asMap(map.get("resourcePackToggles"));
      assertNotNull(toggles);
      assertEquals(false, toggles.get("enableEmojis"));
      assertEquals(true, toggles.get("enableShieldSkins"));
      assertEquals(false, toggles.get("enableSwordSkins"));
      assertEquals(true, toggles.get("enableAxeSkins"));
      assertEquals(false, toggles.get("enablePickaxeSkins"));
      assertEquals(true, toggles.get("enableShovelSkins"));
      assertEquals(false, toggles.get("enableMaceSkins"));
      assertEquals(true, toggles.get("enableBowSkins"));
      assertEquals(false, toggles.get("enableCrossbowSkins"));
   }

   @Test
   void migrationPathFromStage1ProducesExpectedLayout() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("enableEmojis", false);
      map.put("enableShieldSkins", true);
      map.put("enableSwordSkins", false);
      map.put("enableAxeSkins", true);
      map.put("enablePickaxeSkins", false);
      map.put("enableShovelSkins", true);
      map.put("enableMaceSkins", false);
      map.put("enableBowSkins", true);
      map.put("enableCrossbowSkins", false);

      applyMigrationsFromStage(map, 1);

      Map<String, Object> toggles = asMap(map.get("resourcePackToggles"));
      assertNotNull(toggles);
      assertEquals(false, toggles.get("enableEmojis"));
      assertEquals(true, toggles.get("enableShieldSkins"));
      assertEquals(false, toggles.get("enableSwordSkins"));
      assertEquals(true, toggles.get("enableAxeSkins"));
      assertEquals(false, toggles.get("enablePickaxeSkins"));
      assertEquals(true, toggles.get("enableShovelSkins"));
      assertEquals(false, toggles.get("enableMaceSkins"));
      assertEquals(true, toggles.get("enableBowSkins"));
      assertEquals(false, toggles.get("enableCrossbowSkins"));
   }

   @Test
   void migrationPathFromStage2RemovesLegacyDesloppifierKeys() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("ghostedChatEnabled", true);
      map.put("ghostedChatPatterns", List.of("slop", "brainrot"));

      applyMigrationsFromStage(map, 2);

      assertEquals(true, map.get("desloppifierEnabled"));
      assertEquals(List.of("slop", "brainrot"), map.get("desloppifiedPatterns"));
      assertFalse(map.containsKey("ghostedChatEnabled"));
      assertFalse(map.containsKey("ghostedChatPatterns"));
   }

   @Test
   void migrationPathFromStage2KeepsExistingDesloppifierValues() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("ghostedChatEnabled", true);
      map.put("ghostedChatPatterns", List.of("legacy"));
      map.put("desloppifierEnabled", false);
      map.put("desloppifiedPatterns", List.of("current"));

      applyMigrationsFromStage(map, 2);

      assertEquals(false, map.get("desloppifierEnabled"));
      assertEquals(List.of("current"), map.get("desloppifiedPatterns"));
      assertFalse(map.containsKey("ghostedChatEnabled"));
      assertFalse(map.containsKey("ghostedChatPatterns"));
   }

   private static void applyMigrationsFromStage(Map<String, Object> map, int currentStage) {
      if (currentStage < 1) {
         ConfigMigrations.applyMigration1(map);
      }
      if (currentStage < 2) {
         ConfigMigrations.applyMigration2(map);
      }
      if (currentStage < 3) {
         ConfigMigrations.applyMigration3(map);
      }
   }

   @SuppressWarnings("unchecked")
   private static Map<String, Object> asMap(Object value) {
      return (Map<String, Object>) value;
   }
}
