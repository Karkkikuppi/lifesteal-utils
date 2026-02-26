package dev.candycup.lifestealutils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.candycup.lifestealutils.api.observers.ScoreboardObserver;
import dev.candycup.lifestealutils.api.observers.TablistObserver;
import dev.candycup.lifestealutils.config.ConfigContainerRegistry;
import dev.candycup.lifestealutils.config.ConfigDescriptorRegistry;
import dev.candycup.lifestealutils.config.ConfigResolver;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ClientTickEvent;
import dev.candycup.lifestealutils.features.alliances.service.AllianceSelectionController;
import dev.candycup.lifestealutils.features.alliances.service.AllianceTargetSelectionHandler;
import dev.candycup.lifestealutils.features.alliances.ui.AlliancesListScreen;
import dev.candycup.lifestealutils.features.alliances.AllianceMotdListener;
import dev.candycup.lifestealutils.features.alliances.AllianceNameRenderHandler;
import dev.candycup.lifestealutils.features.afk.AfkMode;
import dev.candycup.lifestealutils.features.baltop.BaltopScrapeCoordinator;
import dev.candycup.lifestealutils.features.combat.HeavenlyDurabilityCalculator;
import dev.candycup.lifestealutils.features.gaia.GaiaConnectionToastListener;
import dev.candycup.lifestealutils.features.items.RareItemHighlight;
import dev.candycup.lifestealutils.features.messages.ChatTagRemover;
import dev.candycup.lifestealutils.features.messages.GhostedChatMessageFilter;
import dev.candycup.lifestealutils.features.messages.PrivateMessageFormatter;
import dev.candycup.lifestealutils.features.messages.RankPlusColorNormalizer;
import dev.candycup.lifestealutils.features.qol.AutoJoinLifesteal;
import dev.candycup.lifestealutils.features.qol.PoiTrackingController;
import dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator;
import dev.candycup.lifestealutils.features.qol.PoiWaypointTracker;
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
import dev.candycup.lifestealutils.integrations.xaero.XaeroPoiWaypointIntegration;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.candycup.lifestealutils.ui.HudElementEditor;
import dev.candycup.lifestealutils.ui.RadarScreen;
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
import java.util.function.Supplier;

