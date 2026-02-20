package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;

import java.util.function.Supplier;

/**
 * overlays a dialog on top of the content when present.
 */
final class AllianceDialogLayer implements Drawable {
   private final Drawable content;
   private final Supplier<Drawable> dialogSupplier;

   private UiBounds bounds = UiBounds.empty();
   private Drawable activeDialog;

   AllianceDialogLayer(Drawable content, Supplier<Drawable> dialogSupplier) {
      this.content = content;
      this.dialogSupplier = dialogSupplier;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      content.layout(layoutContext.withBounds(bounds));
      activeDialog = dialogSupplier.get();
      if (activeDialog != null) {
         int screenWidth = layoutContext.minecraft().getWindow().getGuiScaledWidth();
         int screenHeight = layoutContext.minecraft().getWindow().getGuiScaledHeight();
         UiBounds screenBounds = new UiBounds(0, 0, screenWidth, screenHeight);
         activeDialog.layout(layoutContext.withBounds(screenBounds));
      }
   }

   @Override
   public void render(UiContext context) {
      content.render(context);
      if (activeDialog != null) {
         context.graphics().fill(0, 0, context.width(), context.height(), AllianceDetailStyle.OVERLAY_COLOR);
         activeDialog.render(context);
      }
   }

   @Override
   public void handleInput(UiInputState input) {
      if (activeDialog != null) {
         activeDialog.handleInput(input);
         return;
      }
      content.handleInput(input);
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(layoutContext.availableBounds().width(), layoutContext.availableBounds().height());
   }
}
