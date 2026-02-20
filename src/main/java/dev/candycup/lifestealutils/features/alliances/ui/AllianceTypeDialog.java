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
import java.util.function.BooleanSupplier;

final class AllianceTypeDialog implements Drawable {
   private static final Component TITLE = Component.translatable("lsu.alliances.type.title");
   private static final Component QUESTION = Component.translatable("lsu.alliances.type.question");
   private static final Component LOCAL_INFO = Component.translatable("lsu.alliances.type.local_info");
   private static final Component MODERN_INFO = Component.translatable("lsu.alliances.type.modern_info");
   private static final Component MODERN_DISABLED = Component.translatable("lsu.alliances.type.modern_disabled");
   private static final Component LOCAL_BUTTON = Component.translatable("lsu.alliances.type.local_button");
   private static final Component MODERN_BUTTON = Component.translatable("lsu.alliances.type.modern_button");

   private final AllianceDetailButton localButton;
   private final AllianceDetailButton modernButton;
   private final BooleanSupplier modernEnabledSupplier;

   private UiBounds bounds = UiBounds.empty();
   private List<FormattedCharSequence> questionLines = List.of();
   private List<FormattedCharSequence> localInfoLines = List.of();
   private List<FormattedCharSequence> modernInfoLines = List.of();
   private List<FormattedCharSequence> disabledLines = List.of();

   AllianceTypeDialog(Runnable onLocal, Runnable onModern, BooleanSupplier modernEnabledSupplier) {
      this.modernEnabledSupplier = modernEnabledSupplier;
      this.localButton = AllianceDetailButton.secondaryWithWidth(
              () -> LOCAL_BUTTON,
              onLocal,
              () -> true,
              AllianceDetailStyle.DIALOG_BUTTON_WIDTH
      );
      this.modernButton = AllianceDetailButton.primaryWithWidth(
              () -> MODERN_BUTTON,
              onModern,
              () -> modernEnabledSupplier.getAsBoolean(),
              AllianceDetailStyle.DIALOG_BUTTON_WIDTH
      );
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      Font font = layoutContext.font();
      int contentWidth = AllianceDetailStyle.DIALOG_WIDTH - AllianceDetailStyle.DIALOG_PADDING * 2;

      this.questionLines = font.split(QUESTION, contentWidth);
      this.localInfoLines = font.split(LOCAL_INFO, contentWidth);
      this.modernInfoLines = font.split(MODERN_INFO, contentWidth);
      this.disabledLines = modernEnabledSupplier.getAsBoolean() ? List.of() : font.split(MODERN_DISABLED, contentWidth);

      int textLines = 1 + questionLines.size() + localInfoLines.size() + modernInfoLines.size() + disabledLines.size();
      int textHeight = textLines * font.lineHeight;
      int height = AllianceDetailStyle.DIALOG_PADDING * 2
              + textHeight
              + AllianceDetailStyle.DIALOG_TEXT_GAP * 4
              + AllianceDetailStyle.BUTTON_HEIGHT;

      UiBounds available = layoutContext.availableBounds();
      int x = available.x() + (available.width() - AllianceDetailStyle.DIALOG_WIDTH) / 2;
      int y = available.y() + (available.height() - height) / 2;
      this.bounds = new UiBounds(x, y, AllianceDetailStyle.DIALOG_WIDTH, height);

      int buttonsY = bounds.y() + bounds.height() - AllianceDetailStyle.DIALOG_PADDING - AllianceDetailStyle.BUTTON_HEIGHT;
      int totalWidth = AllianceDetailStyle.DIALOG_BUTTON_WIDTH * 2 + AllianceDetailStyle.DIALOG_BUTTON_GAP;
      int leftX = bounds.x() + (bounds.width() - totalWidth) / 2;
      int rightX = leftX + AllianceDetailStyle.DIALOG_BUTTON_WIDTH + AllianceDetailStyle.DIALOG_BUTTON_GAP;

      modernButton.layout(layoutContext.withBounds(new UiBounds(leftX, buttonsY, AllianceDetailStyle.DIALOG_BUTTON_WIDTH, AllianceDetailStyle.BUTTON_HEIGHT)));
      localButton.layout(layoutContext.withBounds(new UiBounds(rightX, buttonsY, AllianceDetailStyle.DIALOG_BUTTON_WIDTH, AllianceDetailStyle.BUTTON_HEIGHT)));
   }

   @Override
   public void render(UiContext context) {
      AlliancesListStyle.renderBackgroundSprite(context.graphics(), bounds);
      context.graphics().fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + 1, AllianceDetailStyle.DIALOG_BORDER);
      context.graphics().fill(bounds.x(), bounds.y() + bounds.height() - 1, bounds.x() + bounds.width(), bounds.y() + bounds.height(), AllianceDetailStyle.DIALOG_BORDER);
      context.graphics().fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.y() + bounds.height(), AllianceDetailStyle.DIALOG_BORDER);
      context.graphics().fill(bounds.x() + bounds.width() - 1, bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), AllianceDetailStyle.DIALOG_BORDER);

      Font font = context.minecraft().font;
      int textX = bounds.x() + AllianceDetailStyle.DIALOG_PADDING;
      int cursorY = bounds.y() + AllianceDetailStyle.DIALOG_PADDING;

      context.graphics().drawString(font, TITLE, textX, cursorY, AllianceDetailStyle.TEXT_PRIMARY, true);
      cursorY += font.lineHeight + AllianceDetailStyle.DIALOG_TEXT_GAP;

      cursorY = renderLines(context, font, questionLines, textX, cursorY, AllianceDetailStyle.TEXT_MUTED);
      cursorY += AllianceDetailStyle.DIALOG_TEXT_GAP;
      cursorY = renderLines(context, font, localInfoLines, textX, cursorY, AllianceDetailStyle.TEXT_PRIMARY);
      cursorY += AllianceDetailStyle.DIALOG_TEXT_GAP;
      cursorY = renderLines(context, font, modernInfoLines, textX, cursorY, AllianceDetailStyle.TEXT_PRIMARY);

      if (!disabledLines.isEmpty()) {
         cursorY += AllianceDetailStyle.DIALOG_TEXT_GAP;
         renderLines(context, font, disabledLines, textX, cursorY, AllianceDetailStyle.TEXT_ERROR);
      }

      localButton.render(context);
      modernButton.render(context);
   }

   private int renderLines(UiContext context, Font font, List<FormattedCharSequence> lines, int x, int y, int color) {
      int cursor = y;
      for (FormattedCharSequence line : lines) {
         context.graphics().drawString(font, line, x, cursor, color, false);
         cursor += font.lineHeight;
      }
      return cursor;
   }

   @Override
   public void handleInput(UiInputState input) {
      localButton.handleInput(input);
      modernButton.handleInput(input);
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
