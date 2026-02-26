package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * renders create alliance dialog content as a modal overlay.
 */
final class AllianceCreateDialog implements Drawable {
   private static final Component TITLE = Component.translatable("lsu.alliances.create.title");
   private static final Component SUBTITLE = Component.translatable("lsu.alliances.create.subtitle");
   private static final Component NAME_LABEL = Component.translatable("lsu.alliances.create.name");
   private static final Component PREFIX_LABEL = Component.translatable("lsu.alliances.create.prefix");
   private static final Component COLOR_LABEL = Component.translatable("lsu.alliances.create.color");
   private static final Component DESCRIPTION_LABEL = Component.translatable("lsu.alliances.create.description");
   private static final Component DESCRIPTION_HINT = Component.translatable("lsu.alliances.create.hint.description");
   private static final Component MOTD_LABEL = Component.translatable("lsu.alliances.create.motd");
   private static final Component MOTD_HINT = Component.translatable("lsu.alliances.create.hint.motd");

   private final Supplier<Component> statusSupplier;
   private final IntSupplier statusColorSupplier;
   private final EditBox nameField;
   private final EditBox prefixField;
   private final EditBox colorField;
   private final MultiLineEditBox descriptionField;
   private final MultiLineEditBox motdField;
   private final AllianceDetailButton createButton;
   private final AllianceDetailButton cancelButton;
   private final BooleanSupplier showExtendedFieldsSupplier;

   private UiBounds bounds = UiBounds.empty();
   private UiBounds contentViewport = UiBounds.empty();
   private int contentWidth;
   private int lineHeight;
   private int scrollOffset;
   private int maxScroll;

   AllianceCreateDialog(
           Supplier<Component> statusSupplier,
           IntSupplier statusColorSupplier,
           EditBox nameField,
           EditBox prefixField,
           EditBox colorField,
           MultiLineEditBox descriptionField,
           MultiLineEditBox motdField,
           AllianceDetailButton createButton,
           AllianceDetailButton cancelButton,
           BooleanSupplier showExtendedFieldsSupplier
   ) {
      this.statusSupplier = statusSupplier;
      this.statusColorSupplier = statusColorSupplier;
      this.nameField = nameField;
      this.prefixField = prefixField;
      this.colorField = colorField;
      this.descriptionField = descriptionField;
      this.motdField = motdField;
      this.createButton = createButton;
      this.cancelButton = cancelButton;
      this.showExtendedFieldsSupplier = showExtendedFieldsSupplier;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      UiBounds available = layoutContext.availableBounds();
      int width = Math.min(AllianceEditStyle.PANEL_WIDTH, Math.max(available.width() - AllianceEditStyle.PANEL_GAP * 2, 0));
      int height = Math.max(available.height() - AllianceEditStyle.PANEL_GAP * 2, 0);
      int x = available.x() + (available.width() - width) / 2;
      int y = available.y() + (available.height() - height) / 2;
      this.bounds = new UiBounds(x, y, width, height);

      Font font = layoutContext.font();
      this.lineHeight = font.lineHeight;
      int textX = bounds.x() + AllianceEditStyle.PANEL_PADDING;
      this.contentWidth = Math.max(bounds.width() - AllianceEditStyle.PANEL_PADDING * 2, 0);

      int titleY = bounds.y() + AllianceEditStyle.PANEL_PADDING;
      int subtitleY = titleY + lineHeight + AllianceEditStyle.PANEL_GAP;

      int buttonsY = bounds.y() + bounds.height() - AllianceEditStyle.PANEL_PADDING - AllianceDetailStyle.BUTTON_HEIGHT;
      int statusY = buttonsY - AllianceEditStyle.PANEL_GAP - lineHeight;
      int contentTop = subtitleY + lineHeight + AllianceEditStyle.PANEL_GAP;
      int contentHeight = Math.max(statusY - AllianceEditStyle.PANEL_GAP - contentTop, 0);
      this.contentViewport = new UiBounds(textX, contentTop, contentWidth, contentHeight);

      this.maxScroll = Math.max(totalContentHeight() - contentViewport.height(), 0);
      this.scrollOffset = clamp(scrollOffset, 0, maxScroll);
      layoutFields();

      UiSize createSize = createButton.preferredSize(layoutContext);
      UiSize cancelSize = cancelButton.preferredSize(layoutContext);
      int totalWidth = createSize.width() + AllianceEditStyle.ACTION_GAP + cancelSize.width();
      int buttonsX = bounds.x() + (bounds.width() - totalWidth) / 2;
      createButton.layout(layoutContext.withBounds(new UiBounds(buttonsX, buttonsY, createSize.width(), AllianceDetailStyle.BUTTON_HEIGHT)));
      cancelButton.layout(layoutContext.withBounds(new UiBounds(buttonsX + createSize.width() + AllianceEditStyle.ACTION_GAP, buttonsY, cancelSize.width(), AllianceDetailStyle.BUTTON_HEIGHT)));
   }

