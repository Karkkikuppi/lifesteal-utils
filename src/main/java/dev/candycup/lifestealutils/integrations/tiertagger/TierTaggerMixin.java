package dev.candycup.lifestealutils.integrations.tiertagger;

import com.kevin.tiertagger.TierTagger;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.candycup.lifestealutils.api.LifestealAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.UUID;

/**
 * Mixin to move the TierTagger tier display to the right side of the player name
 * instead of the left side.
 *
 * <p>Original: {@code [icon][tier] | [name]}
 * <p>Modified: {@code [name] | [icon][tier]}
 */
@Mixin(TierTagger.class)
public class TierTaggerMixin {
   @Unique
   private static final int APPENDED_SIBLING_COUNT = 2;

   /**
    * Modifies the return value of {@code appendTier} to place the tier after the name
    * instead of before it.
    *
    * @param original the original return value from appendTier (tier + separator + name, or just name)
    * @param uuid     the player's UUID (captured from method args)
    * @param name     the original player name component (captured from method args)
    * @return the modified component with tier on the right side
    */
   @ModifyReturnValue(
           method = "appendTier",
           at = @At("RETURN")
   )
   private static Component modifyAppendTierReturnValue(Component original, UUID uuid, Component name) {
      if (!LifestealAPI.isOnLifestealNetwork()) {
         return original;
      }

      // if the original return is the same as the name, no tier was appended
      if (original == name) {
         return original;
      }

      // the original component tree is: [icon?][tier] -> " | " -> name
      // we strip the separator and name siblings from the tier component, then rebuild reversed
      List<Component> siblings = original.getSiblings();
      if (siblings.size() < APPENDED_SIBLING_COUNT) {
         return original;
      }

      // extract the tier part by copying the root and keeping only the pre-existing siblings
      MutableComponent tierPart = original.plainCopy();
      List<Component> tierSiblings = siblings.subList(0, siblings.size() - APPENDED_SIBLING_COUNT);
      tierSiblings.forEach(tierPart::append);

      // rebuild as: name | tier
      return name.copy()
              .append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
              .append(tierPart);
   }
}
