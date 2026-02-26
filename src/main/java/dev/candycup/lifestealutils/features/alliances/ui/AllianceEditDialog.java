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

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * renders an edit dialog overlaying the alliance detail screen.
 */
final class AllianceEditDialog implements Drawable {
   private final Supplier<Component> titleSupplier;
   private final Supplier<Component> subtitleSupplier;
   private final Supplier<Component> statusSupplier;
   private final IntSupplier statusColorSupplier;
   private final EditBox shortField;
   private final MultiLineEditBox longField;
   private final BooleanSupplier useShortFieldSupplier;
   private final AllianceDetailButton saveButton;
   private final AllianceDetailButton cancelButton;

   private UiBounds bounds = UiBounds.empty();

   AllianceEditDialog(
           Supplier<Component> titleSupplier,
           Supplier<Component> subtitleSupplier,
           Supplier<Component> statusSupplier,
           IntSupplier statusColorSupplier,
           EditBox shortField,
           MultiLineEditBox longField,
           BooleanSupplier useShortFieldSupplier,
           AllianceDetailButton saveButton,
           AllianceDetailButton cancelButton
   ) {
      this.titleSupplier = titleSupplier;
      this.subtitleSupplier = subtitleSupplier;
      this.statusSupplier = statusSupplier;
      this.statusColorSupplier = statusColorSupplier;
      this.shortField = shortField;
      this.longField = longField;
      this.useShortFieldSupplier = useShortFieldSupplier;
      this.saveButton = saveButton;
      this.cancelButton = cancelButton;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      UiBounds available = layoutContext.availableBounds();
      int width = Math.min(AllianceEditStyle.PANEL_WIDTH, Math.max(available.width() - AllianceEditStyle.PANEL_GAP * 2, 0));
      int height = Math.min(calculateHeight(layoutContext.font()), Math.max(available.height() - AllianceEditStyle.PANEL_GAP * 2, 0));
      int x = available.x() + (available.width() - width) / 2;
      int y = available.y() + (available.height() - height) / 2;
      this.bounds = new UiBounds(x, y, width, height);

      Font font = layoutContext.font();
      int cursorY = bounds.y() + AllianceEditStyle.PANEL_PADDING;
      int textX = bounds.x() + AllianceEditStyle.PANEL_PADDING;
      int contentWidth = bounds.width() - AllianceEditStyle.PANEL_PADDING * 2;

      cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;
      cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

      boolean useShortField = useShortFieldSupplier.getAsBoolean();
      if (useShortField) {
         shortField.setX(textX);
         shortField.setY(cursorY);
         shortField.setWidth(contentWidth);
         shortField.setHeight(AllianceEditStyle.FIELD_HEIGHT);
         shortField.visible = true;
         longField.visible = false;
         cursorY += AllianceEditStyle.FIELD_HEIGHT;
      } else {
         longField.setX(textX);
         longField.setY(cursorY);
         longField.setWidth(contentWidth);
         longField.setHeight(AllianceEditStyle.FIELD_HEIGHT_LONG);
         longField.visible = true;
         shortField.visible = false;
         cursorY += AllianceEditStyle.FIELD_HEIGHT_LONG;
      }

      cursorY += AllianceEditStyle.PANEL_GAP;
      cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

      int buttonsY = bounds.y() + bounds.height() - AllianceEditStyle.PANEL_PADDING - AllianceDetailStyle.BUTTON_HEIGHT;
      UiSize saveSize = saveButton.preferredSize(layoutContext);
      UiSize cancelSize = cancelButton.preferredSize(layoutContext);
      int totalWidth = saveSize.width() + AllianceEditStyle.ACTION_GAP + cancelSize.width();
      int buttonsX = bounds.x() + (bounds.width() - totalWidth) / 2;
      saveButton.layout(layoutContext.withBounds(new UiBounds(buttonsX, buttonsY, saveSize.width(), AllianceDetailStyle.BUTTON_HEIGHT)));
      cancelButton.layout(layoutContext.withBounds(new UiBounds(buttonsX + saveSize.width() + AllianceEditStyle.ACTION_GAP, buttonsY, cancelSize.width(), AllianceDetailStyle.BUTTON_HEIGHT)));
   }

   @Override
   public void render(UiContext context) {
      AllianceEditStyle.renderPanel(context.graphics(), bounds);

      Font font = context.minecraft().font;
      int cursorY = bounds.y() + AllianceEditStyle.PANEL_PADDING;
      int textX = bounds.x() + AllianceEditStyle.PANEL_PADDING;

      Component title = titleSupplier.get();
      context.graphics().drawString(font, title, textX, cursorY, AllianceEditStyle.TEXT_PRIMARY, true);
      cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

      Component subtitle = subtitleSupplier.get();
      context.graphics().drawString(font, subtitle, textX, cursorY, AllianceEditStyle.TEXT_MUTED, false);
      cursorY += font.lineHeight + AllianceEditStyle.PANEL_GAP;

      cursorY += useShortFieldSupplier.getAsBoolean() ? AllianceEditStyle.FIELD_HEIGHT : AllianceEditStyle.FIELD_HEIGHT_LONG;
      cursorY += AllianceEditStyle.PANEL_GAP;

      Component status = statusSupplier.get();
      if (status != null && !status.getString().isEmpty()) {
         context.graphics().drawString(font, status, textX, cursorY, statusColorSupplier.getAsInt(), true);
      }

      saveButton.render(context);
      cancelButton.render(context);
   }

   @Override
   public void handleInput(UiInputState input) {
      saveButton.handleInput(input);
      cancelButton.handleInput(input);
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(AllianceEditStyle.PANEL_WIDTH, calculateHeight(layoutContext.font()));
   }

   private int calculateHeight(Font font) {
      int fieldHeight = useShortFieldSupplier.getAsBoolean() ? AllianceEditStyle.FIELD_HEIGHT : AllianceEditStyle.FIELD_HEIGHT_LONG;
      return AllianceEditStyle.PANEL_PADDING * 2
              + font.lineHeight
              + AllianceEditStyle.PANEL_GAP
              + font.lineHeight
              + AllianceEditStyle.PANEL_GAP
              + fieldHeight
              + AllianceEditStyle.PANEL_GAP
              + font.lineHeight
              + AllianceEditStyle.PANEL_GAP
              + AllianceDetailStyle.BUTTON_HEIGHT;
   }
}
