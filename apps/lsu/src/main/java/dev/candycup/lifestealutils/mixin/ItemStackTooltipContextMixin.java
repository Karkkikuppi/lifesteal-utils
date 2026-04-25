package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.api.SplashPotionTooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackTooltipContextMixin {

   @Inject(method = "addDetailsToTooltip", at = @At("HEAD"))
   private void lifestealutils$markSplashPotion(CallbackInfo ci) {
      SplashPotionTooltipContext.IS_SPLASH_POTION.set(((ItemStack) (Object) this).getItem() == Items.SPLASH_POTION);
   }

   @Inject(method = "addDetailsToTooltip", at = @At("RETURN"))
   private void lifestealutils$clearSplashPotion(CallbackInfo ci) {
      SplashPotionTooltipContext.IS_SPLASH_POTION.set(false);
   }
}
