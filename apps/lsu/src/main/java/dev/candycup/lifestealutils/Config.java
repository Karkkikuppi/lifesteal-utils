package dev.candycup.lifestealutils;

import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.ConfigurableFloat;
import dev.candycup.lifestealutils.config.configurables.ConfigurableList;
import dev.candycup.lifestealutils.config.configurables.ConfigurableMinimessage;
import dev.candycup.lifestealutils.config.configurables.ConfigurableString;
import dev.candycup.lifestealutils.config.configurables.RequiresGaia;
import dev.candycup.configura.core.Configura;
import dev.candycup.configura.core.GsonJson5ConfiguraCodec;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
import dev.candycup.configura.serial.SerialEntry;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
   private static boolean applyingRemoteOverrides;
   public static Configura<Config> HANDLER = Configura.builder(Config.class)
           .containers(dev.candycup.lifestealutils.config.ConfigContainerRegistry.getRegisteredContainers())
           .path(FabricLoader.getInstance().getConfigDir().resolve("lifestealutils.json5"))
           .codec(new GsonJson5ConfiguraCodec(true))
           .build();

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to enable custom private message formatting")
   @ConfigurableBoolean(location = "customization.messages.pmformatenabled")
   private static boolean enablePmFormat = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Customize the format of private messages (/msg, /r)")
   @ConfigurableMinimessage(location = "customization.messages.pmformat")
   private static String pmFormat = "<light_purple><bold>{{direction}}</bold> {{sender}}</light_purple> <white>➡ {{message}}</white>";

   @Getter
   @Setter
   @SerialEntry(comment = "Custom panorama background on the title screen")
   @ConfigurableBoolean(location = "qol.titlescreen.custompanoramaenabled")
   private static boolean customPanoramaEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Disables chat tags, such as [No-Life] from appearing in messages for visual simplicity.")
   @ConfigurableBoolean(location = "customization.messages.disablechattags")
   private static boolean disableChatTags = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Allows you to add custom words & phrases that you can 'ghost' in chat (making the messages appear in a boring gray!)")
   @ConfigurableBoolean(location = "qol.desloppifychat.enabled")
   private static boolean desloppifierEnabled = false;

   @Getter
   @Setter
   @SerialEntry(comment = "List of words and catch phrases for the desloppifier. If they appear in a message in chat, the message will be shown in gray.")
   @ConfigurableList(location = "qol.desloppifychat.catchwords")
   private static List<String> desloppifiedPatterns = new ArrayList<>();

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to enable alliance features such as colored name tags.")
   @ConfigurableBoolean(location = "alliances.general.enablealliances")
   private static boolean enableAlliances = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to render alliance prefixes in nametags")
   @ConfigurableBoolean(location = "alliances.general.allianceprefixenabled")
   private static boolean allianceNamePrefixEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to apply list-specific alliance name colors to player names")
   @ConfigurableBoolean(location = "alliances.general.alliancenamecolorenabled")
   private static boolean allianceNameColorEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Allow alliance members to bypass ghosted chat filtering")
   @ConfigurableBoolean(location = "alliances.general.allowalliancebypassghostedchat")
   private static boolean allowAllianceBypassGhostedChat = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Selected alliance id used by quick-add actions")
   private static String selectedAllianceId = "";

   @Getter
   @Setter
   @SerialEntry(comment = "List of allied player UUIDs")
   private static List<String> allianceUuids = new ArrayList<>();

   @Getter
   @Setter
   @SerialEntry(comment = "Cache of UUID to username mappings for alliance members")
   private static Map<String, String> uuidUsernameCache = new HashMap<>();

   @Getter
   @Setter
   @SerialEntry(comment = "Locally stored alliances")
   private static List<LocalAllianceConfigEntry> localAlliances = new ArrayList<>();

   @Getter
   @Setter
   @SerialEntry(comment = "Whether legacy alliance UUIDs have been migrated to local alliances")
   private static boolean localAllianceMigrationDone = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to enable custom splashes on the title screen")
   @ConfigurableBoolean(location = "customization.titlescreen.customSplashes")
   private static boolean customSplashesEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Per-timer enabled state keyed by timer id")
   private static Map<String, Boolean> basicTimerEnabled = new HashMap<>();

   @Getter
   @Setter
   @SerialEntry(comment = "Per-timer format overrides keyed by timer id")
   private static Map<String, String> basicTimerFormatOverrides = new HashMap<>();

   @Getter
   @Setter
   @SerialEntry(comment = "Enable increased scale for rare items such as neth and custom enchants.")
   @ConfigurableBoolean(location = "qol.scaling.rareitemscaleenabled")
   private static boolean rareItemScaleEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Increased scale of the rare items.")
   @ConfigurableFloat(location = "qol.scaling.rareitemscale", min = 1.0f, max = 5.0f)
   private static float rareItemScale = 2.0f;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to enable the unbroken chain counter HUD element")
   @ConfigurableBoolean(location = "timers.chaincounter.enabled")
   private static boolean chainCounterEnabled = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Custom format for the unbroken chain counter display")
   @ConfigurableMinimessage(location = "timers.chaincounter.format")
   private static String chainCounterFormat = UnbrokenChainTracker.DEFAULT_FORMAT;

   @Getter
   @Setter
   @SerialEntry(comment = "Automatically join the Lifesteal gamemode when connecting to the lifesteal.net hub")
   @ConfigurableBoolean(location = "qol.autojoin.autojoinlifestealonhub")
   private static boolean autoJoinLifestealOnHub = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Enable the custom baltop interface that replaces the server's /baltop GUI")
   @RequiresGaia(forceStateWhenDenied = "false")
   @ConfigurableBoolean(location = "qol.customuis.custombaltopinterfaceenabled")
   private static boolean customBaltopInterfaceEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Enable the custom auction house interface overlay GUI")
   @RequiresGaia(forceStateWhenDenied = "false")
   @ConfigurableBoolean(location = "experiments.auctionui.experimentalauctionui", icon = "gold_ingot")
   private static boolean customAhInterfaceEnabled = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether the Gaia consent screen has been shown")
   private static boolean gaiaConsentSeen = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether advanced features are enabled after Gaia consent")
   private static boolean gaiaAdvancedFeaturesEnabled = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Show the actual potion duration in the tooltip, including your prestige perk boost")
   @ConfigurableBoolean(location = "qol.potions.showactualpotionduration")
   private static boolean showActualPotionDuration = true;

   @SerialEntry(comment = "Automatically hide custom timers if you don't have the custom in your inventory")
   @ConfigurableBoolean(location = "timers.customenchanttimers.autohide")
   private static boolean timerAutoHide = false;

   public Config() {

   }

   public static class LocalAllianceConfigEntry {
      public String id = "";
      public String name = "";
      public String prefix = "";
      public String color = "";
      public long createdAt = 0L;
      public long updatedAt = 0L;
      public List<LocalAllianceMemberConfigEntry> members = new ArrayList<>();
   }

   public static class LocalAllianceMemberConfigEntry {
      public String id = "";
      public String uuid = "";
      public String cachedName = "";
      public long addedAt = 0L;
      public String addedBy = "";
   }

   public static List<LocalAllianceConfigEntry> getLocalAlliances() {
      return localAlliances == null ? new ArrayList<>() : new ArrayList<>(localAlliances);
   }

   public static boolean isBasicTimerEnabled(String id) {
      return basicTimerEnabled.getOrDefault(id, false);
   }

   public static void setBasicTimerEnabled(String id, boolean enabled) {
      basicTimerEnabled.put(id, enabled);
      if (!applyingRemoteOverrides) {
         HANDLER.save();
      }
   }

   public static void ensureBasicTimerKnown(String id) {
      basicTimerEnabled.putIfAbsent(id, false);
   }

   public static String getBasicTimerFormat(String id, String fallback) {
      String value = basicTimerFormatOverrides.get(id);
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return value;
   }

   public static void setBasicTimerFormat(String id, String format) {
      basicTimerFormatOverrides.put(id, format);
      if (!applyingRemoteOverrides) {
         HANDLER.save();
      }
   }

   public static void ensureBasicTimerFormat(String id, String fallback) {
      basicTimerFormatOverrides.putIfAbsent(id, fallback);
   }

   public static boolean isTimerAutoHide() {
      return timerAutoHide;
   }

   public static void setTimerAutoHide(boolean value) {
      timerAutoHide = value;
      HANDLER.save();
   }

   public static void load() {
      FeatureFlagController.ensureLoaded();
      ConfigMigrations.beginSession();
      HANDLER = Configura.builder(Config.class)
              .containers(dev.candycup.lifestealutils.config.ConfigContainerRegistry.getRegisteredContainers())
              .path(FabricLoader.getInstance().getConfigDir().resolve("lifestealutils.json5"))
              .codec(new GsonJson5ConfiguraCodec(true))
              .migration(1, ConfigMigrations::applyMigration1)
              .migration(2, ConfigMigrations::applyMigration2)
              .migration(3, ConfigMigrations::applyMigration3)
              .build();
      HANDLER.load();
      if (ConfigMigrations.consumeTouched()) {
         HANDLER.save();
      }
      dev.candycup.lifestealutils.config.ConfigResolver.applyRemoteOverridesAtLoad();
   }

   public static void resetAll() {
      HANDLER.resetToDefaults();
      HANDLER.save();
   }

   public static boolean isGaiaAdvancedFeaturesEnabled() {
      return gaiaAdvancedFeaturesEnabled;
   }

   public static void setGaiaAdvancedFeaturesEnabled(boolean enabled) {
      gaiaAdvancedFeaturesEnabled = enabled;
      HANDLER.save();
   }

   public static boolean isEnableAlliances() {
      return enableAlliances;
   }

   public static boolean isAllianceNamePrefixEnabled() {
      return allianceNamePrefixEnabled;
   }

   public static boolean isAllianceNameColorEnabled() {
      return allianceNameColorEnabled;
   }

   public static boolean isDesloppifierEnabled() {
      return desloppifierEnabled;
   }

   public static List<String> getDesloppifiedPatterns() {
      return desloppifiedPatterns;
   }

   public static boolean isCustomBaltopInterfaceEnabled() {
      return customBaltopInterfaceEnabled;
   }

   public static boolean isCustomAhInterfaceEnabled() {
      return customAhInterfaceEnabled;
   }

   public static void runWithRemoteOverrideApplication(Runnable runnable) {
      boolean previous = applyingRemoteOverrides;
      applyingRemoteOverrides = true;
      try {
         runnable.run();
      } finally {
         applyingRemoteOverrides = previous;
      }
   }

}
