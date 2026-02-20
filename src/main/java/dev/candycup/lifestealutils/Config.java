package dev.candycup.lifestealutils;

import com.google.gson.GsonBuilder;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.ConfigurableEnum;
import dev.candycup.lifestealutils.config.configurables.ConfigurableFloat;
import dev.candycup.lifestealutils.config.configurables.ConfigurableList;
import dev.candycup.lifestealutils.config.configurables.ConfigurableMinimessage;
import dev.candycup.lifestealutils.config.configurables.ConfigurableString;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.candycup.lifestealutils.features.alliances.LocalAllianceMigrationUtils.ensureLocalAllianceMigration;
import static dev.candycup.lifestealutils.integrations.xaero.XaeroPoiWaypointIntegration.isXaeroMinimapInstalled;

public class Config {
   public static ConfigClassHandler<Config> HANDLER = LifestealUtilsConfigClassHandler.createBuilder(Config.class)
           .id(Identifier.fromNamespaceAndPath("lifestealutils", "config"))
           .serializer(config -> GsonConfigSerializerBuilder.create(config)
                   .setPath(FabricLoader.getInstance().getConfigDir().resolve("lifestealutils.json5"))
                   .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                   .setJson5(true)
                   .build())
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
   @SerialEntry(comment = "Removes the unique coloring of the plus in LSN+ for visual simplicity.")
   @ConfigurableBoolean(location = "customization.messages.removeuniquepluscolor")
   private static boolean removeUniquePlusColor = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to enable ghosted chat messages for matched regex patterns.")
   @ConfigurableBoolean(location = "customization.messages.ghostedchatenabled")
   private static boolean ghostedChatEnabled = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Regex patterns that will ghost matching chat messages.")
   @ConfigurableList(location = "customization.ghostedchatpatterns")
   private static List<String> ghostedChatPatterns = new ArrayList<>();

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
   @SerialEntry(comment = "Alliance priority list for choosing which prefix and color to display")
   @ConfigurableList(location = "alliances.allianceprefixpriority")
   private static List<String> alliancePrefixPriority = new ArrayList<>();

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
   @ConfigurableFloat(location = "qol.scaling.rareitemscale")
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
   @SerialEntry(comment = "Enable POI waypoints (directional HUD indicator)")
   @ConfigurableBoolean(location = "qol.pois.poiwaypointsenabled")
   private static boolean poiWaypointsEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Show directional arrow indicator pointing toward tracked POI")
   @ConfigurableBoolean(location = "qol.pois.poidirectionalindicatorenabled")
   private static boolean poiDirectionalIndicatorEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "How the POI HUD indicator is shown (text, compass, both, or none)")
   @ConfigurableEnum(location = "qol.pois.poihudindicatormode")
   private static PoiHudIndicatorMode poiHudIndicatorMode = PoiHudIndicatorMode.TEXT_AND_COMPASS;

   @Getter
   @Setter
   @SerialEntry(comment = "Unless you've configured to track a specific POI, show the closest one")
   @ConfigurableBoolean(location = "qol.pois.alwaysshowclosest")
   private static boolean poiAlwaysShowClosest = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Custom format for the POI waypoint display")
   @ConfigurableMinimessage(location = "qol.pois.poiwaypointformat")
   private static String poiWaypointFormat = "<gray><bold>{{poi}}</bold>: {{distance}} blocks away";

   @Getter
   @Setter
   @SerialEntry(comment = "Configured POI id to track (empty = none)")
   private static String poiTrackedId = "";

   @Getter
   @Setter
   @SerialEntry(comment = "Show Lifesteal Utils POIs as Xaero's Minimap waypoints")
   @ConfigurableBoolean(location = "qol.xaero.poiwaypointsenabled")
   private static boolean xaeroPoiWaypointsEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Automatically join the Lifesteal gamemode when connecting to the lifesteal.net hub")
   @ConfigurableBoolean(location = "qol.autojoin.autojoinlifestealonhub")
   private static boolean autoJoinLifestealOnHub = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Enable the custom baltop interface that replaces the server's /baltop GUI")
   @ConfigurableBoolean(location = "qol.customuis.custombaltopinterfaceenabled")
   private static boolean customBaltopInterfaceEnabled = true;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether the Gaia consent screen has been shown")
   private static boolean gaiaConsentSeen = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Whether advanced features are enabled after Gaia consent")
   private static boolean gaiaAdvancedFeaturesEnabled = false;

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

   /**
    * Describes how the POI HUD indicator should be displayed.
    */
   public enum PoiHudIndicatorMode {
      ONLY_TEXT("lsu.option.poiHudIndicatorMode.onlyText", true, false),
      TEXT_AND_COMPASS("lsu.option.poiHudIndicatorMode.textAndCompass", true, true),
      ONLY_COMPASS("lsu.option.poiHudIndicatorMode.onlyCompass", false, true),
      NONE("lsu.option.poiHudIndicatorMode.none", false, false);

      @Getter
      private final String translationKey;
      @Getter
      private final boolean showsTextIndicator;
      @Getter
      private final boolean showsCompassIndicator;

      PoiHudIndicatorMode(String translationKey, boolean showsText, boolean showsCompass) {
         this.translationKey = translationKey;
         this.showsTextIndicator = showsText;
         this.showsCompassIndicator = showsCompass;
      }
   }

   public static List<LocalAllianceConfigEntry> getLocalAlliances() {
      return localAlliances == null ? new ArrayList<>() : new ArrayList<>(localAlliances);
   }

   public static boolean isBasicTimerEnabled(String id) {
      return basicTimerEnabled.getOrDefault(id, false);
   }

   public static void setBasicTimerEnabled(String id, boolean enabled) {
      basicTimerEnabled.put(id, enabled);
      HANDLER.save();
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
      HANDLER.save();
   }

   public static void ensureBasicTimerFormat(String id, String fallback) {
      basicTimerFormatOverrides.putIfAbsent(id, fallback);
   }

   /**
    * Checks if Xaero POI waypoints should be displayed.
    *
    * @return true if xaero waypoints should be active
    */
   public static boolean isXaeroPoiWaypointsEnabled() {
      if (!isXaeroMinimapInstalled()) {
         return false;
      }
      Boolean forced = FeatureFlagController.getForcedState("xaeroPoiWaypointsEnabled");
      if (forced != null) return forced;
      return xaeroPoiWaypointsEnabled;
   }

   public static void load() {
      FeatureFlagController.ensureLoaded();
      HANDLER.load();
      ensureLocalAllianceMigration();
   }
}