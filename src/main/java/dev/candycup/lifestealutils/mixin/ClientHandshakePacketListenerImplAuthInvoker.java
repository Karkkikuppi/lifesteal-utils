package dev.candycup.lifestealutils.mixin;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker for the vanilla authenticateServer method.
 * <p>
 * This allows us to authenticate with Mojang without directly accessing
 * the user's access token (which is smth i just didn't want to do)
 */
@Mixin(ClientHandshakePacketListenerImpl.class)
public interface ClientHandshakePacketListenerImplAuthInvoker {
   /**
    * Invokes the vanilla server authentication method.
    *
    * @param serverId the server id hash to authenticate
    * @return a disconnect component on failure, or null on success
    */
   @Invoker("authenticateServer")
   Component lifestealutils$authenticateServer(String serverId);
}
