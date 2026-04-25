package dev.candycup.lifestealutils.features.titlescreen;

import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.gaia.GaiaConsentController;
import dev.candycup.lifestealutils.gaia.GaiaConsentScreen;
import dev.candycup.lifestealutils.mixin.ScreenAccessor;
import dev.candycup.configura.serial.SerialEntry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * adds a quick join button to the title screen for connecting to lifesteal network.
 */
public final class QuickJoinButton {
   private static int pendingQuickJoinTicks = -1;
   private static Screen pendingLastScreen;

   @Getter
   @Setter
   @SerialEntry(comment = "Quick Join button on the title screen")
   @ConfigurableBoolean(location = "qol.titlescreen.quickjoinbuttonenabled")
   private static boolean quickJoinButtonEnabled = true;

   public QuickJoinButton() {
      LifestealUtilsEvents.CLIENT_TICK.register(event -> {
         Minecraft mc = event.getClient();
         if (pendingQuickJoinTicks < 0) {
            return;
         }
         if (pendingQuickJoinTicks > 0) {
            pendingQuickJoinTicks--;
            return;
         }

         Screen lastScreen = pendingLastScreen;
         pendingQuickJoinTicks = -1;
         pendingLastScreen = null;

         if (lastScreen == null) {
            return;
         }
         if (GaiaConsentController.shouldShowConsent()) {
            mc.setScreen(new GaiaConsentScreen(lastScreen, true));
            return;
         }

         JoinMultiplayerScreen join = new JoinMultiplayerScreen(lastScreen);
         mc.setScreen(join);
         ConnectScreen.startConnecting(
                 join,
                 mc,
                 ServerAddress.parseString("lifesteal.net"),
                 new ServerData(
                         "Lifesteal Network",
                         "lifesteal.net",
                         ServerData.Type.OTHER
                 ),
                 true,
                 null
         );
      });

      LifestealUtilsEvents.TITLE_SCREEN_INIT.register((screen) -> {
         if (!quickJoinButtonEnabled) return;

         int l = screen.height / 4 + 48;
         SpriteIconButton button = ((ScreenAccessor) screen).invokeAddRenderableWidget(
                 SpriteIconButton.builder(
                         Component.translatable("menu.options"),
                         (buttonWidget) -> {
                            pendingLastScreen = screen;
                            pendingQuickJoinTicks = 1;
                         },
                         true
                 ).width(20).sprite(
                         Identifier.fromNamespaceAndPath("lifestealutils", "icon/lsn"),
                         18,
                         18
                 ).build()
         );
         button.setPosition(screen.width / 2 + 104, l);
      });
   }
}
