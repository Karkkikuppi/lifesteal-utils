package dev.candycup.lifestealutils.ui.editor;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;

/**
 * renders the HUD editor background gradients.
 */
public final class HudEditorBackdrop implements Drawable {
   private static final int SCREEN_TOP_ALPHA_BLACK = 0x80000000;
   private static final int SCREEN_BOTTOM_ALPHA_BLACK = 0x40000000;
   private static final int BLOCK_TOP_ALPHA_BLACK = 0xA0000000;
   private static final int BLOCK_BOTTOM_ALPHA_BLACK = 0x50000000;

   private UiBounds bounds = UiBounds.empty();

   /**
    * creates a new backdrop component.
    *
    * @return the backdrop
    */
   public static HudEditorBackdrop create() {
      return new HudEditorBackdrop();
   }

   private HudEditorBackdrop() {
   }

   /**
    * lays out the backdrop to fill available bounds.
    *
    * @param layoutContext the layout context
    */
   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
   }

   /**
    * renders the background gradients.
    *
    * @param context the render context
    */
   @Override
   public void render(UiContext context) {
      int left = bounds.x();
      int top = bounds.y();
      int right = bounds.x() + bounds.width();
      int bottom = bounds.y() + bounds.height();
      context.graphics().fillGradient(left, top, right, bottom, SCREEN_TOP_ALPHA_BLACK, SCREEN_BOTTOM_ALPHA_BLACK);
      context.graphics().fillGradient(left, top, right, bottom, BLOCK_TOP_ALPHA_BLACK, BLOCK_BOTTOM_ALPHA_BLACK);
   }

   /**
    * handles input updates for the backdrop.
    *
    * @param input the input state
    */
   @Override
   public void handleInput(UiInputState input) {
   }

   /**
    * returns the current bounds.
    *
    * @return the bounds
    */
   @Override
   public UiBounds bounds() {
      return bounds;
   }

   /**
    * returns the preferred size based on available bounds.
    *
    * @param layoutContext the layout context
    * @return preferred size
    */
   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(layoutContext.availableBounds().width(), layoutContext.availableBounds().height());
   }
}