public final class LifestealUtils implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   private static final int DEFAULT_MESSAGE_COLOR = 0xFFFFFF;
   //? if >1.21.8
   private static KeyMapping.Category LIFESTEAL_UTIL_BINDS;
   private static KeyMapping openHudEditorKeyBinding;
   private static KeyMapping addAllianceTargetKeyBinding;
   private static int pendingConfigOpenTicks = -1;
   private static int pendingGaiaConsentOpenTicks = -1;
   private static int pendingHudEditorOpenTicks = -1;
   private static int pendingRadarOpenTicks = -1;
   private static int pendingAlliancesScreenOpenTicks = -1;

   private static UnbrokenChainTracker unbrokenChainTracker;
   private static HeavenlyDurabilityCalculator heavenlyDurabilityCalculator;
   @Getter
   private static BasicTimerManager basicTimerManager;
   private static PrivateMessageFormatter privateMessageFormatter;
   private static ChatTagRemover chatTagRemover;
   private static RankPlusColorNormalizer rankPlusColorNormalizer;
   private static GhostedChatMessageFilter ghostedChatMessageFilter;
   private static AllianceMotdListener allianceMotdListener;
   private static AllianceNameRenderHandler allianceNameRenderHandler;
   private static RareItemHighlight rareItemHighlight;
   private static QuickJoinButton quickJoinButton;
   private static CustomSplashes customSplashes;
   private static AutoJoinLifesteal autoJoinLifesteal;
   private static GaiaConnectionToastListener gaiaConnectionToastListener;
   @Getter
   private static GaiaGatewayClient gaiaGatewayClient;

   @Override
   public void onInitializeClient() {
      LOGGER.info("Lifesteal Utils initializing. I LOVE FABRIC !!!!!!");
      ConfigContainerRegistry.initializeGeneratedIndex();
      ConfigDescriptorRegistry.registerDefaultProviders();
      Config.load();
      GaiaConsentController.initialize();
      initializeGaiaIfAuthorized();

      HudElementManager.init();

      registerListeners();
      registerFeatures();
      registerIntegrations();
      registerHudElements();
      registerKeybinds();
      registerCommands();
   }

   public static void registerListeners() {
      new TablistObserver();
      new ScoreboardObserver();
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

      rankPlusColorNormalizer = new RankPlusColorNormalizer();

      ghostedChatMessageFilter = new GhostedChatMessageFilter();

      allianceMotdListener = new AllianceMotdListener();

      allianceNameRenderHandler = new AllianceNameRenderHandler();

      rareItemHighlight = new RareItemHighlight();

      quickJoinButton = new QuickJoinButton();

      customSplashes = new CustomSplashes();

      autoJoinLifesteal = new AutoJoinLifesteal();

      // gaia gateway websocket client
      gaiaGatewayClient = new GaiaGatewayClient();

      gaiaConnectionToastListener = new GaiaConnectionToastListener();
   }

   public static void registerHudElements() {
      // poi waypoint tracker
      PoiWaypointTracker poiWaypointTracker = new PoiWaypointTracker();
      HudElementManager.register(poiWaypointTracker.getHudDefinition());

      // poi directional indicator (renders with the waypoint tracker)
      PoiDirectionalIndicator poiDirectionalIndicator =
              new PoiDirectionalIndicator(poiWaypointTracker);
      HudDisplayLayer.setPoiDirectionalIndicator(poiDirectionalIndicator);
      HudElementEditor.setPoiDirectionalIndicator(poiDirectionalIndicator);

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

   public static void registerIntegrations() {
      if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
         new XaeroPoiWaypointIntegration();
      }
   }

   public static void registerCommands() {
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
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
                         .then(ClientCommandManager.literal("radar")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> pendingRadarOpenTicks = 1);
                                    return 1;
                                 }))
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
                         .then(ClientCommandManager.literal("alliances")
                                 .executes(commandContext -> {
                                    pendingAlliancesScreenOpenTicks = 2;
                                    return 1;
                                 })
                                 .then(ClientCommandManager.literal("select")
                                         .then(ClientCommandManager.argument("allianceName", StringArgumentType.greedyString())
                                                 .suggests((context, builder) -> {
                                                    return AllianceSelectionController.suggestAllianceNames(builder.getRemainingLowerCase(), builder);
                                                 })
                                                 .executes(commandContext -> {
                                                    String rawAllianceName = StringArgumentType.getString(commandContext, "allianceName");
                                                    return AllianceSelectionController.selectAllianceByName(rawAllianceName);
                                                 }))))
                         .then(ClientCommandManager.literal("track-poi")
                                 .then(ClientCommandManager.argument("poi", StringArgumentType.greedyString())
                                         .suggests((context, builder) -> {
                                            return PoiTrackingController.suggestPois(builder.getRemainingLowerCase(), builder);
                                         })
                                         .executes(commandContext -> {
                                            String poiArg = StringArgumentType.getString(commandContext, "poi").trim();
                                            return PoiTrackingController.trackPoiArgument(poiArg);
                                         })))
                         .then(ClientCommandManager.literal("untrack-poi")
                                 .executes(commandContext -> PoiTrackingController.untrackCurrentPoi()))
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
      addAllianceTargetKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
              "key.lifesteal-utils.add_alliance_target",
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_K,
              LIFESTEAL_UTIL_BINDS
      ));
      //?} else {
      /*openHudEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
              "key.lifesteal-utils.open_hud_editor",
              GLFW.GLFW_KEY_H,
              "category.lifesteal-utils.lifesteal_utils"
      ));
      addAllianceTargetKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
              "key.lifesteal-utils.add_alliance_target",
              GLFW.GLFW_KEY_K,
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
                 () -> ConfigResolver.resolve().generateScreen(client.screen)
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
                 () -> new AlliancesListScreen(null)
         );
         BaltopScrapeCoordinator.tick(client);

         if (openHudEditorKeyBinding.consumeClick()) {
            if (client.screen != null) return;
            pendingHudEditorOpenTicks = 1;
         }
         if (addAllianceTargetKeyBinding.consumeClick()) {
            AllianceTargetSelectionHandler.handleKeyClick(client);
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
