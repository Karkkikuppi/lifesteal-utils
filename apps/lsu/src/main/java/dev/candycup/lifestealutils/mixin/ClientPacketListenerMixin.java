package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealUtils;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ContainerContentSetEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.features.baltop.BaltopScraper;
import dev.candycup.lifestealutils.ui.BaltopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts client-bound packets for Lifesteal Utils features.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/packets");
   private static String activeServerAddress;

   @Inject(method = "handleLogin", at = @At("RETURN"))
   private void onHandleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
      // fire server connection event
      String serverAddress = "";
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.getCurrentServer() != null) {
         serverAddress = minecraft.getCurrentServer().ip;
      }

      if (!serverAddress.isBlank() && serverAddress.equals(activeServerAddress)) {
         return;
      }

      activeServerAddress = serverAddress;

      LifestealUtilsEvents.SERVER_CHANGE.invoker().onServerChange(new ServerChangeEvent(ServerChangeEvent.Type.CONNECTED, serverAddress));
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void onClose(CallbackInfo ci) {
      // fire server disconnection event
      String serverAddress = "";
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.getCurrentServer() != null) {
         serverAddress = minecraft.getCurrentServer().ip;
      }

      if (serverAddress.isBlank() && activeServerAddress != null) {
         serverAddress = activeServerAddress;
      }

      if (activeServerAddress == null && serverAddress.isBlank()) {
         return;
      }

      activeServerAddress = null;

      LifestealUtilsEvents.SERVER_CHANGE.invoker().onServerChange(new ServerChangeEvent(ServerChangeEvent.Type.DISCONNECTED, serverAddress));
   }

   /**
    * Fires CONTAINER_CONTENT_SET after the server populates a non-player container's slots.
    * Slots are fully populated by this point, so listeners can safely read items.
    */
   @Inject(method = "handleContainerContent", at = @At("RETURN"))
   private void onHandleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
      if (packet.containerId() == 0) return; // player inventory - skip

      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         LOGGER.debug("handleContainerContent: player is null, skipping event (containerId={})", packet.containerId());
         return;
      }

      AbstractContainerMenu menu = client.player.containerMenu;
      if (menu.containerId != packet.containerId()) {
         LOGGER.debug("handleContainerContent: containerId mismatch (packet={}, menu={}), skipping", packet.containerId(), menu.containerId);
         return;
      }

      Screen screen = client.screen;
      String title = "";
      if (screen instanceof AbstractContainerScreen<?> containerScreen) {
         title = containerScreen.getTitle().getString();
      } else {
         LOGGER.debug("handleContainerContent: screen is not a container screen ({}), title will be empty", screen != null ? screen.getClass().getSimpleName() : "null");
      }

      LOGGER.debug("handleContainerContent: firing CONTAINER_CONTENT_SET for containerId={}, title='{}'", packet.containerId(), title);
      LifestealUtilsEvents.CONTAINER_CONTENT_SET.invoker().onContainerContentSet(new ContainerContentSetEvent(menu, title));
   }

   /**
    * Intercepts container screen opening to prevent server GUI from replacing our BaltopScreen.
    * We let vanilla set up the containerMenu but restore our screen afterwards.
    */
   @Inject(method = "handleOpenScreen", at = @At("RETURN"))
   private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      if (BaltopScraper.getInstance().isScraping()) {
         Minecraft client = Minecraft.getInstance();
         // vanilla has now set up containerMenu, but it also replaced our screen
         // restore our BaltopScreen so user doesn't see the server GUI
         if (!(client.screen instanceof BaltopScreen)) {
            BaltopScreen baltopScreen = BaltopScraper.getInstance().getActiveScreen();
            if (baltopScreen != null) {
               client.execute(() -> client.setScreen(baltopScreen));
            }
         }
      }
   }

   /**
    * Overrides the /baltop command to open the custom interface when typed manually.
    */
   @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
   private void onSendCommand(String command, CallbackInfo ci) {
      LifestealUtilsEvents.COMMAND_SENT.invoker().onCommandSent(command);

      String loweredCommand = command.trim().toLowerCase();
      if (!LifestealAPI.isOnLifestealNetwork()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (!(client.screen instanceof ChatScreen)) {
         return;
      }

      if (!Config.isCustomBaltopInterfaceEnabled()) {
         return;
      }

      if (!loweredCommand.equals("baltop") && !loweredCommand.startsWith("baltop ")) {
         return;
      }

      client.execute(LifestealUtils::queueBaltopScrape);
      ci.cancel();
   }
}

