package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.features.ah.AhSearchAutomation;
import dev.candycup.lifestealutils.features.ah.AhOverlaySearchState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientPacketListenerDialogSearchMixin {
   @Inject(method = "handleShowDialog", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$handleSearchDialogSilently(ClientboundShowDialogPacket packet, CallbackInfo ci) {
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      Minecraft client = Minecraft.getInstance();
      if (!(client.screen instanceof AbstractContainerScreen<?>)) {
         return;
      }

      Object screen = client.screen;
      if (!(screen instanceof AhOverlaySearchState overlayMixin)) {
         return;
      }
      if (!overlayMixin.lifestealutils$consumeSuppressNextSearchDialogFlag()) {
         return;
      }

      String query = AhSearchAutomation.consumePendingQuery();
      if (query == null) {
         ci.cancel();
         return;
      }
      AhSearchAutomation.setActiveQuery(query);

      if (!(packet.dialog().value() instanceof net.minecraft.server.dialog.ConfirmationDialog dialog)) {
         ci.cancel();
         return;
      }

      if (dialog.yesButton().action().isPresent()) {
         var action = dialog.yesButton().action().get();
         Optional<ClickEvent> click = action.createAction(java.util.Map.of("input", net.minecraft.server.dialog.action.Action.ValueGetter.of(query)));
         if (click.isPresent() && click.get() instanceof ClickEvent.Custom(
                 net.minecraft.resources.Identifier id, Optional<net.minecraft.nbt.Tag> payload
         )) {
            if (client.player != null && client.player.connection != null) {
               client.player.connection.send(new net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket(id, payload));
            }
         }
      }

      ci.cancel();
   }
}
