package dev.candycup.lifestealutils.features.shortcuts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TitleScreenQuickJoin {
   public static SpriteIconButton getQuickJoinWidget(Screen titleScreen) {
      return SpriteIconButton.builder(
              Component.translatable("menu.options"),
              (buttonWidget) -> {
                 Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(titleScreen));
                 assert Minecraft.getInstance().screen != null;
                 ConnectScreen.startConnecting(
                         Minecraft.getInstance().screen,
                         Minecraft.getInstance(),
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
              Identifier.fromNamespaceAndPath(
                      "lifestealutils",
                      "icon/lsn"
              ),
              18,
              18
      ).build();
   }
}
