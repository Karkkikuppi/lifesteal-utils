package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.api.PrestigeUtils;
import dev.candycup.lifestealutils.api.SplashPotionTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(PotionContents.class)
public class PotionTooltipMixin {

   @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
   private void lifestealutils$appendBoostedDuration(
           Item.TooltipContext ctx,
           Consumer<Component> consumer,
           TooltipFlag flag,
           DataComponentGetter getter,
           CallbackInfo ci
   ) {
      if (!Boolean.TRUE.equals(SplashPotionTooltipContext.IS_SPLASH_POTION.get())) return;
      if (!Config.isShowActualPotionDuration()) return;
      if (!LifestealAPI.isOnLifestealNetwork()) return;

      float boostPercent = PrestigeUtils.getPotionDurationBoostPercent();
      if (boostPercent <= 0f) return;

      float baseScale = getter.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F).floatValue();
      float boostedScale = baseScale * (1f + boostPercent / 100f);

      PotionContents self = (PotionContents) (Object) this;
      List<String> boostedDurations = new ArrayList<>();
      for (MobEffectInstance effect : self.getAllEffects()) {
         if (effect.isInfiniteDuration()) {
            boostedDurations.add(null);
         } else {
            int boostedTicks = Mth.floor(effect.getDuration() * boostedScale);
            boostedDurations.add(StringUtil.formatTickDuration(boostedTicks, ctx.tickRate()));
         }
      }

      int[] callIndex = {0};
      Consumer<Component> wrappedConsumer = original -> {
         int idx = callIndex[0]++;
         if (idx < boostedDurations.size() && boostedDurations.get(idx) != null) {
            MutableComponent line = original.copy().append(
                    Component.literal(" (" + boostedDurations.get(idx) + ")")
                            .withStyle(ChatFormatting.GOLD)
            );
            consumer.accept(line);
         } else {
            consumer.accept(original);
         }
      };

      PotionContents.addPotionTooltip(self.getAllEffects(), wrappedConsumer, baseScale, ctx.tickRate());
      ci.cancel();
   }
}
