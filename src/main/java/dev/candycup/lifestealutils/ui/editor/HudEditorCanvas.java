package dev.candycup.lifestealutils.ui.editor;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.hud.HudPosition;
import dev.candycup.lifestealutils.interapi.SoundUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * handles dragging and rendering HUD elements in the editor.
 */
public final class HudEditorCanvas implements Drawable {
   private static final int TEXT_DEFAULT_COLOR = 0xFFCCCCCC;
   private static final int TEXT_HOVER_COLOR = 0xFFFFFFFF;
   private static final int TEXT_DRAG_COLOR = 0xFF66FF66;
   private static final int OUTLINE_COLOR = 0xC0FFFFFF;
   private static final int OUTLINE_DRAG_COLOR = 0xC066FF66;
   private static final int INDICATOR_OUTLINE_PADDING = 2;
   private static final int TEXT_OUTLINE_PADDING = 4;
   private static final int OUTLINE_STROKE = 1;

   private final HudEditorState state;
   private final PoiDirectionalIndicator poiDirectionalIndicator;
   private final int snapStepPixels;

   private UiBounds bounds = UiBounds.empty();
   private Font font;
   private int guiWidth;
   private int guiHeight;

   private HudEditorCanvas(Builder builder) {
      this.state = builder.state;
      this.poiDirectionalIndicator = builder.poiDirectionalIndicator;
      this.snapStepPixels = builder.snapStepPixels;
   }

   /**
    * creates a new builder for the editor canvas.
    *
    * @return the builder
    */
   public static Builder builder() {
      return new Builder();
   }

