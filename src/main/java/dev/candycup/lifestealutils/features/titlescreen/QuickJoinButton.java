package dev.candycup.lifestealutils.features.titlescreen;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.event.EventPriority;
import dev.candycup.lifestealutils.event.events.TitleScreenInitEvent;
import dev.candycup.lifestealutils.event.listener.UIEventListener;
import dev.candycup.lifestealutils.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * adds a quick join button to the title screen for connecting to lifesteal network.
 */
public final class QuickJoinButton implements UIEventListener {

   @Override
   public boolean isEnabled() {
      return Config.getQuickJoinButtonEnabled();
   }

   @Override
   public EventPriority getPriority() {
      return EventPriority.NORMAL;
   }

   @Override
   public void onTitleScreenInit(TitleScreenInitEvent event) {
      TitleScreen screen = event.getTitleScreen();

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
   }
}
