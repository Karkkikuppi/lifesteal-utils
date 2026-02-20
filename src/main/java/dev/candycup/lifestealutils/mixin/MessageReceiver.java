package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ChatMessageReceivedEvent;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MessageReceiver {
   @Shadow
   @Final
   private static Logger LOGGER;

   @Unique
   private static final ThreadLocal<Boolean> lifestealutils$reentrant =
           ThreadLocal.withInitial(() -> false);

   @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;)V", cancellable = true)
   private void addMessage(Component component, CallbackInfo ci) {
      Component modified = lifestealutils$filter(component);
      if (modified == null) {
         ci.cancel();
         return;
      }

      if (modified != component) {
         ci.cancel();
         lifestealutils$reentrant.set(true);
         try {
            ((ChatComponent) (Object) this).addMessage(modified);
         } finally {
            lifestealutils$reentrant.set(false);
         }
      }
   }

   @Inject(
           at = @At("HEAD"),
           method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
           cancellable = true
   )
   private void addMessage(Component component, MessageSignature messageSignature, GuiMessageTag guiMessageTag, CallbackInfo ci) {
      Component modified = lifestealutils$filter(component);
      if (modified == null) {
         ci.cancel();
         return;
      }

      if (modified != component) {
         ci.cancel();
         lifestealutils$reentrant.set(true);
         try {
            ((ChatComponent) (Object) this).addMessage(modified, messageSignature, guiMessageTag);
         } finally {
            lifestealutils$reentrant.set(false);
         }
      }
   }

   @Unique
   private Component lifestealutils$filter(Component component) {
      if (lifestealutils$reentrant.get()) {
         return component;
      }

      if (!LifestealAPI.isOnLifestealNetwork()) {
         return component;
      }

      ChatMessageReceivedEvent event = new ChatMessageReceivedEvent(component);
      LifestealUtilsEvents.CHAT_MESSAGE_RECEIVED.invoker().onChatMessageReceived(event);

      if (event.isCancelled()) {
         return null;
      }

      Component modified = event.getModifiedMessage();
      return modified != null ? modified : component;
   }
}