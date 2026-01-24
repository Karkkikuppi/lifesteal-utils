package dev.candycup.lifestealutils.hud;

import dev.candycup.lifestealutils.LifestealServerDetector;
import dev.candycup.lifestealutils.ui.HudElementEditor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class HudDisplayLayer {
   public static final Identifier LSU_HUD_LAYER_ID = Identifier.fromNamespaceAndPath("lifestealutils", "lsu_hud_layer");

   private HudDisplayLayer() {
   }

   public static HudElement lsuHudLayer() {
      return (drawContext, tickCounter) -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (!LifestealServerDetector.isOnLifestealServer()) {
            return;
         }
         if (minecraft.screen instanceof HudElementEditor) {
            return;
         }
         int guiWidth = minecraft.getWindow().getGuiScaledWidth();
         int guiHeight = minecraft.getWindow().getGuiScaledHeight();
         for (HudElementManager.RenderedHudElement element : HudElementManager.renderables(minecraft.font, guiWidth, guiHeight)) {
            drawContext.drawString(minecraft.font, element.component(), element.x(), element.y(), 0xFFFFFFFF, true);
         }
      };
   }
}
