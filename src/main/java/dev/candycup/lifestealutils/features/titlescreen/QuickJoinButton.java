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

    /**
     * Determines whether the quick-join button should be shown based on configuration.
     *
     * @return true if the quick-join button is enabled in the configuration, false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return Config.getQuickJoinButtonEnabled();
    }

    /**
     * Specifies the event priority used by this listener.
     *
     * @return the listener's event priority, `EventPriority.NORMAL`
     */
    @Override
    public EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Adds a quick-join button to the title screen that opens the multiplayer screen and initiates a connection to the Lifesteal network.
     *
     * When invoked, this method injects a clickable button into the title screen UI; activating the button transitions to the multiplayer join screen and starts connecting to "lifesteal.net" with a ServerData entry for "Lifesteal Network".
     *
     * @param event the title screen initialization event providing access to the TitleScreen to modify
     */
    @Override
    public void onTitleScreenInit(TitleScreenInitEvent event) {
        TitleScreen screen = event.getTitleScreen();
        
        int l = screen.height / 4 + 48;
        SpriteIconButton button = ((ScreenAccessor) screen).invokeAddRenderableWidget(
                SpriteIconButton.builder(
                        Component.translatable("menu.options"),
                        (buttonWidget) -> {
                            Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(screen));
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
                        Identifier.fromNamespaceAndPath("lifestealutils", "icon/lsn"),
                        22,
                        22
                ).build()
        );
        button.setPosition(screen.width / 2 + 104, l);
    }
}