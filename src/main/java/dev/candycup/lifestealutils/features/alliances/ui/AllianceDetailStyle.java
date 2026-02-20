package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * shared styling values for the alliance detail ui.
 */
final class AllianceDetailStyle {
   static final int OUTER_PADDING = AlliancesListStyle.OUTER_PADDING;
   static final int HEADER_HEIGHT = AlliancesListStyle.HEADER_HEIGHT;
   static final int HEADER_PADDING_X = AlliancesListStyle.HEADER_PADDING_X;
   static final int HEADER_STEP_SIZE = AlliancesListStyle.HEADER_STEP_SIZE;
   static final int STATUS_HEIGHT = 14;
   static final int SECTION_GAP = 8;
   static final int CONTENT_PADDING = AlliancesListStyle.CONTENT_PADDING;
   static final int PANEL_GAP = 8;
   static final int TAB_HEIGHT = 20;
   static final int TAB_WIDTH = 120;
   static final int TAB_MIN_WIDTH = 64;
   static final int TAB_GAP = 8;
   static final int ACTION_ROW_HEIGHT = 32;

   static final int ENTRY_GAP = 8;
   static final int ENTRY_PADDING = 10;
   static final int CARD_SECTION_GAP = 6;
   static final int BADGE_PADDING_X = AlliancesListStyle.BADGE_PADDING_X;
   static final int BADGE_PADDING_Y = AlliancesListStyle.BADGE_PADDING_Y;
   static final int ACCENT_BAR_WIDTH = 4;
   static final int INFO_ACCENT_COLOR = 0xFF8A6BFF;
   static final int MEMBER_ACCENT_COLOR = 0xFF5BC0BE;

   static final int BUTTON_HEIGHT = 20;
   static final int BUTTON_WIDTH = 140;
   static final int BUTTON_MIN_WIDTH = 72;
   static final int BUTTON_SMALL_WIDTH = 90;
   static final int BUTTON_INVITE_WIDTH = 120;
   static final int BUTTON_EDIT_WIDTH = 80;
   static final int BUTTON_EDIT_MIN_WIDTH = 48;
   static final int BUTTON_PADDING_X = 10;
   static final int BUTTON_PADDING_Y = 4;
   static final int BUTTON_STEP_SIZE = 2;
   static final int BUTTON_SHADOW_OFFSET = 2;

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

   static final int HEADER_BG_COLOR = AlliancesListStyle.HEADER_BG_COLOR;
   static final int CONTENT_BG_COLOR = AlliancesListStyle.CONTENT_BG_COLOR;
   static final int CARD_BG_COLOR = AlliancesListStyle.CARD_BG_COLOR;
   static final int CARD_BG_HOVER = AlliancesListStyle.CARD_BG_HOVER;
   static final int CARD_BORDER_COLOR = AlliancesListStyle.CARD_BORDER_COLOR;
   static final int BADGE_BG_COLOR = AlliancesListStyle.BADGE_BG_COLOR;
   static final int BADGE_BORDER_COLOR = AlliancesListStyle.BADGE_BORDER_COLOR;
   static final int INVITE_BADGE_BG_COLOR = AlliancesListStyle.INVITE_BADGE_BG_COLOR;
   static final int INVITE_BADGE_BORDER_COLOR = AlliancesListStyle.INVITE_BADGE_BORDER_COLOR;
   static final int SECTION_BG_COLOR = AlliancesListStyle.SECTION_BG_COLOR;
   static final int SECTION_BORDER_COLOR = AlliancesListStyle.SECTION_BORDER_COLOR;

   static final int TEXT_PRIMARY = AlliancesListStyle.TEXT_PRIMARY;
   static final int TEXT_MUTED = AlliancesListStyle.TEXT_MUTED;
   static final int TEXT_WARNING = AlliancesListStyle.TEXT_WARNING;
   static final int TEXT_ERROR = AlliancesListStyle.TEXT_ERROR;
   static final int TEXT_INVITE = AlliancesListStyle.TEXT_INVITE;
   static final int TEXT_POSITIVE = 0xFF55FF55;
   static final int TEXT_PENDING = 0xFFFFAA00;

   static final int BUTTON_TEXT = AlliancesListStyle.BUTTON_TEXT;
   static final int BUTTON_TEXT_HOVER = AlliancesListStyle.BUTTON_TEXT_HOVER;
   static final int BUTTON_TEXT_DISABLED = AlliancesListStyle.BUTTON_TEXT_DISABLED;
   static final int BUTTON_BG = AlliancesListStyle.BUTTON_BG;
   static final int BUTTON_BG_HOVER = AlliancesListStyle.BUTTON_BG_HOVER;
   static final int BUTTON_BG_DISABLED = AlliancesListStyle.BUTTON_BG_DISABLED;
   static final int BUTTON_SHADOW_COLOR = AlliancesListStyle.BUTTON_SHADOW_COLOR;
   static final int SECONDARY_BG = AlliancesListStyle.SECONDARY_BG;
   static final int SECONDARY_BG_HOVER = AlliancesListStyle.SECONDARY_BG_HOVER;
   static final int SECONDARY_BG_DISABLED = AlliancesListStyle.SECONDARY_BG_DISABLED;

   static final int OVERLAY_COLOR = 0xAA000000;
   static final int DIALOG_BG = 0xFF2E2E35;
   static final int DIALOG_BORDER = 0xFF3B3B42;
   static final int DIALOG_PADDING = 12;
   static final int DIALOG_WIDTH = 280;
   static final int DIALOG_BUTTON_WIDTH = 124;
   static final int DIALOG_BUTTON_GAP = 8;
   static final int DIALOG_TEXT_GAP = 6;

   private AllianceDetailStyle() {
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
      AlliancesListStyle.renderSteppedRect(graphics, bounds, color, stepSize);
   }
}
