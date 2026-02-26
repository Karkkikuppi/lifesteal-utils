package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * shared styling values for the alliances list ui.
 */
final class AlliancesListStyle {
   static final int OUTER_PADDING = 16;
   static final int HEADER_HEIGHT = 38;
   static final int HEADER_PADDING_X = 12;
   static final int HEADER_STEP_SIZE = 2;
   static final int SECTION_GAP = 8;
   static final int CONTENT_PADDING = 12;
   static final int FOOTER_HEIGHT = 32;
   static final int FOOTER_PADDING = 6;

   static final int ENTRY_GAP = 8;
   static final int SECTION_PADDING_X = 8;
   static final int SECTION_PADDING_Y = 4;
   static final int CARD_PADDING = 10;
   static final int CARD_SECTION_GAP = 6;
   static final int BADGE_PADDING_X = 6;
   static final int BADGE_PADDING_Y = 2;
   static final int BUTTON_PADDING_X = 12;
   static final int BUTTON_PADDING_Y = 6;
   static final int BUTTON_MIN_WIDTH = 64;
   static final int BUTTON_STEP_SIZE = 2;
   static final int BUTTON_SHADOW_OFFSET = 2;
   static final int SCROLL_STEP = 18;

   static final int HEADER_BG_COLOR = 0xFF3F3F46;
   static final int CONTENT_BG_COLOR = 0xFF27272A;
   static final int SECTION_BG_COLOR = 0xFF31313A;
   static final int SECTION_BORDER_COLOR = 0xFF3B3B43;
   static final int CARD_BG_COLOR = 0xFF2F2F35;
   static final int CARD_BG_HOVER = 0xFF36363D;
   static final int CARD_BORDER_COLOR = 0xFF3B3B42;
   static final int BADGE_BG_COLOR = 0xFF34343B;
   static final int BADGE_BORDER_COLOR = 0xFF42424A;
   static final int INVITE_BADGE_BG_COLOR = 0xFF4A3B24;
   static final int INVITE_BADGE_BORDER_COLOR = 0xFF5A4A34;

   static final int TEXT_PRIMARY = 0xFFFFFFFF;
   static final int TEXT_MUTED = 0xFFB0B0B0;
   static final int TEXT_WARNING = 0xFFFFAA00;
   static final int TEXT_ERROR = 0xFFFF6666;
   static final int TEXT_INVITE = 0xFFFFD58A;

   static final int BUTTON_TEXT = 0xFFE6E6E6;
   static final int BUTTON_TEXT_HOVER = 0xFFFFFFFF;
   static final WidgetSprites BUTTON_PRIMARY_SPRITES = new WidgetSprites(
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_primary/button"),
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_primary/button_disabled"),
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_primary/button_highlighted")
   );
   static final WidgetSprites BUTTON_SECONDARY_SPRITES = new WidgetSprites(
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_secondary/button"),
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_secondary/button_disabled"),
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_secondary/button_highlighted")
   );
   static final Identifier HEADER_BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath("lifestealutils", "background/tile_2");
   static final Identifier BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath("lifestealutils", "background/tile_1");
   static final Identifier BOX_BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath("lifestealutils", "background/tile_3");
   static final int BUTTON_BG = 0xFF3471EB;
   static final int BUTTON_BG_HOVER = 0xFF3D7CF0;
   static final int BUTTON_BG_DISABLED = 0xFF2A4E91;
   static final int BUTTON_SHADOW_COLOR = 0xFF234D9F;
   static final int SECONDARY_BG = 0xFF3B3B43;
   static final int SECONDARY_BG_HOVER = 0xFF464650;
   static final int SECONDARY_BG_DISABLED = 0xFF2E2E35;
   static final int BUTTON_TEXT_DISABLED = 0xFF8E8E96;

   private AlliancesListStyle() {
   }

   /**
    * renders a stepped rectangle with square corner cuts.
    *
    * @param graphics the gui graphics
    * @param bounds   the area to fill
    * @param color    the fill color
    * @param stepSize the corner step size
    */
   static void renderSteppedRect(GuiGraphics graphics, UiBounds bounds, int color, int stepSize) {
      int x = bounds.x();
      int y = bounds.y();
      int width = bounds.width();
      int height = bounds.height();

      int step = Math.min(stepSize, Math.min(width / 3, height / 2));

      graphics.fill(x + step, y, x + width - step, y + step, color);
      graphics.fill(x, y + step, x + width, y + height - step, color);
      graphics.fill(x + step, y + height - step, x + width - step, y + height, color);
   }

   static void renderBackgroundSprite(GuiGraphics graphics, UiBounds bounds) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, bounds.x(), bounds.y(), bounds.width(), bounds.height());
   }

   static void renderHeaderBackgroundSprite(GuiGraphics graphics, UiBounds bounds) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEADER_BACKGROUND_SPRITE, bounds.x(), bounds.y(), bounds.width(), bounds.height());
   }

   static void renderBoxBackgroundSprite(GuiGraphics graphics, UiBounds bounds) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOX_BACKGROUND_SPRITE, bounds.x(), bounds.y(), bounds.width(), bounds.height());
   }
}
