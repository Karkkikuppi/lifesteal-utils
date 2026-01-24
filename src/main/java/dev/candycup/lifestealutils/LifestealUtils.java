package dev.candycup.lifestealutils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.candycup.lifestealutils.event.EventBus;
import dev.candycup.lifestealutils.event.events.ClientTickEvent;
import dev.candycup.lifestealutils.features.alliances.Alliances;
import dev.candycup.lifestealutils.features.items.RareItemHighlight;
import dev.candycup.lifestealutils.features.messages.ChatTagRemover;
import dev.candycup.lifestealutils.features.messages.PrivateMessageFormatter;
import dev.candycup.lifestealutils.features.messages.RankPlusColorNormalizer;
import dev.candycup.lifestealutils.features.titlescreen.CustomSplashes;
import dev.candycup.lifestealutils.features.titlescreen.QuickJoinButton;
import dev.candycup.lifestealutils.hud.HudDisplayLayer;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.features.combat.UnbrokenChainTracker;
import dev.candycup.lifestealutils.features.timers.BasicTimerManager;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.candycup.lifestealutils.ui.HudElementEditor;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LifestealUtils implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils");
   //? if >1.21.8
   private static KeyMapping.Category LIFESTEAL_UTIL_BINDS;
   private static KeyMapping openHudEditorKeyBinding;
   private static KeyMapping addAllianceTargetKeyBinding;
   private static int pendingConfigOpenTicks = -1;
   private static int pendingHudEditorOpenTicks = -1;
   
   private static UnbrokenChainTracker unbrokenChainTracker;
   private static BasicTimerManager basicTimerManager;
   private static PrivateMessageFormatter privateMessageFormatter;
   private static ChatTagRemover chatTagRemover;
   private static RankPlusColorNormalizer rankPlusColorNormalizer;
   private static Alliances alliances;
   private static RareItemHighlight rareItemHighlight;
   private static QuickJoinButton quickJoinButton;
   private static CustomSplashes customSplashes;

   /**
    * Initializes client-side components for the mod: loads configuration, registers HUD elements
    * and feature instances with the event bus, creates key bindings, attaches HUD layers, and
    * registers tick and client command handlers.
    *
    * <p>As part of initialization this method also posts per-tick ClientTickEvent via the tick
    * handler, manages deferred opening of configuration and HUD editor screens, handles keybinding
    * actions (open HUD editor, toggle alliance target), and registers the "lsu" client commands.
    */
   @Override
   public void onInitializeClient() {
      LOGGER.info("Lifesteal Utils initializing. I LOVE FABRIC !!!!!!");
      Config.load();

      HudElementManager.init();
      
      // initialize and register event-based features
      basicTimerManager = new BasicTimerManager(FeatureFlagController.getBasicTimers());
      EventBus.getInstance().register(basicTimerManager);
      for (HudElementDefinition definition : basicTimerManager.getHudDefinitions()) {
         HudElementManager.register(definition);
      }

      unbrokenChainTracker = new UnbrokenChainTracker();
      EventBus.getInstance().register(unbrokenChainTracker);
      HudElementManager.register(unbrokenChainTracker.getHudDefinition());
      
      // chat message features - each is independent and modular
      privateMessageFormatter = new dev.candycup.lifestealutils.features.messages.PrivateMessageFormatter();
      EventBus.getInstance().register(privateMessageFormatter);
      
      chatTagRemover = new dev.candycup.lifestealutils.features.messages.ChatTagRemover();
      EventBus.getInstance().register(chatTagRemover);
      
      rankPlusColorNormalizer = new dev.candycup.lifestealutils.features.messages.RankPlusColorNormalizer();
      EventBus.getInstance().register(rankPlusColorNormalizer);
      
      // render-based features (performance-critical)
      alliances = new Alliances();
      EventBus.getInstance().register(alliances);
      
      rareItemHighlight = new dev.candycup.lifestealutils.features.items.RareItemHighlight();
      EventBus.getInstance().register(rareItemHighlight);
      
      // UI features
      quickJoinButton = new dev.candycup.lifestealutils.features.titlescreen.QuickJoinButton();
      EventBus.getInstance().register(quickJoinButton);
      
      customSplashes = new dev.candycup.lifestealutils.features.titlescreen.CustomSplashes();
      EventBus.getInstance().register(customSplashes);

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
         // post tick event to event-based features
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
                          net.minecraft.network.chat.Component.literal("HUD Element Editor")
                  ));
               }
               pendingHudEditorOpenTicks = -1;
            } else {
               pendingHudEditorOpenTicks--;
            }
         }
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
                         .then(ClientCommandManager.literal("support")
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
                                         })))
         );
      });
   }

   /**
    * Provides access to the application's BasicTimerManager instance.
    *
    * @return the shared BasicTimerManager instance, or `null` if it has not been initialized
    */
   public static BasicTimerManager getBasicTimerManager() {
      return basicTimerManager;
   }

}