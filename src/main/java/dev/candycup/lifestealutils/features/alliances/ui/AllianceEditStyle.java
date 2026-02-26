package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import net.minecraft.client.gui.GuiGraphics;

/**
 * shared styling values for the alliance edit ui.
 */
final class AllianceEditStyle {
   static final int PANEL_WIDTH = 360;
   static final int PANEL_PADDING = 16;
   static final int PANEL_GAP = 10;
   static final int FIELD_HEIGHT = 22;
   static final int FIELD_HEIGHT_LONG = 90;
   static final int ACTION_GAP = 8;

   static final int PANEL_BG = AllianceDetailStyle.DIALOG_BG;
   static final int PANEL_BORDER = AllianceDetailStyle.DIALOG_BORDER;

   static final int TEXT_PRIMARY = AlliancesListStyle.TEXT_PRIMARY;
   static final int TEXT_MUTED = AlliancesListStyle.TEXT_MUTED;

   private AllianceEditStyle() {
   }

   static void renderPanel(GuiGraphics graphics, UiBounds bounds) {
      AlliancesListStyle.renderBackgroundSprite(graphics, bounds);
      graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + 1, PANEL_BORDER);
      graphics.fill(bounds.x(), bounds.y() + bounds.height() - 1, bounds.x() + bounds.width(), bounds.y() + bounds.height(), PANEL_BORDER);
      graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.y() + bounds.height(), PANEL_BORDER);
      graphics.fill(bounds.x() + bounds.width() - 1, bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), PANEL_BORDER);
   }
}
