package dev.candycup.lifestealutils.ui.editor;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.hud.HudAnchor;
import dev.candycup.lifestealutils.hud.HudElementManager;
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
   private static final int TEXT_OUTLINE_PADDING = 4;
   private static final int OUTLINE_STROKE = 1;
   private static final int ANCHOR_ROW_GAP = 6;
   private static final int ANCHOR_BUTTON_GAP = 2;
   private static final int ANCHOR_BUTTON_PADDING_X = 3;
   private static final int ANCHOR_BUTTON_PADDING_Y = 1;
   private static final int ANCHOR_BUTTON_SIZE_BONUS = 1;
   private static final int ANCHOR_BUTTON_BG = 0xAA111111;
   private static final int ANCHOR_BUTTON_HOVER_BG = 0xAA333333;
   private static final int ANCHOR_BUTTON_ACTIVE_BG = 0xAA1B4B9A;
   private static final int ANCHOR_BUTTON_BORDER = 0xCCFFFFFF;

   private final HudEditorState state;
   private final int snapStepPixels;

   private UiBounds bounds = UiBounds.empty();
   private Font font;
   private int guiWidth;
   private int guiHeight;

   private HudEditorCanvas(Builder builder) {
      this.state = builder.state;
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
      List<HudElementManager.RenderedHudElement> elements = renderElements();
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

   private void handleTextInput(UiInputState input) {
      List<HudElementManager.RenderedHudElement> elements = renderElements();
      for (HudElementManager.RenderedHudElement element : elements) {
         Identifier id = element.definition().id();
         AnchorControlLayout controls = anchorControlLayout(element);
         boolean hoveringLabel = isHovering(element.x(), element.y(), element.textWidth(), element.textHeight(), input);
         HudAnchor clickedAnchor = clickedAnchor(controls, input);
         if (!state.isDraggingAny() && clickedAnchor != null) {
            if (HudElementManager.anchorFor(id) != clickedAnchor) {
               HudElementManager.updateAnchor(id, clickedAnchor, guiWidth, element.textWidth());
               HudElementManager.saveLayout();
               SoundUtils.playUiClick();
            }
            continue;
         }

         if (hoveringLabel && input.leftClicked()) {
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

   private List<HudElementManager.RenderedHudElement> renderElements() {
      return HudElementManager.renderables(font, guiWidth, guiHeight);
   }

   private void drawTextElement(UiContext context, HudElementManager.RenderedHudElement element, Identifier id) {
      boolean dragging = state.isDragging(id);

      HudElementManager.RenderedHudElement renderElement = element;
      if (dragging) {
         renderElement = HudElementManager.renderable(renderElement.definition(), font, guiWidth, guiHeight);
      }

      AnchorControlLayout controls = anchorControlLayout(renderElement);
      boolean hoveringLabel = isHovering(renderElement.x(), renderElement.y(), renderElement.textWidth(), renderElement.textHeight(), context.input());
      boolean hoveringAnchorRow = isHovering(anchorHoverRowBounds(controls), context.input());
      boolean highlighted = hoveringLabel || hoveringAnchorRow;

      if (highlighted || dragging) {
         int outlineX = Mth.floor(renderElement.x()) - TEXT_OUTLINE_PADDING;
         int outlineY = Mth.floor(renderElement.y()) - TEXT_OUTLINE_PADDING;
         int outlineWidth = renderElement.textWidth() + TEXT_OUTLINE_PADDING * 2;
         int outlineHeight = renderElement.textHeight() + TEXT_OUTLINE_PADDING * 2;
         drawOutline(context, outlineX, outlineY, outlineWidth, outlineHeight, OUTLINE_COLOR);
      }

      if (!dragging && !state.isDraggingAny() && highlighted) {
         drawAnchorControls(context, controls, HudElementManager.anchorFor(id));
      }

      int color = dragging ? TEXT_DRAG_COLOR : (highlighted ? TEXT_HOVER_COLOR : TEXT_DEFAULT_COLOR);
      context.graphics().drawString(font, renderElement.component(), renderElement.x(), renderElement.y(), color, true);
   }

   private AnchorControlLayout anchorControlLayout(HudElementManager.RenderedHudElement element) {
      int buttonHeight = font.lineHeight + ANCHOR_BUTTON_PADDING_Y * 2 + ANCHOR_BUTTON_SIZE_BONUS;
      int rowY = element.y() + element.textHeight() + ANCHOR_ROW_GAP;
      int labelX = element.x();
      int labelY = rowY + ANCHOR_BUTTON_PADDING_Y;
      int buttonsStartX = labelX + font.width("Anchor") + 4;

      int buttonWidth = font.width("C") + ANCHOR_BUTTON_PADDING_X * 2 + ANCHOR_BUTTON_SIZE_BONUS;
      UiBounds left = new UiBounds(buttonsStartX, rowY, buttonWidth, buttonHeight);
      UiBounds center = new UiBounds(left.x() + buttonWidth + ANCHOR_BUTTON_GAP, rowY, buttonWidth, buttonHeight);
      UiBounds right = new UiBounds(center.x() + buttonWidth + ANCHOR_BUTTON_GAP, rowY, buttonWidth, buttonHeight);
      return new AnchorControlLayout(labelX, labelY, left, center, right);
   }

   private UiBounds anchorHoverRowBounds(AnchorControlLayout controls) {
      int left = controls.labelX();
      int top = Math.min(controls.labelY(), controls.left().y());
      int right = controls.right().x() + controls.right().width();
      int bottom = Math.max(controls.labelY() + font.lineHeight, controls.left().y() + controls.left().height());
      return new UiBounds(left, top, right - left, bottom - top);
   }

   private HudAnchor clickedAnchor(AnchorControlLayout controls, UiInputState input) {
      if (!input.leftClicked() || state.isDraggingAny()) {
         return null;
      }
      if (isHovering(controls.left(), input)) {
         return HudAnchor.LEFT;
      }
      if (isHovering(controls.center(), input)) {
         return HudAnchor.CENTER;
      }
      if (isHovering(controls.right(), input)) {
         return HudAnchor.RIGHT;
      }
      return null;
   }

   private void drawAnchorControls(UiContext context, AnchorControlLayout controls, HudAnchor selectedAnchor) {
      context.graphics().drawString(font, "Anchor", controls.labelX(), controls.labelY(), 0xFFFFFFFF, true);
      drawAnchorButton(context, controls.left(), "L", selectedAnchor == HudAnchor.LEFT);
      drawAnchorButton(context, controls.center(), "C", selectedAnchor == HudAnchor.CENTER);
      drawAnchorButton(context, controls.right(), "R", selectedAnchor == HudAnchor.RIGHT);
   }

   private void drawAnchorButton(UiContext context, UiBounds button, String label, boolean selected) {
      boolean hovered = isHovering(button, context.input());
      int color = selected ? ANCHOR_BUTTON_ACTIVE_BG : (hovered ? ANCHOR_BUTTON_HOVER_BG : ANCHOR_BUTTON_BG);
      context.graphics().fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), color);
      drawOutline(context, button.x(), button.y(), button.width(), button.height(), ANCHOR_BUTTON_BORDER);

      int textX = button.x() + (button.width() - font.width(label)) / 2;
      int textY = button.y() + (button.height() - font.lineHeight) / 2;
      context.graphics().drawString(font, label, textX + 1, textY + 1, 0xFFFFFFFF, true);
   }

   private boolean isHovering(UiBounds bounds, UiInputState input) {
      return input.mouseX() >= bounds.x() && input.mouseX() <= bounds.x() + bounds.width()
              && input.mouseY() >= bounds.y() && input.mouseY() <= bounds.y() + bounds.height();
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

   private record AnchorControlLayout(
           int labelX,
           int labelY,
           UiBounds left,
           UiBounds center,
           UiBounds right
   ) {
   }

   /**
    * builder for editor canvas components.
    */
   public static final class Builder {
      private HudEditorState state;
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