   /**
    * lays out the canvas to fill available bounds.
    *
    * @param layoutContext the layout context
    */
   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      this.font = layoutContext.font();
      this.guiWidth = bounds.width();
      this.guiHeight = bounds.height();
   }

   /**
    * renders the hud elements and drag handles.
    *
    * @param context the render context
    */
   @Override
   public void render(UiContext context) {
      if (font == null) {
         return;
      }
      renderIndicator(context);
      List<HudElementManager.RenderedHudElement> elements = HudElementManager.renderables(font, guiWidth, guiHeight);
      for (HudElementManager.RenderedHudElement element : elements) {
         drawTextElement(context, element, element.definition().id());
      }
   }

   /**
    * handles input updates for dragging hud elements.
    *
    * @param input the input state
    */
   @Override
   public void handleInput(UiInputState input) {
      if (font == null) {
         return;
      }
      boolean snappingChanged = state.updateSnapping(input.shiftDown());
      if (snappingChanged) {
         SoundUtils.playUiClick();
      }

      handleIndicatorInput(input);
      handleTextInput(input);

      if (state.isDraggingAny() && input.leftReleased()) {
         HudElementManager.saveLayout();
         state.clearDrag();
      }
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

   private void renderIndicator(UiContext context) {
      if (poiDirectionalIndicator == null) {
         return;
      }
      poiDirectionalIndicator.ensurePositionRegistered(guiWidth, guiHeight);
      poiDirectionalIndicator.render(context.graphics(), guiWidth, guiHeight);

      Identifier indicatorId = poiDirectionalIndicator.getHudElementId();
      int indicatorSize = poiDirectionalIndicator.getTextureSize();
      HudPosition indicatorPos = HudElementManager.positionFor(indicatorId);
      int indicatorX = pixelCoordinate(indicatorPos.x(), guiWidth, indicatorSize);
      int indicatorY = pixelCoordinate(indicatorPos.y(), guiHeight, indicatorSize);

      boolean hoveringIndicator = isHovering(indicatorX, indicatorY, indicatorSize, indicatorSize, context.input());
      boolean draggingIndicator = state.isDragging(indicatorId);
      if (hoveringIndicator || draggingIndicator) {
         drawOutline(
                 context,
                 indicatorX - INDICATOR_OUTLINE_PADDING,
                 indicatorY - INDICATOR_OUTLINE_PADDING,
                 indicatorSize + INDICATOR_OUTLINE_PADDING * 2,
                 indicatorSize + INDICATOR_OUTLINE_PADDING * 2,
                 draggingIndicator ? OUTLINE_DRAG_COLOR : OUTLINE_COLOR
         );
      }
   }

   private void handleIndicatorInput(UiInputState input) {
      if (poiDirectionalIndicator == null) {
         return;
      }
      Identifier indicatorId = poiDirectionalIndicator.getHudElementId();
      int indicatorSize = poiDirectionalIndicator.getTextureSize();
      HudPosition indicatorPos = HudElementManager.positionFor(indicatorId);
      int indicatorX = pixelCoordinate(indicatorPos.x(), guiWidth, indicatorSize);
      int indicatorY = pixelCoordinate(indicatorPos.y(), guiHeight, indicatorSize);

      boolean hoveringIndicator = isHovering(indicatorX, indicatorY, indicatorSize, indicatorSize, input);
      if (hoveringIndicator && input.leftClicked()) {
         state.beginDrag(indicatorId, (float) input.mouseX() - indicatorX, (float) input.mouseY() - indicatorY);
      }

      if (state.isDragging(indicatorId) && input.leftDown()) {
         float dragX = (float) input.mouseX() - state.dragOffsetX();
         float dragY = (float) input.mouseY() - state.dragOffsetY();
         if (state.isSnappingActive()) {
            dragX = snapPixelX(dragX, guiWidth, indicatorSize, snapStepPixels);
            dragY = snapPixelY(dragY, guiHeight, indicatorSize, snapStepPixels);
         }
         HudElementManager.updatePositionFromPixels(
                 indicatorId,
                 dragX,
                 dragY,
                 guiWidth,
                 guiHeight,
                 indicatorSize,
                 indicatorSize
         );
      }
   }

   private void handleTextInput(UiInputState input) {
      List<HudElementManager.RenderedHudElement> elements = HudElementManager.renderables(font, guiWidth, guiHeight);
      for (HudElementManager.RenderedHudElement element : elements) {
         Identifier id = element.definition().id();
         boolean hovering = isHovering(element.x(), element.y(), element.textWidth(), element.textHeight(), input);
         if (hovering && input.leftClicked()) {
            state.beginDrag(id, (float) input.mouseX() - element.x(), (float) input.mouseY() - element.y());
         }

         if (state.isDragging(id) && input.leftDown()) {
            float dragX = (float) input.mouseX() - state.dragOffsetX();
            float dragY = (float) input.mouseY() - state.dragOffsetY();
            if (state.isSnappingActive()) {
               dragX = snapPixelX(dragX, guiWidth, element.textWidth(), snapStepPixels);
               dragY = snapPixelY(dragY, guiHeight, element.textHeight(), snapStepPixels);
            }
            HudElementManager.updatePositionFromPixels(
                    id,
                    dragX,
                    dragY,
                    guiWidth,
                    guiHeight,
                    element.textWidth(),
                    element.textHeight()
            );
         }
      }
   }

   private void drawTextElement(UiContext context, HudElementManager.RenderedHudElement element, Identifier id) {
      boolean hovering = isHovering(element.x(), element.y(), element.textWidth(), element.textHeight(), context.input());
      boolean dragging = state.isDragging(id);

      HudElementManager.RenderedHudElement renderElement = element;
      if (dragging) {
         renderElement = HudElementManager.renderable(renderElement.definition(), font, guiWidth, guiHeight);
      }

      if (hovering || dragging) {
         int outlineX = Mth.floor(renderElement.x()) - TEXT_OUTLINE_PADDING;
         int outlineY = Mth.floor(renderElement.y()) - TEXT_OUTLINE_PADDING;
         int outlineWidth = renderElement.textWidth() + TEXT_OUTLINE_PADDING * 2;
         int outlineHeight = renderElement.textHeight() + TEXT_OUTLINE_PADDING * 2;
         drawOutline(context, outlineX, outlineY, outlineWidth, outlineHeight, OUTLINE_COLOR);
      }

      int color = dragging ? TEXT_DRAG_COLOR : (hovering ? TEXT_HOVER_COLOR : TEXT_DEFAULT_COLOR);
      context.graphics().drawString(font, renderElement.component(), renderElement.x(), renderElement.y(), color, true);
   }

   private void drawOutline(UiContext context, int x, int y, int width, int height, int color) {
      int left = x;
      int right = x + width;
      int top = y;
      int bottom = y + height;
      context.graphics().fill(left, top, right, top + OUTLINE_STROKE, color);
      context.graphics().fill(left, bottom - OUTLINE_STROKE, right, bottom, color);
      context.graphics().fill(left, top, left + OUTLINE_STROKE, bottom, color);
      context.graphics().fill(right - OUTLINE_STROKE, top, right, bottom, color);
   }

   private boolean isHovering(int x, int y, int width, int height, UiInputState input) {
      return input.mouseX() >= x && input.mouseX() <= x + width
              && input.mouseY() >= y && input.mouseY() <= y + height;
   }

   private int pixelCoordinate(float normalized, int guiSize, int elementSize) {
      int available = Math.max(guiSize - elementSize, 0);
      float clamped = Mth.clamp(normalized, 0F, 1F);
      return Mth.floor(clamped * available);
   }

   private float snapPixelX(float pixelX, int width, int elementWidth, int snapStep) {
      float max = Math.max(width - elementWidth, 0);
      float clamped = Mth.clamp(pixelX, 0F, max);
      float snapped = snapToStep(clamped, snapStep);
      return Mth.clamp(snapped, 0F, max);
   }

   private float snapPixelY(float pixelY, int height, int elementHeight, int snapStep) {
      float max = Math.max(height - elementHeight, 0);
      float clamped = Mth.clamp(pixelY, 0F, max);
      float fromBottom = max - clamped;
      float snappedFromBottom = snapToStep(fromBottom, snapStep);
      float snapped = max - snappedFromBottom;
      return Mth.clamp(snapped, 0F, max);
   }

   private float snapToStep(float value, int snapStep) {
      if (snapStep <= 1) {
         return value;
      }
      return Math.round(value / (float) snapStep) * snapStep;
   }

   /**
    * builder for editor canvas components.
    */
   public static final class Builder {
      private HudEditorState state;
      private PoiDirectionalIndicator poiDirectionalIndicator;
      private int snapStepPixels;

      private Builder() {
      }

      /**
       * sets the editor state.
       *
       * @param state the editor state
       * @return the builder
       */
      public Builder state(HudEditorState state) {
         this.state = state;
         return this;
      }

      /**
       * sets the poi indicator instance.
       *
       * @param poiDirectionalIndicator the indicator
       * @return the builder
       */
      public Builder poiIndicator(PoiDirectionalIndicator poiDirectionalIndicator) {
         this.poiDirectionalIndicator = poiDirectionalIndicator;
         return this;
      }

      /**
       * sets the snap step in pixels.
       *
       * @param snapStepPixels the snap step
       * @return the builder
       */
      public Builder snapStepPixels(int snapStepPixels) {
         this.snapStepPixels = snapStepPixels;
         return this;
      }

      /**
       * builds the editor canvas component.
       *
       * @return the editor canvas
       */
      public HudEditorCanvas build() {
         return new HudEditorCanvas(this);
      }
   }
}
