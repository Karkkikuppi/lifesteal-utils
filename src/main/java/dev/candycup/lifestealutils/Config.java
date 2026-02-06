package dev.candycup.lifestealutils;

import com.google.gson.GsonBuilder;
import dev.candycup.lifestealutils.features.timers.BasicTimerManager;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.controllers.slider.FloatSliderController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
   public static ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
           .id(Identifier.fromNamespaceAndPath("lifestealutils", "config"))
           .serializer(config -> GsonConfigSerializerBuilder.create(config)
                   .setPath(FabricLoader.getInstance().getConfigDir().resolve("lifestealutils.json5"))
                   .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                   .setJson5(true)
                   .build())
           .build();

   @SerialEntry(comment = "Whether to enable custom private message formatting")
   public static boolean enablePmFormat = false;

   @SerialEntry(comment = "Customize the format of private messages (/msg, /r)")
   public static String pmFormat = "<light_purple><bold>{{direction}}</bold> {{sender}}</light_purple> <white>➡ {{message}}</white>";

   /*
   @SerialEntry(comment = "Whether to enable custom claim chat formatting")
   public static boolean enableClaimChatFormat = false;

   @SerialEntry(comment = "Customize the format of claim chat messages")
   public static String claimChatFormat = "<gold><bold>{{claim}}</bold></gold> <dark_gray>|</dark_gray> <aqua>{{username}}</aqua><gray>:</gray> <white>{{message}}</white>";
   */

   @SerialEntry(comment = "Quick Join button on the title screen")
   public static boolean quickJoinButtonEnabled = true;

   @SerialEntry(comment = "Custom panorama background on the title screen")
   public static boolean customPanoramaEnabled = true;

   @SerialEntry(comment = "Disables chat tags, such as [No-Life] from appearing in messages for visual simplicity.")
   public static boolean disableChatTags = false;

   @SerialEntry(comment = "Removes the unique coloring of the plus in LSN+ for visual simplicity.")
   public static boolean removeUniquePlusColor = false;

   @SerialEntry(comment = "Whether to enable alliance features such as colored name tags.")
   public static boolean enableAlliances = true;

   @SerialEntry(comment = "Alliance name color as ARGB int")
   public static int allianceNameColor = 0xFF55FF55;

   @SerialEntry(comment = "List of allied player UUIDs")
   public static List<String> allianceUuids = new ArrayList<>();

   @SerialEntry(comment = "Cache of UUID to username mappings for alliance members")
   public static Map<String, String> uuidUsernameCache = new HashMap<>();

   @SerialEntry(comment = "Whether to enable custom splashes on the title screen")
   public static boolean customSplashes = true;

   @SerialEntry(comment = "Per-timer enabled state keyed by timer id")
   public static Map<String, Boolean> basicTimerEnabled = new HashMap<>();

   @SerialEntry(comment = "Per-timer format overrides keyed by timer id")
   public static Map<String, String> basicTimerFormatOverrides = new HashMap<>();

   @SerialEntry(comment = "Enable increased scale for rare items such as neth and custom enchants.")
   public static boolean rareItemScaleEnabled = true;

   @SerialEntry(comment = "Increased scale of the rare items.")
   public static float rareItemScale = 2.0f;

   @SerialEntry(comment = "Whether to enable the unbroken chain counter HUD element")
   public static boolean chainCounterEnabled = false;

   @SerialEntry(comment = "Custom format for the unbroken chain counter display")
   public static String chainCounterFormat = "";

   @SerialEntry(comment = "Whether to enable the heavenly durability calculator HUD element")
   public static boolean heavenlyDurabilityCalculatorEnabled = false;

   @SerialEntry(comment = "Custom format for the heavenly durability calculator display")
   public static String heavenlyDurabilityCalculatorFormat = "";

   @SerialEntry(comment = "Enable POI waypoints (directional HUD indicator)")
   public static boolean poiWaypointsEnabled = true;

   @SerialEntry(comment = "Show directional arrow indicator pointing toward tracked POI")
   public static boolean poiDirectionalIndicatorEnabled = true;

   @SerialEntry(comment = "How the POI HUD indicator is shown (text, compass, both, or none)")
   public static PoiHudIndicatorMode poiHudIndicatorMode = null;

   @SerialEntry(comment = "Unless you've configured to track a specific POI, show the closest one")
   public static boolean poiAlwaysShowClosest = false;

   @SerialEntry(comment = "Custom format for the POI waypoint display")
   public static String poiWaypointFormat = "<gray><bold>{{poi}}</bold>: {{distance}} blocks away";

   @SerialEntry(comment = "Configured POI id to track (empty = none)")
   public static String poiTrackedId = "";

   @SerialEntry(comment = "Show Lifesteal Utils POIs as Xaero's Minimap waypoints")
   public static boolean xaeroPoiWaypointsEnabled = true;

   @SerialEntry(comment = "Automatically join the Lifesteal gamemode when connecting to the lifesteal.net hub")
   public static boolean autoJoinLifestealOnHub = false;

   @SerialEntry(comment = "Enable the custom baltop interface that replaces the server's /baltop GUI")
   public static boolean customBaltopInterfaceEnabled = true;

   /**
    * Describes how the POI HUD indicator should be displayed.
    */
   public enum PoiHudIndicatorMode {
      ONLY_TEXT("lsu.option.poiHudIndicatorMode.onlyText", true, false),
      TEXT_AND_COMPASS("lsu.option.poiHudIndicatorMode.textAndCompass", true, true),
      ONLY_COMPASS("lsu.option.poiHudIndicatorMode.onlyCompass", false, true),
      NONE("lsu.option.poiHudIndicatorMode.none", false, false);

      private final String translationKey;
      private final boolean showsText;
      private final boolean showsCompass;

      PoiHudIndicatorMode(String translationKey, boolean showsText, boolean showsCompass) {
         this.translationKey = translationKey;
         this.showsText = showsText;
         this.showsCompass = showsCompass;
      }

      /**
       * Gets the translation key for this mode.
       *
       * @return the translation key
       */
      public String getTranslationKey() {
         return translationKey;
      }

      /**
       * Checks if this mode displays the text label.
       *
       * @return true if text should be shown
       */
      public boolean showsText() {
         return showsText;
      }

      /**
       * Checks if this mode displays the compass indicator.
       *
       * @return true if compass should be shown
       */
      public boolean showsCompass() {
         return showsCompass;
      }
   }

   private static OptionDescription descriptionWithRemoteReasoning(String baseMiniMessage, String featureKey) {
      OptionDescription.Builder builder = OptionDescription.createBuilder()
              .text(MessagingUtils.miniMessage(baseMiniMessage));
      String reasoning = FeatureFlagController.getReasoning(featureKey);
      if (reasoning != null && !reasoning.isBlank()) {
         builder.text(MessagingUtils.miniMessage(reasoning));
      }
      return builder.build();
   }

   private static OptionDescription xaeroPoiWaypointDescription() {
      OptionDescription.Builder builder = OptionDescription.createBuilder()
              .text(Component.translatable("lsu.option.xaeroPoiWaypointsEnabled.description"));
      if (!isXaeroMinimapInstalled()) {
         builder.text(Component.translatable("lsu.option.xaeroPoiWaypointsEnabled.missingMod"));
      }
      String reasoning = FeatureFlagController.getReasoning("xaeroPoiWaypointsEnabled");
      if (reasoning != null && !reasoning.isBlank()) {
         builder.text(MessagingUtils.miniMessage(reasoning));
      }
      return builder.build();
   }

   private static OptionGroup buildTimerOptions() {
      OptionGroup.Builder group = OptionGroup.createBuilder()
              .name(Component.translatable("lsu.group.customEnchantTimers"));

      BasicTimerManager timerManager = LifestealUtils.getBasicTimerManager();
      if (timerManager == null) {
         return group.build();
      }

      List<BasicTimerManager.TimerEntry> timers = timerManager.getTimerEntries();

      timers.forEach(entry -> {
         String id = entry.id();
         ensureBasicTimerKnown(id);
         ensureBasicTimerFormat(id, entry.definition().defaultFormat());
         String label = entry.definition().toggleOption() != null && !entry.definition().toggleOption().isBlank()
                 ? entry.definition().toggleOption()
                 : entry.definition().name();

         group.option(Option.<Boolean>createBuilder()
                 .name(Component.literal(label))
                 .binding(false, () -> isBasicTimerEnabled(id), enabled -> setBasicTimerEnabled(id, enabled))
                 .controller(TickBoxControllerBuilder::create)
                 .build());
      });

      timers.forEach(entry -> {
         String id = entry.id();
         String label = entry.definition().name() + " Format";
         String fallback = entry.definition().defaultFormat();

         group.option(Option.<String>createBuilder()
                 .name(Component.literal(label))
                 .binding(getBasicTimerFormat(id, fallback), () -> getBasicTimerFormat(id, fallback), format -> setBasicTimerFormat(id, format))
                 .controller(StringControllerBuilder::create)
                 .build());
      });

      return group.build();
   }

   private static OptionGroup buildChainCounterOptions() {
      String defaultFormat = "<gray>Chain:</gray> <gold>{{count}}</gold> <gray>(+{{bonus}}% dmg)</gray>";
      ensureChainCounterFormat(defaultFormat);

      return OptionGroup.createBuilder()
              .name(Component.translatable("lsu.group.chainCounter"))
              .option(Option.<Boolean>createBuilder()
                      .name(Component.translatable("lsu.option.chainCounterEnabled.name"))
                      .description(OptionDescription.createBuilder()
                              .text(MessagingUtils.miniMessage(
                                      "Tracks consecutive hits without receiving damage.\n\n" +
                                              "Each consecutive hit grants +5% bonus damage, capping at 50%.\n" +
                                              "Bonus starts on the 3rd hit and the chain resets after 5 seconds without a hit."
                              ))
                              .build())
                      .binding(false, Config::isChainCounterEnabled, Config::setChainCounterEnabled)
                      .controller(TickBoxControllerBuilder::create)
                      .available(FeatureFlagController.isFeatureAvailable("chainCounterEnabled"))
                      .build())
              .option(Option.<String>createBuilder()
                      .name(Component.translatable("lsu.option.chainCounterFormat.name"))
                      .description(OptionDescription.createBuilder()
                              .text(MessagingUtils.miniMessage(
                                      "Customize the chain counter display format.\n\n" +
                                              "Placeholders:\n" +
                                              "- <gray>{{count}}</gray> - current chain count\n" +
                                              "- <gray>{{bonus}}</gray> - bonus damage percentage\n\n" +
                                              "Default: " + defaultFormat
                              ))
                              .build())
                      .binding(defaultFormat, () -> getChainCounterFormat(defaultFormat), Config::setChainCounterFormat)
                      .controller(StringControllerBuilder::create)
                      .build())
              .build();
   }

   private static OptionGroup buildHeavenlyDurabilityCalculatorOptions() {
      String defaultFormat = "<gold><bold>Dura after heavenly:</bold></gold><white> {{durability}}</white>";
      ensureHeavenlyDurabilityFormat(defaultFormat);

      return OptionGroup.createBuilder()
              .name(Component.translatable("lsu.group.heavenlyDurabilityCalculator"))
              .option(Option.<Boolean>createBuilder()
                      .name(Component.translatable("lsu.option.heavenlyDurabilityEnabled.name"))
                      .description(OptionDescription.createBuilder()
                              .text(MessagingUtils.miniMessage(
                                      "Calculates how much durability your helmet would have after triggering Heavenly."
                              ))
                              .build())
                      .binding(false, Config::isHeavenlyDurabilityCalculatorEnabled, Config::setHeavenlyDurabilityCalculatorEnabled)
                      .controller(TickBoxControllerBuilder::create)
                      .build())
              .option(Option.<String>createBuilder()
                      .name(Component.translatable("lsu.option.heavenlyDurabilityFormat.name"))
                      .description(OptionDescription.createBuilder()
                              .text(MessagingUtils.miniMessage(
                                      "Customize the heavenly durability calculator display format.\n\n" +
                                              "Placeholders:\n" +
                                              "- <gray>{{durability}}</gray> - calculated durability after heavenly\n\n" +
                                              "Default: " + defaultFormat
                              ))
                              .build())
                      .binding(defaultFormat, () -> getHeavenlyDurabilityFormat(defaultFormat), Config::setHeavenlyDurabilityFormat)
                      .controller(StringControllerBuilder::create)
                      .build())
              .build();
   }

   public static void setPmFormat(String format) {
      Config.pmFormat = format;
   }

   /*
   public static boolean getEnableClaimChatFormat() {
      return Config.enableClaimChatFormat;
   }

   public static void setEnableClaimChatFormat(boolean enable) {
      Config.enableClaimChatFormat = enable;
      HANDLER.save();
   }

   public static String getClaimChatFormat() {
      return Config.claimChatFormat;
   }

   public static void setClaimChatFormat(String format) {
      Config.claimChatFormat = format;
   }
   */

   public static boolean getEnablePmFormat() {
      Boolean forcedState = FeatureFlagController.getForcedState("enablePmFormat");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.enablePmFormat;
   }

   public static void setEnablePmFormat(boolean enable) {
      Config.enablePmFormat = enable;
      HANDLER.save();
   }

   public static boolean getQuickJoinButtonEnabled() {
      Boolean forcedState = FeatureFlagController.getForcedState("quickJoinButtonEnabled");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.quickJoinButtonEnabled;
   }

   public static void setQuickJoinButtonEnabled(boolean enabled) {
      Config.quickJoinButtonEnabled = enabled;
      HANDLER.save();
   }

   public static boolean getCustomPanoramaEnabled() {
      Boolean forcedState = FeatureFlagController.getForcedState("customPanoramaEnabled");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.customPanoramaEnabled;
   }

   public static void setCustomPanoramaEnabled(boolean enabled) {
      Config.customPanoramaEnabled = enabled;
      HANDLER.save();
   }

   public static boolean getDisableChatTags() {
      Boolean forcedState = FeatureFlagController.getForcedState("disableChatTags");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.disableChatTags;
   }

   public static void setDisableChatTags(boolean enabled) {
      Config.disableChatTags = enabled;
      HANDLER.save();
   }

   public static boolean getRemoveUniquePlusColor() {
      Boolean forcedState = FeatureFlagController.getForcedState("removeUniquePlusColor");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.removeUniquePlusColor;
   }

   public static void setRemoveUniquePlusColor(boolean enabled) {
      Config.removeUniquePlusColor = enabled;
      HANDLER.save();
   }

   public static boolean getEnableAlliances() {
      Boolean forcedState = FeatureFlagController.getForcedState("enableAlliances");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.enableAlliances;
   }

   public static void setEnableAlliances(boolean enabled) {
      Config.enableAlliances = enabled;
      HANDLER.save();
   }

   public static int getAllianceNameColor() {
      return Config.allianceNameColor;
   }

   public static void setAllianceNameColor(int color) {
      Config.allianceNameColor = color;
      HANDLER.save();
   }

   public static Color getAllianceNameColorValue() {
      return new Color(Config.allianceNameColor, true);
   }

   public static void setAllianceNameColorValue(Color color) {
      if (color == null) return;
      Config.allianceNameColor = color.getRGB();
      HANDLER.save();
   }

   public static String getAllianceNameColorTag() {
      return String.format("#%06X", Config.allianceNameColor & 0xFFFFFF);
   }

   public static List<String> getAllianceUuids() {
      return Config.allianceUuids;
   }

   public static void setAllianceUuids(List<String> uuids) {
      Config.allianceUuids = uuids;
      HANDLER.save();
   }

   public static Map<String, String> getUuidUsernameCache() {
      return Config.uuidUsernameCache;
   }

   public static void setUuidUsernameCache(Map<String, String> cache) {
      Config.uuidUsernameCache = cache;
      HANDLER.save();
   }

   public static boolean getCustomSplashes() {
      Boolean forcedState = FeatureFlagController.getForcedState("customSplashes");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.customSplashes;
   }

   public static void setCustomSplashes(boolean enabled) {
      Config.customSplashes = enabled;
      HANDLER.save();
   }

   public static boolean isRareItemScaling() {
      Boolean forcedState = FeatureFlagController.getForcedState("rareItemScaleEnabled");
      if (forcedState != null) {
         return forcedState;
      }
      return Config.rareItemScaleEnabled;
   }

   public static void toggleRareItemScaling(boolean enabled) {
      Config.rareItemScaleEnabled = enabled;
      HANDLER.save();
   }

   public static float getRareItemScaling() {
      return Config.rareItemScale;
   }

   public static void setRareItemScaling(float scale) {
      Config.rareItemScale = scale;
      HANDLER.save();
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

   public static boolean isChainCounterEnabled() {
      Boolean forcedState = FeatureFlagController.getForcedState("chainCounterEnabled");
      if (forcedState != null) {
         return forcedState;
      }
      return chainCounterEnabled;
   }

   public static void setChainCounterEnabled(boolean enabled) {
      chainCounterEnabled = enabled;
      HANDLER.save();
   }

   public static String getChainCounterFormat(String fallback) {
      if (chainCounterFormat == null || chainCounterFormat.isBlank()) {
         return fallback;
      }
      return chainCounterFormat;
   }

   public static void setChainCounterFormat(String format) {
      chainCounterFormat = format;
      HANDLER.save();
   }

   public static void ensureChainCounterFormat(String fallback) {
      if (chainCounterFormat == null || chainCounterFormat.isBlank()) {
         chainCounterFormat = fallback;
      }
   }

   public static boolean isHeavenlyDurabilityCalculatorEnabled() {
      return heavenlyDurabilityCalculatorEnabled;
   }

   public static void setHeavenlyDurabilityCalculatorEnabled(boolean enabled) {
      heavenlyDurabilityCalculatorEnabled = enabled;
      HANDLER.save();
   }

   public static String getHeavenlyDurabilityFormat(String fallback) {
      if (heavenlyDurabilityCalculatorFormat == null || heavenlyDurabilityCalculatorFormat.isBlank()) {
         return fallback;
      }
      return heavenlyDurabilityCalculatorFormat;
   }

   public static void setHeavenlyDurabilityFormat(String format) {
      heavenlyDurabilityCalculatorFormat = format;
      HANDLER.save();
   }

   public static void ensureHeavenlyDurabilityFormat(String fallback) {
      if (heavenlyDurabilityCalculatorFormat == null || heavenlyDurabilityCalculatorFormat.isBlank()) {
         heavenlyDurabilityCalculatorFormat = fallback;
      }
   }

   public static boolean getPoiWaypointsEnabled() {
      Boolean forced = FeatureFlagController.getForcedState("poiWaypointsEnabled");
      if (forced != null) return forced;
      return poiWaypointsEnabled;
   }

   public static void setPoiWaypointsEnabled(boolean enabled) {
      poiWaypointsEnabled = enabled;
      HANDLER.save();
   }

   /**
    * Retrieves the configured POI HUD indicator mode.
    *
    * @return the indicator mode
    */
   public static PoiHudIndicatorMode getPoiHudIndicatorMode() {
      ensurePoiHudIndicatorMode();
      return poiHudIndicatorMode;
   }

   /**
    * Updates the POI HUD indicator mode.
    *
    * @param mode the new indicator mode
    */
   public static void setPoiHudIndicatorMode(PoiHudIndicatorMode mode) {
      poiHudIndicatorMode = mode == null ? PoiHudIndicatorMode.TEXT_AND_COMPASS : mode;
      HANDLER.save();
   }

   /**
    * Checks if the POI HUD text should be displayed.
    *
    * @return true if the text should render
    */
   public static boolean isPoiHudTextEnabled() {
      if (!getPoiWaypointsEnabled()) {
         return false;
      }
      return getPoiHudIndicatorMode().showsText();
   }

   /**
    * Checks if the POI HUD compass indicator should be displayed.
    *
    * @return true if the compass should render
    */
   public static boolean isPoiHudCompassEnabled() {
      if (!getPoiWaypointsEnabled()) {
         return false;
      }
      return getPoiHudIndicatorMode().showsCompass();
   }

   public static boolean isPoiDirectionalIndicatorEnabled() {
      return isPoiHudCompassEnabled();
   }

   public static void setPoiDirectionalIndicatorEnabled(boolean enabled) {
      poiDirectionalIndicatorEnabled = enabled;
      if (poiHudIndicatorMode == null) {
         poiHudIndicatorMode = enabled ? PoiHudIndicatorMode.TEXT_AND_COMPASS : PoiHudIndicatorMode.ONLY_TEXT;
      }
      HANDLER.save();
   }

   public static boolean isPoiAlwaysShowClosest() {
      Boolean forced = FeatureFlagController.getForcedState("poiAlwaysShowClosest");
      if (forced != null) return forced;
      return poiAlwaysShowClosest;
   }

   public static void setPoiAlwaysShowClosest(boolean enabled) {
      poiAlwaysShowClosest = enabled;
      HANDLER.save();
   }

   public static String getPoiWaypointFormat(String fallback) {
      if (poiWaypointFormat == null || poiWaypointFormat.isBlank()) {
         return fallback;
      }
      return poiWaypointFormat;
   }

   public static void setPoiWaypointFormat(String format) {
      poiWaypointFormat = format;
      HANDLER.save();
   }

   public static void ensurePoiWaypointFormat(String fallback) {
      if (poiWaypointFormat == null || poiWaypointFormat.isBlank()) {
         poiWaypointFormat = fallback;
      }
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

   /**
    * Updates Xaero POI waypoint integration state.
    *
    * @param enabled whether xaero poi waypoints should be active
    */
   public static void setXaeroPoiWaypointsEnabled(boolean enabled) {
      xaeroPoiWaypointsEnabled = enabled;
      HANDLER.save();
   }

   /**
    * Checks if Xaero's Minimap is installed.
    *
    * @return true when xaero minimap is present
    */
   public static boolean isXaeroMinimapInstalled() {
      return FabricLoader.getInstance().isModLoaded("xaerominimap");
   }

   /**
    * Ensures the POI HUD indicator mode is set, using legacy values if needed.
    */
   public static void ensurePoiHudIndicatorMode() {
      if (poiHudIndicatorMode != null) {
         return;
      }
      poiHudIndicatorMode = poiDirectionalIndicatorEnabled
              ? PoiHudIndicatorMode.TEXT_AND_COMPASS
              : PoiHudIndicatorMode.ONLY_TEXT;
   }

   public static String getPoiTrackedId() {
      if (poiTrackedId == null) return "";
      return poiTrackedId;
   }

   public static void setPoiTrackedId(String id) {
      poiTrackedId = id == null ? "" : id;
      HANDLER.save();
   }

   public static boolean isAutoJoinLifestealOnHub() {
      Boolean forcedState = FeatureFlagController.getForcedState("autoJoinLifestealOnHub");
      if (forcedState != null) {
         return forcedState;
      }
      return autoJoinLifestealOnHub;
   }

   public static void setAutoJoinLifestealOnHub(boolean enabled) {
      autoJoinLifestealOnHub = enabled;
      HANDLER.save();
   }

   public static boolean isCustomBaltopInterfaceEnabled() {
      return customBaltopInterfaceEnabled;
   }

   public static void setCustomBaltopInterfaceEnabled(boolean enabled) {
      customBaltopInterfaceEnabled = enabled;
      HANDLER.save();
   }

   public static void load() {
      FeatureFlagController.ensureLoaded();
      HANDLER.load();
      ensurePoiHudIndicatorMode();
   }

   public static YetAnotherConfigLib getConfig() {
      FeatureFlagController.ensureLoaded();
      return YetAnotherConfigLib.createBuilder()
              .title(Component.translatable("lsu.name"))
              .category(ConfigCategory.createBuilder()
                      .name(Component.translatable("lsu.category.timersCounters"))
                      .group(buildTimerOptions())
                      .group(buildChainCounterOptions())
                      .group(buildHeavenlyDurabilityCalculatorOptions())
                      .build()
              )
              .category(ConfigCategory.createBuilder()
                      .name(Component.translatable("lsu.category.alliances"))
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.allianceSettings"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.enableAlliances.name"))
                                      .description(descriptionWithRemoteReasoning(
                                              "Enables alliance features such as colored name tags.",
                                              "enableAlliances"
                                      ))
                                      .binding(true, Config::getEnableAlliances, Config::setEnableAlliances)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("enableAlliances"))
                                      .build()
                              )
                              .option(Option.<Color>createBuilder()
                                      .name(Component.translatable("lsu.option.allianceNameColor.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Pick the name color for allied players."
                                              ))
                                              .build())
                                      .binding(Config.getAllianceNameColorValue(), Config::getAllianceNameColorValue, Config::setAllianceNameColorValue)
                                      .controller(ColorControllerBuilder::create)
                                      .build()
                              )
                              .build()
                      )
                      .build()
              )
              .category(ConfigCategory.createBuilder()
                      .name(Component.translatable("lsu.category.qol"))
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.customUis"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.customBaltopInterface.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(Component.translatable("lsu.option.customBaltopInterface.description"))
                                              .build())
                                      .binding(true, Config::isCustomBaltopInterfaceEnabled, Config::setCustomBaltopInterfaceEnabled)
                                      .controller(TickBoxControllerBuilder::create)
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.autoJoin"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.autoJoinLifesteal.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Automatically joins the Lifesteal gamemode on lifesteal.net when you join the main hub.\n\nExecutes /joinlifesteal after a second of joining the hub."
                                              ))
                                              .build())
                                      .binding(false, Config::isAutoJoinLifestealOnHub, Config::setAutoJoinLifestealOnHub)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("autoJoinLifestealOnHub"))
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.rareItemScaling"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.rareScaleEnabled.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enable increased scale for rare items such as neth and custom enchants."
                                              ))
                                              .build())
                                      .binding(false, Config::isRareItemScaling, Config::toggleRareItemScaling)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("rareItemScaleEnabled"))
                                      .build()
                              )
                              .option(Option.<Float>createBuilder()
                                      .name(Component.translatable("lsu.option.rareScale.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Increased scale of the rare items."
                                              ))
                                              .build())
                                      .binding(2.0f, Config::getRareItemScaling, Config::setRareItemScaling)
                                      .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                              .range(0.5f, 5.0f)
                                              .step(0.1f)
                                              .valueFormatter(val -> Component.literal(val + "x")))
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.pois"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.poiWaypointsEnabled.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enables the POI directional indicator in your HUD."
                                              ))
                                              .build())
                                      .binding(true, Config::getPoiWaypointsEnabled, Config::setPoiWaypointsEnabled)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("poiWaypointsEnabled"))
                                      .build()
                              )
                              .option(Option.<PoiHudIndicatorMode>createBuilder()
                                      .name(Component.translatable("lsu.option.poiHudIndicatorMode.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Controls how the POI tracker is shown in your HUD.\n\n" +
                                                              "This setting is ignored when POI waypoints are disabled."
                                              ))
                                              .build())
                                      .binding(PoiHudIndicatorMode.TEXT_AND_COMPASS, Config::getPoiHudIndicatorMode, Config::setPoiHudIndicatorMode)
                                      .controller(opt -> EnumControllerBuilder.create(opt)
                                              .enumClass(PoiHudIndicatorMode.class)
                                              .formatValue(mode -> Component.translatable(mode.getTranslationKey())))
                                      .build()
                              )
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.poiAlwaysShowClosest.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Unless you've configured to track a specific POI, the HUD will always track the closest one to you."
                                              ))
                                              .build())
                                      .binding(false, Config::isPoiAlwaysShowClosest, Config::setPoiAlwaysShowClosest)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("poiAlwaysShowClosest"))
                                      .build()
                              )
                              .option(Option.<String>createBuilder()
                                      .name(Component.translatable("lsu.option.poiWaypointFormat.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "The format of the waypoint tracker in your hud\n\nDefault: <gray><bold>{{poi}}</bold>: {{distance}} blocks away"
                                              ))
                                              .build())
                                      .binding(Config.poiWaypointFormat, () -> Config.getPoiWaypointFormat("<gray><bold>{{poi}}</bold>: {{distance}} blocks away"), Config::setPoiWaypointFormat)
                                      .controller(StringControllerBuilder::create)
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.xaeroWaypointsIntegration"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.xaeroPoiWaypointsEnabled.name"))
                                      .description(xaeroPoiWaypointDescription())
                                      .binding(Config.xaeroPoiWaypointsEnabled, Config::isXaeroPoiWaypointsEnabled, Config::setXaeroPoiWaypointsEnabled)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("xaeroPoiWaypointsEnabled") && isXaeroMinimapInstalled())
                                      .build()
                              )
                              .build()
                      )
                      .build()
              )
              .category(ConfigCategory.createBuilder()
                      .name(Component.translatable("lsu.category.funCustomization"))
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.titleScreen"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.quickJoinButtonEnabled.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enables a Quick Join button on the title screen that connects you to lifesteal.net automatically."
                                              ))
                                              .build())
                                      .binding(true, Config::getQuickJoinButtonEnabled, Config::setQuickJoinButtonEnabled)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("quickJoinButtonEnabled"))
                                      .build()
                              )
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.customSplashes.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enables custom LSN-related splash texts in the main menu. Submit new ones at https://discord.gg/qmWYNtRzEg :)"
                                              ))
                                              .build())
                                      .binding(true, Config::getCustomSplashes, Config::setCustomSplashes)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("customSplashes"))
                                      .build()
                              )
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.lobbyPanorama.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enables a panorama of the Lifesteal 2.0 lobby on the title screen."
                                              ))
                                              .build())
                                      .binding(true, Config::getCustomPanoramaEnabled, Config::setCustomPanoramaEnabled)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("customPanoramaEnabled"))
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.simplifications"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.disableChatTags.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Disables chat tags, such as [No-Life] from appearing in messages for visual simplicity."
                                              ))
                                              .build())
                                      .binding(false, Config::getDisableChatTags, Config::setDisableChatTags)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("disableChatTags"))
                                      .build()
                              )
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.removeUniquePlusColor.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Removes the unique coloring of the plus in LSN+ for visual simplicity.\n\nExample:\n<dark_gray>[</dark_gray><bold><#FF7200>HEROIC</#FF7200></bold><green>+</green><dark_gray>]</dark_gray> becomes <dark_gray>[</dark_gray><bold><#FF7200>HEROIC+</#FF7200></bold><dark_gray>]</dark_gray>"
                                              ))
                                              .build())
                                      .binding(false, Config::getRemoveUniquePlusColor, Config::setRemoveUniquePlusColor)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("removeUniquePlusColor"))
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.messageCustomization"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.pmFormatEnabled.name"))
                                      .binding(false, Config::getEnablePmFormat, Config::setEnablePmFormat)
                                      .controller(TickBoxControllerBuilder::create)
                                      .available(FeatureFlagController.isFeatureAvailable("enablePmFormat"))
                                      .build()
                              )
                              .option(Option.<String>createBuilder()
                                      .name(Component.translatable("lsu.option.pmFormat.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Changes the format of in-game direct messages.\n\n" +
                                                              "Default: <light_purple><bold>{{direction}}</bold> {{sender}}</light_purple> <white>➡ {{message}}</white>\n"
                                              ))
                                              .build())
                                      .binding(Config.pmFormat, () -> Config.pmFormat, Config::setPmFormat)
                                      .controller(StringControllerBuilder::create)
                                      .build()
                              )
                              /*
                              .option(Option.<Boolean>createBuilder()
                                 .name(Component.translatable("lsu.option.claimChatFormatEnabled.name"))
                                 .binding(false, Config::getEnableClaimChatFormat, Config::setEnableClaimChatFormat)
                                 .controller(TickBoxControllerBuilder::create)
                                 .build()
                              )
                              .option(Option.<String>createBuilder()
                                 .name(Component.translatable("lsu.option.claimChatFormat.name"))
                                 .description(OptionDescription.createBuilder()
                                    .text(MessagingUtils.miniMessage(
                                       "Changes the format of claim chat messages.\n\n" +
                                          "Default: <gold><bold>{{claim}}</bold></gold> <dark_gray>|</dark_gray> <aqua>{{username}}</aqua><gray>:</gray> <white>{{message}}</white>\n"
                                    ))
                                    .build())
                                 .binding(Config.claimChatFormat, Config::getClaimChatFormat, Config::setClaimChatFormat)
                                 .controller(StringControllerBuilder::create)
                                 .build()
                              )
                              */
                              .build()
                      )
                      .build()
              ).build();
   }

   public static Screen getConfigScreen(Screen parent) {
      return getConfig().generateScreen(parent);
   }

}