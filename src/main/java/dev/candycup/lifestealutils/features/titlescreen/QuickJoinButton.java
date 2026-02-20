package dev.candycup.lifestealutils.features.titlescreen;

import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.mixin.ScreenAccessor;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * adds a quick join button to the title screen for connecting to lifesteal network.
 */
public final class QuickJoinButton {

   @Getter
   @Setter
   @SerialEntry(comment = "Quick Join button on the title screen")
   @ConfigurableBoolean(location = "qol.titlescreen.quickjoinbuttonenabled")
   private static boolean quickJoinButtonEnabled = true;

   public QuickJoinButton() {
      LifestealUtilsEvents.TITLE_SCREEN_INIT.register((screen) -> {
         if (!quickJoinButtonEnabled) return;

         int l = screen.height / 4 + 48;
         SpriteIconButton button = ((ScreenAccessor) screen).invokeAddRenderableWidget(
                 SpriteIconButton.builder(
                         Component.translatable("menu.options"),
                         (buttonWidget) -> {
                            Minecraft mc = Minecraft.getInstance();
                            JoinMultiplayerScreen join = new JoinMultiplayerScreen(screen);
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
