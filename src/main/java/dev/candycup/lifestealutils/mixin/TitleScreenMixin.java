package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
   protected TitleScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   public void init(CallbackInfo ci) {
      LifestealUtilsEvents.TITLE_SCREEN_INIT.invoker().onTitleScreenInit((TitleScreen) (Object) this);
   }
}
