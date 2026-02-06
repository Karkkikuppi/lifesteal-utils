package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealUtils;
import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.api.LifestealTablistAPI;
import dev.candycup.lifestealutils.features.baltop.BaltopScraper;
import dev.candycup.lifestealutils.ui.BaltopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts client-bound packets for Lifesteal Utils features.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

   @Inject(method = "handleTabListCustomisation", at = @At("RETURN"))
   private void onTabListUpdate(ClientboundTabListPacket packet, CallbackInfo ci) {
      if (LifestealServerDetector.isOnLifestealServer()) {
         LifestealTablistAPI.updateFromFooter(packet.footer());
      } else {
         LifestealTablistAPI.reset();
      }
   }

   /**
    * Intercepts container screen opening to prevent server GUI from replacing our BaltopScreen.
    * We let vanilla set up the containerMenu but restore our screen afterwards.
    */
   @Inject(method = "handleOpenScreen", at = @At("RETURN"))
   private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
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
      if (!Config.isCustomBaltopInterfaceEnabled()) {
         return;
      }

      if (!LifestealServerDetector.isOnLifestealServer()) {
         return;
      }

      String lowered = command.toLowerCase();
      if (!lowered.equals("baltop") && !lowered.startsWith("baltop ")) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (!(client.screen instanceof ChatScreen)) {
         return;
      }

      client.execute(LifestealUtils::queueBaltopScrape);
      ci.cancel();
   }
}
