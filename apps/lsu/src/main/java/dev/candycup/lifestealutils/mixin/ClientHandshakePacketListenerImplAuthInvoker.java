package dev.candycup.lifestealutils.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
//? if >1.21.8
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Invoker for the vanilla authenticateServer method.
 * <p>
 * This allows us to authenticate with Mojang without directly accessing
 * the user's access token (which is smth i just didn't want to do)
 */
@Mixin(ClientHandshakePacketListenerImpl.class)
public interface ClientHandshakePacketListenerImplAuthInvoker {
    //? if >1.21.8 {
    @Invoker("<init>")
    static ClientHandshakePacketListenerImpl lifestealutils$create(
            Connection connection,
            Minecraft minecraft,
            ServerData serverData,
            Screen screen,
            boolean quickPlay,
            Duration worldLoadDuration,
            Consumer<Component> updateStatus,
            LevelLoadTracker levelLoadTracker,
            TransferState transferState
    ) {
        throw new AssertionError();
    }
    //?} else {
   /*@Invoker("<init>")
   static ClientHandshakePacketListenerImpl lifestealutils$create(
           Connection connection,
           Minecraft minecraft,
           ServerData serverData,
           Screen screen,
           boolean quickPlay,
           Duration worldLoadDuration,
           Consumer<Component> updateStatus,
           TransferState transferState
   ) {
      throw new AssertionError();
   }
   *///?}

    /**
     * Invokes the vanilla server authentication method.
     *
     * @param serverId the server id hash to authenticate
     * @return a disconnect component on failure, or null on success
     */
    @Invoker("authenticateServer")
    Component lifestealutils$authenticateServer(String serverId);
}
