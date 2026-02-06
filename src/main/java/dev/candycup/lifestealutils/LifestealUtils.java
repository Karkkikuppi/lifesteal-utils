package dev.candycup.lifestealutils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.candycup.lifestealutils.event.EventBus;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.features.alliances.Alliances;
import dev.candycup.lifestealutils.features.afk.AfkMode;
import dev.candycup.lifestealutils.features.baltop.BaltopScraper;
import dev.candycup.lifestealutils.features.combat.HeavenlyDurabilityCalculator;
import dev.candycup.lifestealutils.features.items.RareItemHighlight;
import dev.candycup.lifestealutils.features.messages.ChatTagRemover;
import dev.candycup.lifestealutils.features.messages.PrivateMessageFormatter;
import dev.candycup.lifestealutils.features.messages.RankPlusColorNormalizer;
import dev.candycup.lifestealutils.features.qol.AutoJoinLifesteal;
import dev.candycup.lifestealutils.features.titlescreen.CustomSplashes;
import dev.candycup.lifestealutils.features.titlescreen.QuickJoinButton;
import dev.candycup.lifestealutils.hud.HudDisplayLayer;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
import dev.candycup.lifestealutils.features.timers.BasicTimerManager;
import dev.candycup.lifestealutils.integrations.xaero.XaeroPoiWaypointIntegration;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.candycup.lifestealutils.ui.HudElementEditor;
import dev.candycup.lifestealutils.ui.RadarScreen;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Objects;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class LifestealUtils implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   private static final int DEFAULT_MESSAGE_COLOR = 0xFFFFFF;
   private static final String TOGGLE_AFK_ENABLED_TRANSLATION_KEY = "lsu.command.toggle_afk.enabled";
   private static final String TOGGLE_AFK_DISABLED_TRANSLATION_KEY = "lsu.command.toggle_afk.disabled";
   //? if >1.21.8
   private static KeyMapping.Category LIFESTEAL_UTIL_BINDS;
   private static KeyMapping openHudEditorKeyBinding;
   private static KeyMapping addAllianceTargetKeyBinding;
   private static int pendingConfigOpenTicks = -1;
   private static int pendingHudEditorOpenTicks = -1;
   private static int pendingRadarOpenTicks = -1;
   private static boolean pendingBaltopScrape = false;

   private static UnbrokenChainTracker unbrokenChainTracker;
   private static HeavenlyDurabilityCalculator heavenlyDurabilityCalculator;
   private static BasicTimerManager basicTimerManager;
   private static PrivateMessageFormatter privateMessageFormatter;
   private static ChatTagRemover chatTagRemover;
   private static RankPlusColorNormalizer rankPlusColorNormalizer;
   private static Alliances alliances;
   private static RareItemHighlight rareItemHighlight;
   private static QuickJoinButton quickJoinButton;
   private static CustomSplashes customSplashes;
   private static AutoJoinLifesteal autoJoinLifesteal;

   @Override
   public void onInitializeClient() {
      LOGGER.info("Lifesteal Utils initializing. I LOVE FABRIC !!!!!!");
      Config.load();

      HudElementManager.init();

      basicTimerManager = new BasicTimerManager(FeatureFlagController.getBasicTimers());
      EventBus.getInstance().register(basicTimerManager);
      for (HudElementDefinition definition : basicTimerManager.getHudDefinitions()) {
         HudElementManager.register(definition);
      }

      unbrokenChainTracker = new UnbrokenChainTracker();
      EventBus.getInstance().register(unbrokenChainTracker);
      HudElementManager.register(unbrokenChainTracker.getHudDefinition());

      heavenlyDurabilityCalculator = new HeavenlyDurabilityCalculator();
      HudElementManager.register(heavenlyDurabilityCalculator.getHudDefinition());

      privateMessageFormatter = new PrivateMessageFormatter();
      EventBus.getInstance().register(privateMessageFormatter);

      chatTagRemover = new ChatTagRemover();
      EventBus.getInstance().register(chatTagRemover);

      rankPlusColorNormalizer = new RankPlusColorNormalizer();
      EventBus.getInstance().register(rankPlusColorNormalizer);

      alliances = new Alliances();
      EventBus.getInstance().register(alliances);

      rareItemHighlight = new RareItemHighlight();
      EventBus.getInstance().register(rareItemHighlight);

      quickJoinButton = new QuickJoinButton();
      EventBus.getInstance().register(quickJoinButton);

      customSplashes = new CustomSplashes();
      EventBus.getInstance().register(customSplashes);

      autoJoinLifesteal = new AutoJoinLifesteal();
      EventBus.getInstance().register(autoJoinLifesteal);

      // poi waypoint tracker
      dev.candycup.lifestealutils.features.qol.PoiWaypointTracker poiWaypointTracker = new dev.candycup.lifestealutils.features.qol.PoiWaypointTracker();
      EventBus.getInstance().register(poiWaypointTracker);
      HudElementManager.register(poiWaypointTracker.getHudDefinition());

      if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
         XaeroPoiWaypointIntegration xaeroPoiWaypointIntegration = new XaeroPoiWaypointIntegration();
         EventBus.getInstance().register(xaeroPoiWaypointIntegration);
      }

      // poi directional indicator (renders with the waypoint tracker)
      dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator poiDirectionalIndicator =
              new dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator(poiWaypointTracker);
      HudDisplayLayer.setPoiDirectionalIndicator(poiDirectionalIndicator);
      dev.candycup.lifestealutils.ui.HudElementEditor.setPoiDirectionalIndicator(poiDirectionalIndicator);

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
         EventBus.getInstance().post(new ClientTickEvent(client));

         if (client.player == null) return;
         if (pendingConfigOpenTicks >= 0) {
            if (pendingConfigOpenTicks == 0) {
               client.setScreen(Config.getConfigScreen(client.screen));
               pendingConfigOpenTicks = -1;
            } else {
               pendingConfigOpenTicks--;
            }
         }
         if (pendingHudEditorOpenTicks >= 0) {
            if (pendingHudEditorOpenTicks == 0) {
               if (client.screen == null) {
                  client.setScreen(new HudElementEditor(
                          Component.translatable("lsu.screen.hudEditor")
                  ));
               }
               pendingHudEditorOpenTicks = -1;
            } else {
               pendingHudEditorOpenTicks--;
            }
         }
         if (pendingRadarOpenTicks >= 0) {
            if (pendingRadarOpenTicks == 0) {
               if (client.screen == null) {
                  client.setScreen(new RadarScreen());
               }
               pendingRadarOpenTicks = -1;
            } else {
               pendingRadarOpenTicks--;
            }
         }
         if (pendingBaltopScrape) {
            if (client.screen == null) {
               pendingBaltopScrape = false;
               BaltopScraper.getInstance().startScraping(
                       null,
                       error -> {
                          LOGGER.warn("Baltop scraping failed: {}", error);
                          MessagingUtils.showMiniMessage("<red>Failed to load baltop: " + error + "</red>");
                       }
               );
            }
         }
         // tick the scraper (handles pending clicks and timeout)
         BaltopScraper.getInstance().tick();

         if (openHudEditorKeyBinding.consumeClick()) {
            if (client.screen != null) return;
            pendingHudEditorOpenTicks = 1;
         }
         if (addAllianceTargetKeyBinding.consumeClick()) {
            if (client.screen != null) return;
            LocalPlayer localPlayer = client.player;
            if (localPlayer == null) return;
            HitResult hitResult = client.hitResult;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player targetPlayer) {
               boolean isInvisible = targetPlayer.isInvisible();

               if (isInvisible) {
                  return;
               }

               boolean isCreative = targetPlayer.isCreative();
               boolean isSpectator = targetPlayer.isSpectator();
               if (isCreative || isSpectator) {
                  return;
               }

               Boolean added = Alliances.toggleAlliance(targetPlayer);
               String name = targetPlayer.getName().getString();
               if (added == null) {
                  MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<red>Couldn't update alliance for <white>" + MiniMessage.miniMessage().escapeTags(name) + "</white>.</red>"));
               } else if (added) {
                  MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<green>Added <white>" + MiniMessage.miniMessage().escapeTags(name) + "</white> to your alliance.</green>"));
               } else {
                  MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<yellow>Removed <white>" + MiniMessage.miniMessage().escapeTags(name) + "</white> from your alliance.</yellow>"));
               }
            } else {
               MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<red>You're not looking at a player.</red>"));
            }
         }
      });

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
                                       String translationKey = enabled ? TOGGLE_AFK_ENABLED_TRANSLATION_KEY : TOGGLE_AFK_DISABLED_TRANSLATION_KEY;
                                       MessagingUtils.showMessage(Component.translatable(translationKey), DEFAULT_MESSAGE_COLOR);
                                    });
                                    return 1;
                                 }))
                         .then(ClientCommandManager.literal("baltop")
                                 .executes(commandContext -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> {
                                       if (Config.isCustomBaltopInterfaceEnabled()) {
                                          pendingBaltopScrape = true;
                                       } else if (client.player != null) {
                                          client.player.connection.sendCommand("baltop");
                                       }
                                    });
                                    return 1;
                                 }))
                         .then(ClientCommandManager.literal("alliances")
                                 .executes(commandContext -> {
                                    Alliances.showAllianceList();
                                    return 1;
                                 })
                                 .then(ClientCommandManager.literal("list")
                                         .executes(commandContext -> {
                                            Alliances.showAllianceList();
                                            return 1;
                                         }))
                                 .then(ClientCommandManager.literal("add")
                                         .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                                 .executes(commandContext -> {
                                                    String username = StringArgumentType.getString(commandContext, "username");
                                                    String escapedUsername = MiniMessage.miniMessage().escapeTags(username);
                                                    Alliances.addAllianceAsync(username, added -> {
                                                       if (added) {
                                                          MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<green>Added <white>" + escapedUsername + "</white> to your alliance.</green>"));
                                                       } else {
                                                          MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<red>Could not find player <white>" + escapedUsername + "</white>.</red>"));
                                                       }
                                                    });
                                                    return 1;
                                                 })))
                                 .then(ClientCommandManager.literal("remove")
                                         .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                                 .executes(commandContext -> {
                                                    String username = StringArgumentType.getString(commandContext, "username");
                                                    String escapedUsername = MiniMessage.miniMessage().escapeTags(username);
                                                    Alliances.removeAllianceAsync(username, removed -> {
                                                       if (removed) {
                                                          MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<yellow>Removed <white>" + escapedUsername + "</white> from your alliance.</yellow>"));
                                                       } else {
                                                          MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<red>Could not find player <white>" + escapedUsername + "</white> in your alliance.</red>"));
                                                       }
                                                    });
                                                    return 1;
                                                 })))

                                 .then(ClientCommandManager.literal("clear")
                                         .executes(commandContext -> {
                                            Alliances.clearAlliances();
                                            MessagingUtils.showMiniMessage(Alliances.withDisabledWarning("<yellow>Cleared all alliance members.</yellow>"));
                                            return 1;
                                         })))
                         .then(ClientCommandManager.literal("track-poi")
                                 .then(ClientCommandManager.argument("poi", StringArgumentType.greedyString())
                                         .suggests((context, builder) -> {
                                            String remaining = builder.getRemainingLowerCase();
                                            for (dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition p : dev.candycup.lifestealutils.FeatureFlagController.getPois()) {
                                               if (p == null || p.name() == null || p.name().isBlank()) continue;
                                               String nameLower = p.name().toLowerCase();
                                               if (remaining.isBlank() || nameLower.contains(remaining)) {
                                                  builder.suggest(p.name());
                                               }
                                            }
                                            return builder.buildFuture();
                                         })
                                         .executes(commandContext -> {
                                            String poiArg = StringArgumentType.getString(commandContext, "poi").trim();
                                            if (poiArg.equalsIgnoreCase("none") || poiArg.equalsIgnoreCase("clear") || poiArg.equalsIgnoreCase("off")) {
                                               return handleUntrackPoi();
                                            }

                                            dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition matched = dev.candycup.lifestealutils.FeatureFlagController.getPois().stream()
                                                    .filter(p -> p.name() != null && p.name().equalsIgnoreCase(poiArg))
                                                    .findFirst().orElse(null);
                                            if (matched == null) {
                                               MessagingUtils.showMiniMessage("<red>Unknown POI: <white>" + MiniMessage.miniMessage().escapeTags(poiArg) + "</white></red>");
                                               return 0;
                                            }

                                            dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition currentTracked = resolveCurrentTrackedPoi();
                                            if (currentTracked != null && Objects.equals(currentTracked.id(), matched.id())) {
                                               return handleUntrackPoi();
                                            }

                                            Config.setPoiTrackedId(matched.id());
                                            MessagingUtils.showMiniMessage("<green>Now tracking POI: <white>" + MiniMessage.miniMessage().escapeTags(matched.name()) + "</white></green>");
                                            return 1;
                                         })))
                         .then(ClientCommandManager.literal("untrack-poi")
                                 .executes(commandContext -> handleUntrackPoi()))
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
      pendingBaltopScrape = true;
   }

   public static BasicTimerManager getBasicTimerManager() {
      return basicTimerManager;
   }

   /**
    * Clears any tracked POI and reports the result to the user.
    *
    * @return command result code
    */
   private static int handleUntrackPoi() {
      dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition currentTracked = resolveCurrentTrackedPoi();
      String trackedName = currentTracked != null && currentTracked.name() != null && !currentTracked.name().isBlank()
              ? currentTracked.name()
              : "POI";
      String escapedName = MiniMessage.miniMessage().escapeTags(trackedName);

      Config.setPoiTrackedId("");

      if (Config.isPoiAlwaysShowClosest()) {
         MessagingUtils.showMiniMessage("<yellow>No longer tracking <white>" + escapedName + "</white>! Reverting to tracking the closest POI.</yellow>");
      } else {
         MessagingUtils.showMiniMessage("<yellow>No longer tracking <white>" + escapedName + "</white>!</yellow>");
      }
      return 1;
   }

   /**
    * Resolves the currently tracked POI from configuration and player position.
    *
    * @return the currently tracked POI, or null if none can be resolved
    */
   private static dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition resolveCurrentTrackedPoi() {
      String configuredId = Config.getPoiTrackedId();
      if (configuredId != null && !configuredId.isBlank()) {
         return resolvePoiById(configuredId);
      }
      if (!Config.isPoiAlwaysShowClosest()) {
         return null;
      }
      return resolveClosestPoi();
   }

   /**
    * Finds a POI by its id.
    *
    * @param id the poi id
    * @return the matching poi, or null
    */
   private static dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition resolvePoiById(String id) {
      for (dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition p : dev.candycup.lifestealutils.FeatureFlagController.getPois()) {
         if (p == null || p.id() == null) continue;
         if (p.id().equals(id)) {
            return p;
         }
      }
      return null;
   }

   /**
    * Resolves the closest POI in the player's current dimension.
    *
    * @return the closest poi, or null if unavailable
    */
   private static dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition resolveClosestPoi() {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         return null;
      }

      String currentDimension = null;
      try {
         if (client.level.dimension() != null) {
            currentDimension = client.level.dimension().toString();
         }
      } catch (Exception ignore) {
      }

      double px = client.player.getX();
      double pz = client.player.getZ();

      dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition best = null;
      double bestDist = Double.MAX_VALUE;
      for (dev.candycup.lifestealutils.FeatureFlagController.PoiDefinition poi : dev.candycup.lifestealutils.FeatureFlagController.getPois()) {
         if (poi == null) continue;
         if (poi.dimension() != null && currentDimension != null) {
            if (!poi.dimension().equals(currentDimension) && !currentDimension.contains(poi.dimension())) {
               continue;
            }
         }

         double dx = poi.x() - px;
         double dz = poi.z() - pz;
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist < bestDist) {
            bestDist = dist;
            best = poi;
         }
      }

      return best;
   }

}