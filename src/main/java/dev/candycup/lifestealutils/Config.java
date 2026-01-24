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

   private static OptionDescription descriptionWithRemoteReasoning(String baseMiniMessage, String featureKey) {
      OptionDescription.Builder builder = OptionDescription.createBuilder()
              .text(MessagingUtils.miniMessage(baseMiniMessage));
      String reasoning = FeatureFlagController.getReasoning(featureKey);
      if (reasoning != null && !reasoning.isBlank()) {
         builder.text(MessagingUtils.miniMessage(reasoning));
      }
      return builder.build();
   }

   private static OptionGroup buildTimerOptions() {
      OptionGroup.Builder group = OptionGroup.createBuilder()
              .name(Component.literal("Timers"));

      List<BasicTimerManager.TimerEntry> timers = BasicTimerManager.timerEntries();

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
              .name(Component.literal("Unbroken Chain Counter"))
              .option(Option.<Boolean>createBuilder()
                      .name(Component.literal("Enable Chain Counter"))
                      .description(OptionDescription.createBuilder()
                              .text(MessagingUtils.miniMessage(
                                      "Tracks consecutive hits without receiving damage.\n\n" +
                                              "Each consecutive hit grants +5% bonus damage, capping at 50%.\n" +
                                              "Bonus only applies after 2 consecutive hits."
                              ))
                              .build())
                      .binding(false, Config::isChainCounterEnabled, Config::setChainCounterEnabled)
                      .controller(TickBoxControllerBuilder::create)
                      .build())
              .option(Option.<String>createBuilder()
                      .name(Component.literal("Chain Counter Format"))
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
      return Config.enablePmFormat;
   }

   public static void setEnablePmFormat(boolean enable) {
      Config.enablePmFormat = enable;
      HANDLER.save();
   }

   public static boolean getQuickJoinButtonEnabled() {
      return Config.quickJoinButtonEnabled;
   }

   public static void setQuickJoinButtonEnabled(boolean enabled) {
      Config.quickJoinButtonEnabled = enabled;
      HANDLER.save();
   }

   public static boolean getDisableChatTags() {
      return Config.disableChatTags;
   }

   public static void setDisableChatTags(boolean enabled) {
      Config.disableChatTags = enabled;
      HANDLER.save();
   }

   public static boolean getRemoveUniquePlusColor() {
      return Config.removeUniquePlusColor;
   }

   public static void setRemoveUniquePlusColor(boolean enabled) {
      Config.removeUniquePlusColor = enabled;
      HANDLER.save();
   }

   public static boolean getEnableAlliances() {
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
      return Config.customSplashes;
   }

   public static void setCustomSplashes(boolean enabled) {
      Config.customSplashes = enabled;
      HANDLER.save();
   }

   public static boolean isRareItemScaling() {
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
      return chainCounterEnabled;
   }

   public static void setChainCounterEnabled(boolean enabled) {
      chainCounterEnabled = enabled;
      HANDLER.save();
   }

   public static void ensureChainCounterKnown() {
      // no-op, field has default value
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

   public static void load() {
      FeatureFlagController.ensureLoaded();
      HANDLER.load();
   }

   public static YetAnotherConfigLib getConfig() {
      FeatureFlagController.ensureLoaded();
      return YetAnotherConfigLib.createBuilder()
              .title(Component.translatable("lsu.name"))
              .category(ConfigCategory.createBuilder()
                      .name(Component.translatable("lsu.category.main"))
                      .group(buildTimerOptions())
                      .group(buildChainCounterOptions())
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.alliances"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.enableAlliances.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enables alliance features such as colored name tags."
                                              ))
                                              .build())
                                      .binding(true, Config::getEnableAlliances, Config::setEnableAlliances)
                                      .controller(TickBoxControllerBuilder::create)
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
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.messageCustomization"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.pmFormatEnabled.name"))
                                      .binding(false, Config::getEnablePmFormat, Config::setEnablePmFormat)
                                      .controller(TickBoxControllerBuilder::create)
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
                                      .build()
                              )
                              .build()
                      )
                      .group(OptionGroup.createBuilder()
                              .name(Component.translatable("lsu.group.rareScale"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.rareScaleEnabled.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Enable increased scale for rare items such as neth and custom enchants."
                                              ))
                                              .build())
                                      .binding(false, Config::isRareItemScaling, Config::toggleRareItemScaling)
                                      .controller(TickBoxControllerBuilder::create)
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
                              .name(Component.translatable("lsu.group.simplications"))
                              .option(Option.<Boolean>createBuilder()
                                      .name(Component.translatable("lsu.option.disableChatTags.name"))
                                      .description(OptionDescription.createBuilder()
                                              .text(MessagingUtils.miniMessage(
                                                      "Disables chat tags, such as [No-Life] from appearing in messages for visual simplicity."
                                              ))
                                              .build())
                                      .binding(false, Config::getDisableChatTags, Config::setDisableChatTags)
                                      .controller(TickBoxControllerBuilder::create)
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
                                      .build()
                              )
                              .build()
                      ).build()
              ).build();
   }

   public static Screen getConfigScreen(Screen parent) {
      return getConfig().generateScreen(parent);
   }

}