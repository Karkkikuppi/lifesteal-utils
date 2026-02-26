package dev.candycup.lifestealutils.ui.editor;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.util.Mth;

/**
 * renders the snapping grid overlay for the HUD editor.
 */
public final class HudEditorGrid implements Drawable {
   private static final int GRID_TOP_ALPHA = 0x40;
   private static final int GRID_BOTTOM_ALPHA = 0x18;
   private static final int GRID_COLOR_MASK = 0x00FFFFFF;

   private final int gridSpacing;

   private UiBounds bounds = UiBounds.empty();

   private HudEditorGrid(Builder builder) {
      this.gridSpacing = builder.gridSpacing;
   }

   /**
    * creates a new builder for the grid component.
    *
    * @return the builder
    */
   public static Builder builder() {
      return new Builder();
   }

   /**
    * lays out the grid to fill available bounds.
    *
    * @param layoutContext the layout context
    */
   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
   }

   /**
    * renders the grid overlay.
    *
    * @param context the render context
    */
   @Override
   public void render(UiContext context) {
      if (gridSpacing <= 0) {
         return;
      }
      int left = bounds.x();
      int top = bounds.y();
      int width = bounds.width();
      int height = bounds.height();
      int centerX = left + width / 2;
      int gridTopColor = (GRID_TOP_ALPHA << 24) | GRID_COLOR_MASK;
      int gridBottomColor = (GRID_BOTTOM_ALPHA << 24) | GRID_COLOR_MASK;

      for (int x = centerX; x < left + width; x += gridSpacing) {
         context.graphics().fillGradient(x, top, x + 1, top + height, gridTopColor, gridBottomColor);
      }
      for (int x = centerX - gridSpacing; x >= left; x -= gridSpacing) {
         context.graphics().fillGradient(x, top, x + 1, top + height, gridTopColor, gridBottomColor);
      }
      for (int y = 0; y < height; y += gridSpacing) {
         float t = height == 0 ? 0F : (float) y / (float) height;
         int alpha = Mth.floor(Mth.lerp(t, GRID_TOP_ALPHA, GRID_BOTTOM_ALPHA));
         int color = (alpha << 24) | GRID_COLOR_MASK;
         context.graphics().fill(left, top + y, left + width, top + y + 1, color);
      }
   }

   /**
    * handles input updates for the grid.
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

   /**
    * builder for grid components.
    */
   public static final class Builder {
      private int gridSpacing;

      private Builder() {
      }

      /**
       * sets the grid spacing.
       *
       * @param gridSpacing the grid spacing
       * @return the builder
       */
      public Builder spacing(int gridSpacing) {
         this.gridSpacing = gridSpacing;
         return this;
      }

      /**
       * builds the grid component.
       *
       * @return the grid
       */
      public HudEditorGrid build() {
         return new HudEditorGrid(this);
      }
   }
}
