package dev.candycup.lifestealutils.hud;

import dev.candycup.lifestealutils.api.LifestealServerDetector;
import dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator;
import dev.candycup.lifestealutils.ui.HudElementEditor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class HudDisplayLayer {
   public static final Identifier LSU_HUD_LAYER_ID = Identifier.fromNamespaceAndPath("lifestealutils", "lsu_hud_layer");

   private static PoiDirectionalIndicator poiDirectionalIndicator;

   private HudDisplayLayer() {
   }

   /**
    * Sets the POI directional indicator instance for rendering.
    *
    * @param indicator the directional indicator to render
    */
   public static void setPoiDirectionalIndicator(PoiDirectionalIndicator indicator) {
      poiDirectionalIndicator = indicator;
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

         // render the directional indicator first (behind text)
         if (poiDirectionalIndicator != null) {
            poiDirectionalIndicator.ensurePositionRegistered(guiWidth, guiHeight);
            poiDirectionalIndicator.render(drawContext, guiWidth, guiHeight);
         }

         // render text-based HUD elements
         for (HudElementManager.RenderedHudElement element : HudElementManager.renderables(minecraft.font, guiWidth, guiHeight)) {
            drawContext.drawString(minecraft.font, element.component(), element.x(), element.y(), 0xFFFFFFFF, true);
         }
      };
   }
}