   private int totalContentHeight() {
      boolean showExtended = showExtendedFieldsSupplier.getAsBoolean();
      int total = 0;
      total += blockHeight(1, AllianceEditStyle.FIELD_HEIGHT);
      total += blockHeight(1, AllianceEditStyle.FIELD_HEIGHT);
      total += blockHeight(1, AllianceEditStyle.FIELD_HEIGHT);
      if (showExtended) {
         total += blockHeight(2, AllianceEditStyle.FIELD_HEIGHT_LONG);
         total += blockHeight(2, AllianceEditStyle.FIELD_HEIGHT_LONG);
      }
      return total;
   }

   private int blockHeight(int labelLines, int fieldHeight) {
      return labelLines * (lineHeight + AllianceEditStyle.PANEL_GAP) + fieldHeight + AllianceEditStyle.PANEL_GAP;
   }

   private void layoutFields() {
      boolean showExtended = showExtendedFieldsSupplier.getAsBoolean();
      int relativeCursor = 0;
      relativeCursor = layoutField(contentViewport, contentWidth, relativeCursor, nameField, AllianceEditStyle.FIELD_HEIGHT, 1);
      relativeCursor = layoutField(contentViewport, contentWidth, relativeCursor, prefixField, AllianceEditStyle.FIELD_HEIGHT, 1);
      relativeCursor = layoutField(contentViewport, contentWidth, relativeCursor, colorField, AllianceEditStyle.FIELD_HEIGHT, 1);
      if (showExtended) {
         relativeCursor = layoutField(contentViewport, contentWidth, relativeCursor, descriptionField, AllianceEditStyle.FIELD_HEIGHT_LONG, 2);
         layoutField(contentViewport, contentWidth, relativeCursor, motdField, AllianceEditStyle.FIELD_HEIGHT_LONG, 2);
      } else {
         descriptionField.visible = false;
         motdField.visible = false;
      }
   }

   private int layoutField(
           UiBounds viewport,
           int contentWidth,
           int relativeCursor,
           EditBox field,
           int height,
           int labelLines
   ) {
      int fieldRelativeY = relativeCursor + labelLines * (lineHeight + AllianceEditStyle.PANEL_GAP);
      int fieldY = viewport.y() + fieldRelativeY - scrollOffset;
      field.setX(viewport.x());
      field.setY(fieldY);
      field.setWidth(contentWidth);
      field.setHeight(height);
      field.visible = fullyInside(fieldY, height, viewport);
      return fieldRelativeY + height + AllianceEditStyle.PANEL_GAP;
   }

   private int layoutField(
           UiBounds viewport,
           int contentWidth,
           int relativeCursor,
           MultiLineEditBox field,
           int height,
           int labelLines
   ) {
      int fieldRelativeY = relativeCursor + labelLines * (lineHeight + AllianceEditStyle.PANEL_GAP);
      int fieldY = viewport.y() + fieldRelativeY - scrollOffset;
      field.setX(viewport.x());
      field.setY(fieldY);
      field.setWidth(contentWidth);
      field.setHeight(height);
      field.visible = fullyInside(fieldY, height, viewport);
      return fieldRelativeY + height + AllianceEditStyle.PANEL_GAP;
   }

   private boolean fullyInside(int y, int height, UiBounds viewport) {
      int viewportTop = viewport.y();
      int viewportBottom = viewport.y() + viewport.height();
      return y >= viewportTop && y + height <= viewportBottom;
   }

