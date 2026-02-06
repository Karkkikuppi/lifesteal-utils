package dev.candycup.lifestealutils.interapi;

import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public class MessagingUtils {
   public static void showMessage(String message) {
      showMessage(Component.literal(message), 0xFFFFFF);
   }

   public static void showMessage(Component message, int color) {
      Minecraft.getInstance().gui.getChat().addMessage(
              message,
              new MessageSignature(new byte[256]),
              new GuiMessageTag(
                      color,
                      GuiMessageTag.Icon.CHAT_MODIFIED,
                      Component.literal("Message modified by Lifesteal Utils"),
                      "Lifesteal Utils"
              )
      );
   }

   public static void showMiniMessage(String miniMessage) {
      showMiniMessage(miniMessage, 0xFFFFFF);
   }

   public static void showMiniMessage(String miniMessage, int color) {
      showMessage(
              MinecraftClientAudiences.of().asNative(
                      MiniMessage.miniMessage().deserialize(miniMessage)
              ),
              color
      );
   }

   public static Component miniMessage(String miniMessage) {
      return MinecraftClientAudiences.of().asNative(
              MiniMessage.miniMessage().deserialize(miniMessage)
      );
   }

   public static Component miniMessage(net.kyori.adventure.text.Component component) {
      return MinecraftClientAudiences.of().asNative(component);
   }

   public static net.kyori.adventure.text.Component asMiniMessage(Component component) {
      return MinecraftClientAudiences.of().asAdventure(component);
   }

   public static net.kyori.adventure.text.Component asMiniMessage(String string) {
      return MinecraftClientAudiences.of().asAdventure(Component.literal(string));
   }

   /**
    * as support for components in the splash screen was added in 1.21.11,
    * older versions need a safe string version of minimessage (which removes formatting)
    *
    * @param miniMessage the minimessage formatted string
    * @return the safe string for the current version
    */
   public static String miniMessageToSplashSafe(String miniMessage) {
      //? if > 1.21.10 {
      return miniMessage;
      //? } else {
      /*return miniMessage(miniMessage).getString();
       *///? }
   }
}

