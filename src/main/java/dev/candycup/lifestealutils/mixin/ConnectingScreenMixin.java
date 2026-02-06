package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.interapi.MessagingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectingScreenMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void lifestealutils$render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
      /*
       guiGraphics.drawCenteredString(
                ((ConnectScreen)(Object)this).getFont(),
               MessagingUtils.miniMessage(
                       "<gray><bold>Did you know?</bold></gray>"
               ),
               ((ConnectScreen) (Object) this).width / 2, ((ConnectScreen) (Object) this).height / 2 + 80, -1
       );
      guiGraphics.drawCenteredString(
              ((ConnectScreen)(Object)this).getFont(),
              MessagingUtils.miniMessage(
                      "<gray>LSN was released in open beta exactly 826 days ago.</gray>"
              ),
              ((ConnectScreen) (Object) this).width / 2, ((ConnectScreen) (Object) this).height / 2 + 95, -1
      );
       */

   }
}