   @Override
   public void render(UiContext context) {
      AllianceEditStyle.renderPanel(context.graphics(), bounds);

      Font font = context.minecraft().font;
      int textX = bounds.x() + AllianceEditStyle.PANEL_PADDING;
      int titleY = bounds.y() + AllianceEditStyle.PANEL_PADDING;
      int subtitleY = titleY + font.lineHeight + AllianceEditStyle.PANEL_GAP;
      int buttonsY = bounds.y() + bounds.height() - AllianceEditStyle.PANEL_PADDING - AllianceDetailStyle.BUTTON_HEIGHT;
      int statusY = buttonsY - AllianceEditStyle.PANEL_GAP - font.lineHeight;

      context.graphics().drawString(font, TITLE, textX, titleY, AllianceEditStyle.TEXT_PRIMARY, true);
      context.graphics().drawString(font, SUBTITLE, textX, subtitleY, AllianceEditStyle.TEXT_MUTED, false);

      renderContent(context, font);

      Component status = statusSupplier.get();
      if (status != null && !status.getString().isEmpty()) {
         context.graphics().drawString(font, status, textX, statusY, statusColorSupplier.getAsInt(), true);
      }

      createButton.render(context);
      cancelButton.render(context);
   }

   private void renderContent(UiContext context, Font font) {
      boolean showExtended = showExtendedFieldsSupplier.getAsBoolean();
      context.graphics().enableScissor(
              contentViewport.x(),
              contentViewport.y(),
              contentViewport.x() + contentViewport.width(),
              contentViewport.y() + contentViewport.height()
      );

      int textX = contentViewport.x();
      int cursorY = contentViewport.y() - scrollOffset;
      cursorY = renderLabelAndAdvance(context, font, textX, cursorY, NAME_LABEL, AllianceEditStyle.TEXT_PRIMARY);
      cursorY += AllianceEditStyle.FIELD_HEIGHT + AllianceEditStyle.PANEL_GAP;

      cursorY = renderLabelAndAdvance(context, font, textX, cursorY, PREFIX_LABEL, AllianceEditStyle.TEXT_PRIMARY);
      cursorY += AllianceEditStyle.FIELD_HEIGHT + AllianceEditStyle.PANEL_GAP;

      cursorY = renderLabelAndAdvance(context, font, textX, cursorY, COLOR_LABEL, AllianceEditStyle.TEXT_PRIMARY);
      cursorY += AllianceEditStyle.FIELD_HEIGHT + AllianceEditStyle.PANEL_GAP;

      if (showExtended) {
         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, DESCRIPTION_LABEL, AllianceEditStyle.TEXT_PRIMARY);
         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, DESCRIPTION_HINT, AllianceEditStyle.TEXT_MUTED);
         cursorY += AllianceEditStyle.FIELD_HEIGHT_LONG + AllianceEditStyle.PANEL_GAP;

         cursorY = renderLabelAndAdvance(context, font, textX, cursorY, MOTD_LABEL, AllianceEditStyle.TEXT_PRIMARY);
         renderLabelAndAdvance(context, font, textX, cursorY, MOTD_HINT, AllianceEditStyle.TEXT_MUTED);
      }

      context.graphics().disableScissor();
   }

   private int renderLabelAndAdvance(UiContext context, Font font, int textX, int cursorY, Component label, int color) {
      context.graphics().drawString(font, label, textX, cursorY, color, false);
      return cursorY + font.lineHeight + AllianceEditStyle.PANEL_GAP;
   }

   @Override
   public void handleInput(UiInputState input) {
      if (input.isHovering(contentViewport)) {
         double scrollDelta = input.scrollY();
         if (scrollDelta != 0) {
            int delta = (int) Math.round(scrollDelta * -AlliancesListStyle.SCROLL_STEP);
            int nextScroll = clamp(scrollOffset + delta, 0, maxScroll);
            if (nextScroll != scrollOffset) {
               scrollOffset = nextScroll;
               layoutFields();
            }
         }
      }

      createButton.handleInput(input);
      cancelButton.handleInput(input);
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(AllianceEditStyle.PANEL_WIDTH, layoutContext.availableBounds().height());
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   void resetScroll() {
      this.scrollOffset = 0;
      this.maxScroll = 0;
   }
}
