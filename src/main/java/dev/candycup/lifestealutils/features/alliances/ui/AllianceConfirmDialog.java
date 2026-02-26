package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * renders a modal confirmation dialog.
 */
final class AllianceConfirmDialog implements Drawable {
   private final Component title;
   private final Component message;
   private final AllianceDetailButton confirmButton;
   private final AllianceDetailButton cancelButton;

   private UiBounds bounds = UiBounds.empty();
   private List<FormattedCharSequence> messageLines = List.of();

   AllianceConfirmDialog(Component title, Component message, Component confirmLabel, Runnable onConfirm, Component cancelLabel, Runnable onCancel) {
      this.title = title;
      this.message = message;
      this.confirmButton = AllianceDetailButton.primaryWithWidth(
              () -> confirmLabel,
              onConfirm,
              () -> true,
              AllianceDetailStyle.DIALOG_BUTTON_WIDTH
      );
      this.cancelButton = AllianceDetailButton.secondaryWithWidth(
              () -> cancelLabel,
              onCancel,
              () -> true,
              AllianceDetailStyle.DIALOG_BUTTON_WIDTH
      );
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      Font font = layoutContext.font();
      this.messageLines = font.split(message, AllianceDetailStyle.DIALOG_WIDTH - AllianceDetailStyle.DIALOG_PADDING * 2);
      int textHeight = messageLines.size() * font.lineHeight;
      int titleHeight = font.lineHeight;

      int height = AllianceDetailStyle.DIALOG_PADDING * 2 + titleHeight + AllianceDetailStyle.DIALOG_TEXT_GAP + textHeight
              + AllianceDetailStyle.DIALOG_TEXT_GAP + AllianceDetailStyle.BUTTON_HEIGHT;

      UiBounds available = layoutContext.availableBounds();
      int x = available.x() + (available.width() - AllianceDetailStyle.DIALOG_WIDTH) / 2;
      int y = available.y() + (available.height() - height) / 2;
      this.bounds = new UiBounds(x, y, AllianceDetailStyle.DIALOG_WIDTH, height);

      int buttonsY = bounds.y() + bounds.height() - AllianceDetailStyle.DIALOG_PADDING - AllianceDetailStyle.BUTTON_HEIGHT;
      int totalWidth = AllianceDetailStyle.DIALOG_BUTTON_WIDTH * 2 + AllianceDetailStyle.DIALOG_BUTTON_GAP;
      int leftX = bounds.x() + (bounds.width() - totalWidth) / 2;
      int rightX = leftX + AllianceDetailStyle.DIALOG_BUTTON_WIDTH + AllianceDetailStyle.DIALOG_BUTTON_GAP;

      confirmButton.layout(layoutContext.withBounds(new UiBounds(leftX, buttonsY, AllianceDetailStyle.DIALOG_BUTTON_WIDTH, AllianceDetailStyle.BUTTON_HEIGHT)));
      cancelButton.layout(layoutContext.withBounds(new UiBounds(rightX, buttonsY, AllianceDetailStyle.DIALOG_BUTTON_WIDTH, AllianceDetailStyle.BUTTON_HEIGHT)));
   }

   @Override
   public void render(UiContext context) {
      AlliancesListStyle.renderBackgroundSprite(context.graphics(), bounds);
      context.graphics().fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + 1, AllianceDetailStyle.DIALOG_BORDER);
      context.graphics().fill(bounds.x(), bounds.y() + bounds.height() - 1, bounds.x() + bounds.width(), bounds.y() + bounds.height(), AllianceDetailStyle.DIALOG_BORDER);
      context.graphics().fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.y() + bounds.height(), AllianceDetailStyle.DIALOG_BORDER);
      context.graphics().fill(bounds.x() + bounds.width() - 1, bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), AllianceDetailStyle.DIALOG_BORDER);

      Font font = context.minecraft().font;
      int cursorY = bounds.y() + AllianceDetailStyle.DIALOG_PADDING;
      context.graphics().drawString(font, title, bounds.x() + AllianceDetailStyle.DIALOG_PADDING, cursorY, AllianceDetailStyle.TEXT_PRIMARY, true);
      cursorY += font.lineHeight + AllianceDetailStyle.DIALOG_TEXT_GAP;

      for (FormattedCharSequence line : messageLines) {
         context.graphics().drawString(font, line, bounds.x() + AllianceDetailStyle.DIALOG_PADDING, cursorY, AllianceDetailStyle.TEXT_MUTED, false);
         cursorY += font.lineHeight;
      }

      confirmButton.render(context);
      cancelButton.render(context);
   }

   @Override
   public void handleInput(UiInputState input) {
      confirmButton.handleInput(input);
      cancelButton.handleInput(input);
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(AllianceDetailStyle.DIALOG_WIDTH, bounds.height());
   }
}
