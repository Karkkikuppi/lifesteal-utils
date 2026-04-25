package dev.candycup.lifestealutils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.candycup.lifestealutils.api.PersistentKnowledgeController;
import dev.candycup.lifestealutils.api.observers.ScoreboardObserver;
import dev.candycup.lifestealutils.api.observers.TablistObserver;
import dev.candycup.lifestealutils.config.ConfigContainerRegistry;
import dev.candycup.lifestealutils.config.ConfigDescriptorRegistry;
import dev.candycup.lifestealutils.config.ConfigResolver;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.features.afk.AfkMode;
import dev.candycup.lifestealutils.features.alliances.AllianceCommandController;
import dev.candycup.lifestealutils.features.alliances.AllianceNameDecorator;
import dev.candycup.lifestealutils.features.alliances.AllianceProfileCacheManager;
import dev.candycup.lifestealutils.features.alliances.AllianceService;
import dev.candycup.lifestealutils.features.alliances.AllianceSyncManager;
import dev.candycup.lifestealutils.features.baltop.BaltopScrapeCoordinator;
import dev.candycup.lifestealutils.features.combat.HeavenlyDurabilityCalculator;
import dev.candycup.lifestealutils.features.gaia.GaiaConnectionToastListener;
import dev.candycup.lifestealutils.features.items.RareItemHighlight;
import dev.candycup.lifestealutils.features.messages.ChatTagRemover;
import dev.candycup.lifestealutils.features.messages.GhostedChatMessageFilter;
import dev.candycup.lifestealutils.features.messages.PrivateMessageFormatter;
import dev.candycup.lifestealutils.features.prestige.PrestigeMenuListener;
import dev.candycup.lifestealutils.features.qol.AutoJoinLifesteal;
import dev.candycup.lifestealutils.features.titlescreen.CustomSplashes;
import dev.candycup.lifestealutils.features.titlescreen.QuickJoinButton;
import dev.candycup.lifestealutils.gaia.GaiaConsentController;
import dev.candycup.lifestealutils.gaia.GaiaConsentScreen;
import dev.candycup.lifestealutils.gaia.GaiaAuthClient;
import dev.candycup.lifestealutils.gaia.gateway.GaiaGatewayClient;
import dev.candycup.lifestealutils.hud.HudDisplayLayer;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
import dev.candycup.lifestealutils.features.timers.BasicTimerManager;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.candycup.lifestealutils.ui.HudElementEditor;
import dev.candycup.lifestealutils.ui.RadarScreen;
import dev.candycup.lifestealutils.ui.AllianceListScreen;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class LifestealUtils implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   private static final int DEFAULT_MESSAGE_COLOR = 0xFFFFFF;
   private static final Map<String, Integer> CONFIG_CATEGORY_WEIGHTS = new HashMap<>();

   static {
      CONFIG_CATEGORY_WEIGHTS.put("timers", 0);
      CONFIG_CATEGORY_WEIGHTS.put("alliances", 10);
      CONFIG_CATEGORY_WEIGHTS.put("customization", 20);
      CONFIG_CATEGORY_WEIGHTS.put("qol", 30);
      CONFIG_CATEGORY_WEIGHTS.put("experiments", 40);
   }

   //? if >1.21.8
   private static KeyMapping.Category LIFESTEAL_UTIL_BINDS;
   private static KeyMapping openHudEditorKeyBinding;
   private static int pendingConfigOpenTicks = -1;
   private static int pendingGaiaConsentOpenTicks = -1;
   private static int pendingHudEditorOpenTicks = -1;
   private static int pendingRadarOpenTicks = -1;
   private static int pendingAlliancesScreenOpenTicks = -1;
   private static long lastAllianceSyncAttemptMs = 0L;

   private static UnbrokenChainTracker unbrokenChainTracker;
   private static HeavenlyDurabilityCalculator heavenlyDurabilityCalculator;
   @Getter
   private static BasicTimerManager basicTimerManager;
   private static PrivateMessageFormatter privateMessageFormatter;
   private static ChatTagRemover chatTagRemover;
   private static GhostedChatMessageFilter ghostedChatMessageFilter;
   private static RareItemHighlight rareItemHighlight;
   private static QuickJoinButton quickJoinButton;
   private static CustomSplashes customSplashes;
   private static AutoJoinLifesteal autoJoinLifesteal;
   private static GaiaConnectionToastListener gaiaConnectionToastListener;
   private static AllianceNameDecorator allianceNameDecorator;
   private static PrestigeMenuListener prestigeMenuListener;
   @Getter
   private static GaiaGatewayClient gaiaGatewayClient;

   @Override
   public void onInitializeClient() {
      LOGGER.info("Lifesteal Utils initializing. I LOVE FABRIC !!!!!!");
      ConfigContainerRegistry.initializeGeneratedIndex();
      ConfigDescriptorRegistry.registerDefaultProviders();
      Config.load();
      PersistentKnowledgeController.initialize();
      FeatureFlagController.assertNoIncompatibleModsDetected();
      GaiaConsentController.initialize();
      initializeGaiaIfAuthorized();

      HudElementManager.init();

      registerListeners();
      registerFeatures();
      registerHudElements();
      registerKeybinds();
      registerCommands();
   }

   public static void registerListeners() {
      new TablistObserver();
      new ScoreboardObserver();
   }

   public static int getConfigCategoryWeight(String category) {
      if (category == null) {
         return Integer.MAX_VALUE;
      }
      return CONFIG_CATEGORY_WEIGHTS.getOrDefault(category.toLowerCase(Locale.ROOT), Integer.MAX_VALUE);
   }

   public static void initializeGaiaIfAuthorized() {
      if (Config.isGaiaAdvancedFeaturesEnabled()) {
         GaiaAuthClient.confirmHandshakeOnStartup(
                 Minecraft.getInstance().getUser().getName(),
                 Minecraft.getInstance().getUser().getProfileId()
         ).thenAccept(success -> {
            if (success) {
               LOGGER.info("Gaia authentication completed successfully");
            } else {
               LOGGER.warn("Gaia authentication failed");
            }
         });
      }
   }

   public static void registerFeatures() {
      AllianceProfileCacheManager.initialize();
      AllianceService.initialize();

      basicTimerManager = new BasicTimerManager(FeatureFlagController.getBasicTimers());
      for (HudElementDefinition definition : basicTimerManager.getHudDefinitions()) {
         HudElementManager.register(definition);
      }

      unbrokenChainTracker = new UnbrokenChainTracker();
      HudElementManager.register(unbrokenChainTracker.getHudDefinition());

      heavenlyDurabilityCalculator = new HeavenlyDurabilityCalculator();
      HudElementManager.register(heavenlyDurabilityCalculator.getHudDefinition());

      privateMessageFormatter = new PrivateMessageFormatter();

      chatTagRemover = new ChatTagRemover();

      ghostedChatMessageFilter = new GhostedChatMessageFilter();

      rareItemHighlight = new RareItemHighlight();

      quickJoinButton = new QuickJoinButton();

      customSplashes = new CustomSplashes();

      autoJoinLifesteal = new AutoJoinLifesteal();

      // gaia gateway websocket client
      gaiaGatewayClient = new GaiaGatewayClient();

      gaiaConnectionToastListener = new GaiaConnectionToastListener();

      allianceNameDecorator = new AllianceNameDecorator();

      prestigeMenuListener = new PrestigeMenuListener();
   }

   public static void registerHudElements() {
      HudElementRegistry.attachElementAfter(
              VanillaHudElements.CHAT,
              HudDisplayLayer.LSU_HUD_LAYER_ID,
              HudDisplayLayer.lsuHudLayer()
      );

      HudElementRegistry.attachElementAfter(
              VanillaHudElements.CHAT,
              HudElementEditor.EDITOR_LAYER_ID,
              HudElementEditor.editorLayer()
      );
   }

   public static void openAllianceMenu() {
      Minecraft client = Minecraft.getInstance();
      client.execute(() -> pendingAlliancesScreenOpenTicks = 1);
   }

   public static void registerCommands() {
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
         var alliancesCommand = ClientCommandManager.literal("alliances")
                 .executes(commandContext -> {
                    openAllianceMenu();
                    return 1;
                 });
         var alliancesRootCommand = alliancesCommand.build();
         alliancesCommand
                 .then(ClientCommandManager.literal("view").redirect(alliancesRootCommand))
                 .then(ClientCommandManager.literal("list").redirect(alliancesRootCommand))
                 .then(ClientCommandManager.literal("manage").redirect(alliancesRootCommand))
                 .then(ClientCommandManager.literal("edit").redirect(alliancesRootCommand))
                 .then(ClientCommandManager.literal("add")
                         .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                 .suggests((context, builder) ->
                                         AllianceCommandController.suggestOnlinePlayers(builder.getRemainingLowerCase(), builder)
                                 )
                                 .then(ClientCommandManager.argument("alliance_and_list", StringArgumentType.greedyString())
                                         .suggests((context, builder) ->
                                                 AllianceCommandController.suggestAllianceAndListTargets(builder.getRemainingLowerCase(), builder)
                                         )
                                         .executes(commandContext -> {
                                            String username = StringArgumentType.getString(commandContext, "username");
                                            String allianceAndMaybeList = StringArgumentType.getString(commandContext, "alliance_and_list");
                                            return AllianceCommandController.addMemberToAllianceParsed(username, allianceAndMaybeList);
                                         })))
                 )
                 .then(ClientCommandManager.literal("remove")
                         .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                 .suggests((context, builder) ->
                                         AllianceCommandController.suggestOnlinePlayers(builder.getRemainingLowerCase(), builder)
                                 )
                                 .then(ClientCommandManager.argument("alliance_and_list", StringArgumentType.greedyString())
                                         .suggests((context, builder) ->
                                                 AllianceCommandController.suggestAllianceAndListTargets(builder.getRemainingLowerCase(), builder)
                                         )
                                         .executes(commandContext -> {
                                            String username = StringArgumentType.getString(commandContext, "username");
                                            String allianceAndMaybeList = StringArgumentType.getString(commandContext, "alliance_and_list");
                                            return AllianceCommandController.removeMemberFromAllianceParsed(username, allianceAndMaybeList);
                                         }))))
                 .then(ClientCommandManager.literal("create")
                         .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                 .executes(commandContext -> {
                                    String name = StringArgumentType.getString(commandContext, "name");
                                    return AllianceCommandController.createAlliance(name);
                                 })))
                 .then(ClientCommandManager.literal("subscribe")
                         .then(ClientCommandManager.argument("id", StringArgumentType.greedyString())
                                 .executes(commandContext -> {
                                    String id = StringArgumentType.getString(commandContext, "id");
                                    return AllianceCommandController.subscribeToAlliance(id);
                                 })))
                 .then(ClientCommandManager.literal("unsubscribe")
                         .then(ClientCommandManager.argument("name_or_id", StringArgumentType.greedyString())
                                 .suggests((context, builder) ->
                                         AllianceCommandController.suggestSubscribedAllianceNames(builder.getRemainingLowerCase(), builder)
                                 )
                                 .executes(commandContext -> {
                                    String nameOrId = StringArgumentType.getString(commandContext, "name_or_id");
                                    return AllianceCommandController.unsubscribeFromAlliance(nameOrId);
                                 })));

         dispatcher.register(
                 ClientCommandManager.literal("lsu")
                         .executes(commandContext -> {
                            Minecraft client = Minecraft.getInstance();
                            client.execute(() -> pendingConfigOpenTicks = 2);
                            return 1;
                         })
                         .then(ClientCommandManager.literal("config")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> pendingConfigOpenTicks = 2);
                                    return 1;
                                 }))
                         .then(ClientCommandManager.literal("consent-gaia")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> pendingGaiaConsentOpenTicks = 2);
                                    return 1;
                                 }))
                         .then(ClientCommandManager.literal("edit-hud")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> pendingHudEditorOpenTicks = 1);
                                    return 1;
                                 }))
                         .then(alliancesCommand)
                         .then(ClientCommandManager.literal("toggle-afk")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> {
                                       boolean enabled = AfkMode.toggle();
                                       String translationKey = enabled ? "lsu.command.toggle_afk.enabled" : "lsu.command.toggle_afk.disabled";
                                       MessagingUtils.showMessage(Component.translatable(translationKey), DEFAULT_MESSAGE_COLOR);
                                    });
                                    return 1;
                                 }))
                         .then(ClientCommandManager.literal("baltop")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> BaltopScrapeCoordinator.handleBaltopCommand(client));
                                    return 1;
                                 }))
                         .then(ClientCommandManager.literal("utilities")
                                 .then(ClientCommandManager.literal("copy-client-info-to-clipboard")
                                         .executes(commandContext -> {
                                            Minecraft client = Minecraft.getInstance();
                                            boolean copied = DebugInformationController.copyBasicInfoToClipboard(client);
                                            if (copied) {
                                               MessagingUtils.showMiniMessage("<green>Copied basic info to clipboard.</green>");
                                               return 1;
                                            }
                                            MessagingUtils.showMiniMessage("<red>Player not available.</red>");
                                            return 0;
                                         }))
                                 .then(ClientCommandManager.literal("take-panorama-screenshot")
                                         .executes(commandContext -> {
                                            Minecraft client = Minecraft.getInstance();

                                            final File GAME_DIR = new File(FabricLoader.getInstance().getGameDir().toString());

                                            client.execute(() -> {
                                               client.grabPanoramixScreenshot(
                                                       GAME_DIR
                                               );

                                               if (client.player != null) {
                                                  client.player.sendMessage(
                                                          MiniMessage.miniMessage().deserialize(
                                                                  "<gray><italic>[Lifesteal Utils] snip snap! panorama taken! open your screenshots folder to see it!"
                                                          )
                                                  );
                                               }
                                            });
                                            return 1;
                                         }))));
      });
   }

   /**
    * Queues the custom baltop interface to open once no other screen is active.
    */
   public static void queueBaltopScrape() {
      BaltopScrapeCoordinator.queueScrape();
   }

   private static void registerKeybinds() {
      //? if >1.21.8 {
      LIFESTEAL_UTIL_BINDS = KeyMapping.Category.register(
              Identifier.fromNamespaceAndPath("lifestealutils", "lifesteal_utils")
      );

      openHudEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
              "key.lifesteal-utils.open_hud_editor",
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_H,
              LIFESTEAL_UTIL_BINDS
      ));
      //?} else {
      /*openHudEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
              "key.lifesteal-utils.open_hud_editor",
              GLFW.GLFW_KEY_H,
              "category.lifesteal-utils.lifesteal_utils"
      ));
      *///?}

      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         LifestealUtilsEvents.CLIENT_TICK.invoker().onClientTick(new ClientTickEvent(client));

         // tick gateway client for keep-alive pings
         if (gaiaGatewayClient != null) {
            gaiaGatewayClient.tick();
         }

         if (client.player == null) return;
         pendingConfigOpenTicks = handleScheduledScreenOpen(
                 client,
                 pendingConfigOpenTicks,
                 false,
                 () -> ConfigResolver.resolveScreen(client.screen)
         );
         pendingGaiaConsentOpenTicks = handleScheduledScreenOpen(
                 client,
                 pendingGaiaConsentOpenTicks,
                 true,
                 () -> new GaiaConsentScreen(null)
         );
         pendingHudEditorOpenTicks = handleScheduledScreenOpen(
                 client,
                 pendingHudEditorOpenTicks,
                 true,
                 () -> new HudElementEditor(Component.translatable("lsu.screen.hudEditor"))
         );
         pendingRadarOpenTicks = handleScheduledScreenOpen(client, pendingRadarOpenTicks, true, RadarScreen::new);
         pendingAlliancesScreenOpenTicks = handleScheduledScreenOpen(
                 client,
                 pendingAlliancesScreenOpenTicks,
                 true,
                 () -> new AllianceListScreen(client.screen)
         );
         BaltopScrapeCoordinator.tick(client);

         if (gaiaGatewayClient != null && gaiaGatewayClient.isConnected()) {
            long now = System.currentTimeMillis();
            if (now - lastAllianceSyncAttemptMs >= 60_000L) {
               lastAllianceSyncAttemptMs = now;
               AllianceSyncManager.syncSubscriptionsAsync();
            }
         }

         if (openHudEditorKeyBinding.consumeClick()) {
            if (client.screen != null) return;
            pendingHudEditorOpenTicks = 1;
         }
      });
   }

   private static int handleScheduledScreenOpen(Minecraft client, int pendingTicks, boolean requireNoScreen, Supplier<Screen> screenSupplier) {
      if (pendingTicks < 0) {
         return pendingTicks;
      }
      if (pendingTicks > 0) {
         return pendingTicks - 1;
      }
      if (!requireNoScreen || client.screen == null) {
         client.setScreen(screenSupplier.get());
      }
      return -1;
   }

}
