package dev.candycup.lifestealutils.features.resources;

import dev.candycup.configura.core.ToggleGroup;
import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableToggleGroup;

public class ResourcePackOverrides {
   @SerialEntry(comment = "Toggle overrides for server-provided resource pack features")
   @ConfigurableToggleGroup(location = "customization.resourcepack.overrides", icon = "minecraft:lever")
   private static ToggleGroup resourcePackToggles = ToggleGroup.builder()
           .entry("enableEmojis", true, "minecraft:book")
           .entry("enableShieldSkins", true, "minecraft:shield")
           .entry("enableSwordSkins", true, "minecraft:diamond_sword")
           .entry("enableAxeSkins", true, "minecraft:diamond_axe")
           .entry("enablePickaxeSkins", true, "minecraft:diamond_pickaxe")
           .entry("enableShovelSkins", true, "minecraft:diamond_shovel")
           .entry("enableMaceSkins", true, "minecraft:mace")
           .entry("enableBowSkins", true, "minecraft:bow")
           .entry("enableCrossbowSkins", true, "minecraft:crossbow")
           .build();

   public static boolean isEnableEmojis() {
      return resourcePackToggles.get("enableEmojis");
   }

   public static boolean isEnableShieldSkins() {
      return resourcePackToggles.get("enableShieldSkins");
   }

   public static boolean isEnableSwordSkins() {
      return resourcePackToggles.get("enableSwordSkins");
   }

   public static boolean isEnableAxeSkins() {
      return resourcePackToggles.get("enableAxeSkins");
   }

   public static boolean isEnablePickaxeSkins() {
      return resourcePackToggles.get("enablePickaxeSkins");
   }

   public static boolean isEnableShovelSkins() {
      return resourcePackToggles.get("enableShovelSkins");
   }

   public static boolean isEnableMaceSkins() {
      return resourcePackToggles.get("enableMaceSkins");
   }

   public static boolean isEnableBowSkins() {
      return resourcePackToggles.get("enableBowSkins");
   }

   public static boolean isEnableCrossbowSkins() {
      return resourcePackToggles.get("enableCrossbowSkins");
   }
}
