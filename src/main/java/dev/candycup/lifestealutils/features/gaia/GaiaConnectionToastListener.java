package dev.candycup.lifestealutils.features.gaia;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealUtils;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.GatewayDisconnectedEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.LifestealShardSwapEvent;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ServerChangeEvent;
import dev.candycup.lifestealutils.gaia.gateway.GaiaGatewayClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GaiaConnectionToastListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("lifestealutils/gaia-toast");
   private static final SystemToast.SystemToastId GAIA_UNAVAILABLE_TOAST_ID = new SystemToast.SystemToastId();
   private static final SystemToast.SystemToastId GAIA_RESTORED_TOAST_ID = new SystemToast.SystemToastId();

   private String previousShard;
   private boolean shouldShowRestoreToast;

   public GaiaConnectionToastListener() {
      LifestealUtilsEvents.SERVER_CHANGE.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onServerChange(event);
      });
      LifestealUtilsEvents.SHARD_SWAP.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onShardSwap(event);
      });
      LifestealUtilsEvents.GATEWAY_CONNECTED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onGatewayConnected();
      });
      LifestealUtilsEvents.GATEWAY_DISCONNECTED.register(event -> {
         if (!isEnabled()) {
            return;
         }
         onGatewayDisconnected(event);
      });
   }

   private boolean isEnabled() {
      return Config.isGaiaAdvancedFeaturesEnabled();
   }

   private void onServerChange(ServerChangeEvent event) {
      if (event.getType() == ServerChangeEvent.Type.DISCONNECTED) {
         resetSession();
      }
   }

   private void onShardSwap(LifestealShardSwapEvent event) {
      String shard = event.getShardName();
      if (shard == null || shard.isBlank()) {
         return;
      }

      if (isHub(previousShard) && !isHub(shard)) {
         GaiaGatewayClient gaiaGatewayClient = LifestealUtils.getGaiaGatewayClient();
         boolean connected = gaiaGatewayClient != null && gaiaGatewayClient.isConnected();
         if (!connected) {
            if (!shouldShowRestoreToast) {
               showUnavailableToast();
            }
            shouldShowRestoreToast = true;
         } else {
            shouldShowRestoreToast = false;
         }
      }

      previousShard = shard;
   }

   private void onGatewayConnected() {
      if (!shouldShowRestoreToast) {
         return;
      }

      showRestoredToast();
      shouldShowRestoreToast = false;
   }

   private void onGatewayDisconnected(GatewayDisconnectedEvent event) {
      if (!event.willReconnect()) {
         return;
      }

      showUnavailableToast();
      shouldShowRestoreToast = true;
   }

   private void showUnavailableToast() {
      showToast(
              GAIA_UNAVAILABLE_TOAST_ID,
              "lsu.gaia.toast.unavailable.title",
              ChatFormatting.GOLD,
              "lsu.gaia.toast.unavailable.body",
              "Gaia unavailable"
      );
   }

   private void showRestoredToast() {
      showToast(
              GAIA_RESTORED_TOAST_ID,
              "lsu.gaia.toast.restored.title",
              ChatFormatting.GREEN,
              "lsu.gaia.toast.restored.body",
              "Gaia restored"
      );
   }

   private void showToast(
           SystemToast.SystemToastId toastId,
           String titleKey,
           ChatFormatting titleColor,
           String bodyKey,
           String debugName
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         LOGGER.debug("Skipping {} toast because player is null", debugName);
         return;
      }

      Component title = Component.translatable(titleKey)
              .withStyle(titleColor);
      Component body = Component.translatable(bodyKey)
              .withStyle(ChatFormatting.GRAY);

      minecraft.execute(() ->
              SystemToast.add(
                      minecraft.getToastManager(),
                      toastId,
                      title,
                      body
              )
      );
   }

   private static boolean isHub(String shard) {
      return shard != null && shard.startsWith("hub-");
   }

   private void resetSession() {
      previousShard = null;
      shouldShowRestoreToast = false;
   }
}