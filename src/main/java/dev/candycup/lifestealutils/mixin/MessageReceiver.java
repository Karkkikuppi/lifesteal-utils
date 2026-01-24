package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealServerDetector;
//import dev.candycup.lifestealutils.features.messages.ClaimChatCustomizer;
import dev.candycup.lifestealutils.features.messages.MessageCustomizer;
import dev.candycup.lifestealutils.features.timers.BasicTimerManager;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MessageReceiver {
   @Shadow
   @Final
   private static Logger LOGGER;

   // Re-entrancy guard for internally added messages
   private static boolean lsuInternalAdd = false;

   @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;)V", cancellable = true)
   private void addMessage(Component component, CallbackInfo ci) {
      // If this message was added internally (we replaced a message), let it through without re-processing
      if (lsuInternalAdd) {
         lsuInternalAdd = false;
         return;
      }

      if (!LifestealServerDetector.isOnLifestealServer()) {
         return;
      }

      String rawMessage = component.getString();
      BasicTimerManager.handleChatMessage(rawMessage);

      /*
      // Attempt to handle claim chat formatting; if handled, cancel the original message
      if (Config.getEnableClaimChatFormat() && ClaimChatCustomizer.tryHandle(rawMessage)) {
         ci.cancel();
         return;
      }
      */

      // Attempt to handle private message formatting; if handled, cancel the original message
      if (Config.getEnablePmFormat() && MessageCustomizer.tryHandle(rawMessage)) {
         ci.cancel();
      }

      // Optionally remove chat tags or merge the '+' coloring into the rank label
      boolean disableTags = Config.getDisableChatTags();
      boolean removePlus = Config.getRemoveUniquePlusColor();
      if (disableTags || removePlus) {
         // Use the serialized MiniMessage form so we preserve color and formatting metadata
         String serialized = MiniMessage.miniMessage().serialize(MinecraftClientAudiences.of().asAdventure(component));
         String filtered = serialized;
         if (removePlus) {
            filtered = dev.candycup.lifestealutils.features.messages.RemoveUniquePlusColor.apply(filtered);
         }
         if (disableTags) {
            filtered = dev.candycup.lifestealutils.features.messages.DisableChatTags.removeTag(filtered);
         }
         // Ensure only a single space follows the closing rank bracket for visual consistency
         filtered = filtered.replaceAll("(<dark_gray>\\]</dark_gray>)\\s+", "$1 ");
         // Fallback: normalize any bracket->space runs even if formatting tags differ
         filtered = filtered.replaceAll("\\]\\s+", "] ");
         // Final whitespace normalization (collapse spaces and non-breaking spaces)
         filtered = filtered.replaceAll("[\\s\\u00A0]+", " ").trim();
         boolean changed = !filtered.equals(serialized);

         if (changed) {
            ci.cancel();
            lsuInternalAdd = true;
            // Convert back to a Component using our helper so formatting is preserved
            ((ChatComponent) (Object) this).addMessage(MessagingUtils.miniMessage(filtered));
         }
      }
   }
}