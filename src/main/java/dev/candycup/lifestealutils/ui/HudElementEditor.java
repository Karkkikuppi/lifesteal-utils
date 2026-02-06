package dev.candycup.lifestealutils.ui;

import dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.hud.HudPosition;
import dev.candycup.lifestealutils.interapi.MessagingUtils;
import dev.candycup.lifestealutils.interapi.SoundUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HudElementEditor extends Screen {
   public static final Identifier EDITOR_LAYER_ID = Identifier.fromNamespaceAndPath("lifestealutils", "hud_editor");
   private static final int GRID_SPACING_PIXELS = 32;
   private static final int SNAP_STEP_PIXELS = GRID_SPACING_PIXELS / 2;
   private static final int SNAP_BUTTON_PADDING_X = 6;
   private static final int SNAP_BUTTON_PADDING_Y = 3;
   private static final int SNAP_BUTTON_SPACING = 6;
   private static final int SNAP_BUTTON_BORDER = 1;
   private static final int SNAP_BUTTON_BACKGROUND = 0xB0000000;
   private static final int SNAP_BUTTON_BACKGROUND_HOVER = 0xC0000000;
   private static final int SNAP_BUTTON_TEXT = 0xFFFFFFFF;

   private static Identifier draggingId;
   private static float dragOffsetX;
   private static float dragOffsetY;
   private static boolean lastLeftDown;
   private static boolean snapEnabled;
   private static boolean snappingStateInitialized;
   private static boolean lastSnappingActive;

   private static PoiDirectionalIndicator poiDirectionalIndicator;

   public HudElementEditor(Component component) {
      super(component);
   }

   /**
    * Sets the POI directional indicator for preview rendering in the editor.
    *
    * @param indicator the directional indicator to render
    */
   public static void setPoiDirectionalIndicator(PoiDirectionalIndicator indicator) {
      poiDirectionalIndicator = indicator;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(null);
      draggingId = null;
      lastLeftDown = false;
      snappingStateInitialized = false;
      lastSnappingActive = false;
   }

   @Override
   public void renderBlurredBackground(GuiGraphics guiGraphics) {
   }

   public static HudElement editorLayer() {
      return (drawContext, tickCounter) -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (!(minecraft.screen instanceof HudElementEditor)) {
            return;
         }

         double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
         double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
         boolean leftDown = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;

         int guiWidth = minecraft.getWindow().getGuiScaledWidth();
         int guiHeight = minecraft.getWindow().getGuiScaledHeight();

         int screenTopAlphaBlack = 0x80000000;
         int screenBottomAlphaBlack = 0x40000000;
         drawContext.fillGradient(0, 0, guiWidth, guiHeight, screenTopAlphaBlack, screenBottomAlphaBlack);
         int blockTopAlphaBlack = 0xA0000000;
         int blockBottomAlphaBlack = 0x50000000;
         drawContext.fillGradient(0, 0, guiWidth, guiHeight, blockTopAlphaBlack, blockBottomAlphaBlack);

         Component title = MessagingUtils.miniMessage("<red><bold>Lifesteal Utils HUD Editor</bold></red>");
         Component subtitle = MessagingUtils.miniMessage("<gray><italic>Access more timers at /lsu config!</italic></gray>");
         int titleY = 8;
         drawContext.drawString(minecraft.font, title, 8, titleY, 0xFFFFFFFF, true);
         drawContext.drawString(minecraft.font, subtitle, 8, titleY + minecraft.font.lineHeight + 2, 0xFFFFFFFF, true);

         boolean shiftDown = isShiftDown(minecraft.getWindow().handle());
         boolean snappingActive = isSnappingActive(snapEnabled, shiftDown);

         Component snapLabel = Component.translatable(snappingActive
                 ? "lsu.hudEditor.snap.on"
                 : "lsu.hudEditor.snap.off");
         Component openConfigLabel = Component.translatable("lsu.hudEditor.openConfig");

         int buttonX = 8;
         int buttonY = titleY + minecraft.font.lineHeight * 2 + SNAP_BUTTON_SPACING + 2;
         int buttonHeight = minecraft.font.lineHeight + SNAP_BUTTON_PADDING_Y * 2;

         int snapButtonWidth = minecraft.font.width(snapLabel) + SNAP_BUTTON_PADDING_X * 2;
         boolean hoveringSnap = drawEditorButton(drawContext, minecraft, snapLabel, buttonX, buttonY, snapButtonWidth, buttonHeight, mouseX, mouseY);

         int configButtonX = buttonX + snapButtonWidth + SNAP_BUTTON_SPACING;
         int configButtonWidth = minecraft.font.width(openConfigLabel) + SNAP_BUTTON_PADDING_X * 2;
         boolean hoveringConfig = drawEditorButton(drawContext, minecraft, openConfigLabel, configButtonX, buttonY, configButtonWidth, buttonHeight, mouseX, mouseY);

         if (hoveringSnap && leftDown && !lastLeftDown) {
            snapEnabled = !snapEnabled;
            snappingActive = isSnappingActive(snapEnabled, shiftDown);
         }
         if (hoveringConfig && leftDown && !lastLeftDown) {
            SoundUtils.playUiClick();
            sendConfigCommand(minecraft);
         }

         int gridSpacing = GRID_SPACING_PIXELS;
         int gridTopAlpha = 0x40;
         int gridBottomAlpha = 0x18;
         int gridTopColor = (gridTopAlpha << 24) | 0x00FFFFFF;
         int gridBottomColor = (gridBottomAlpha << 24) | 0x00FFFFFF;
         int centerX = guiWidth / 2;
         for (int x = centerX; x < guiWidth; x += gridSpacing) {
            drawContext.fillGradient(x, 0, x + 1, guiHeight, gridTopColor, gridBottomColor);
         }
         for (int x = centerX - gridSpacing; x >= 0; x -= gridSpacing) {
            drawContext.fillGradient(x, 0, x + 1, guiHeight, gridTopColor, gridBottomColor);
         }
         for (int y = 0; y < guiHeight; y += gridSpacing) {
            float t = guiHeight == 0 ? 0F : (float) y / (float) guiHeight;
            int alpha = Mth.floor(Mth.lerp(t, gridTopAlpha, gridBottomAlpha));
            int color = (alpha << 24) | 0x00FFFFFF;
            drawContext.fill(0, y, guiWidth, y + 1, color);
         }

         // render and handle the directional indicator as a draggable element
         if (poiDirectionalIndicator != null) {
            poiDirectionalIndicator.ensurePositionRegistered(guiWidth, guiHeight);
            poiDirectionalIndicator.render(drawContext, guiWidth, guiHeight);

            // get indicator position and size for hit testing
            Identifier indicatorId = poiDirectionalIndicator.getHudElementId();
            int indicatorSize = poiDirectionalIndicator.getTextureSize();
            HudPosition indicatorPos = HudElementManager.positionFor(indicatorId);
            int indicatorX = pixelCoordinate(indicatorPos.x(), guiWidth, indicatorSize);
            int indicatorY = pixelCoordinate(indicatorPos.y(), guiHeight, indicatorSize);

            boolean hoveringIndicator = mouseX >= indicatorX && mouseX <= indicatorX + indicatorSize
                    && mouseY >= indicatorY && mouseY <= indicatorY + indicatorSize;

            // start dragging indicator
            if (leftDown && !lastLeftDown && hoveringIndicator) {
               draggingId = indicatorId;
               dragOffsetX = (float) mouseX - indicatorX;
               dragOffsetY = (float) mouseY - indicatorY;
            }

            boolean isDraggingIndicator = draggingId != null && draggingId.equals(indicatorId);
            if (isDraggingIndicator) {
               float snappedX = (float) mouseX - dragOffsetX;
               float snappedY = (float) mouseY - dragOffsetY;
               if (snappingActive) {
                  snappedX = snapPixelX(snappedX, guiWidth, indicatorSize, SNAP_STEP_PIXELS);
                  snappedY = snapPixelY(snappedY, guiHeight, indicatorSize, SNAP_STEP_PIXELS);
               }
               HudElementManager.updatePositionFromPixels(
                       indicatorId,
                       snappedX,
                       snappedY,
                       guiWidth,
                       guiHeight,
                       indicatorSize,
                       indicatorSize
               );
            }

            // draw indicator bounding box when hovering or dragging
            if (hoveringIndicator || isDraggingIndicator) {
               int boxLeft = indicatorX - 2;
               int boxTop = indicatorY - 2;
               int boxRight = indicatorX + indicatorSize + 2;
               int boxBottom = indicatorY + indicatorSize + 2;
               int strokeColor = isDraggingIndicator ? 0xC066FF66 : 0xC0FFFFFF;
               drawContext.fill(boxLeft, boxTop, boxRight, boxTop + 1, strokeColor);
               drawContext.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, strokeColor);
               drawContext.fill(boxLeft, boxTop, boxLeft + 1, boxBottom, strokeColor);
               drawContext.fill(boxRight - 1, boxTop, boxRight, boxBottom, strokeColor);
            }
         }

         List<HudElementManager.RenderedHudElement> elements = HudElementManager.renderables(minecraft.font, guiWidth, guiHeight);
         for (HudElementManager.RenderedHudElement element : elements) {
            HudElementManager.RenderedHudElement current = element;
            boolean hovering = mouseX >= current.x() && mouseX <= current.x() + current.textWidth()
                    && mouseY >= current.y() && mouseY <= current.y() + current.textHeight();
            if (leftDown && !lastLeftDown && hovering) {
               draggingId = current.definition().id();
               dragOffsetX = (float) mouseX - current.x();
               dragOffsetY = (float) mouseY - current.y();
            }

            boolean isDragging = draggingId != null && draggingId.equals(current.definition().id());
            if (isDragging) {
               float snappedX = (float) mouseX - dragOffsetX;
               float snappedY = (float) mouseY - dragOffsetY;
               if (snappingActive) {
                  snappedX = snapPixelX(snappedX, guiWidth, current.textWidth(), SNAP_STEP_PIXELS);
                  snappedY = snapPixelY(snappedY, guiHeight, current.textHeight(), SNAP_STEP_PIXELS);
               }
               HudElementManager.updatePositionFromPixels(
                       current.definition().id(),
                       snappedX,
                       snappedY,
                       guiWidth,
                       guiHeight,
                       current.textWidth(),
                       current.textHeight()
               );
               current = HudElementManager.renderable(current.definition(), minecraft.font, guiWidth, guiHeight);
            }

            int boxLeft = Mth.floor(current.x()) - 4;
            int boxTop = Mth.floor(current.y()) - 4;
            int boxRight = boxLeft + current.textWidth() + 8;
            int boxBottom = boxTop + current.textHeight() + 8;
            boolean visible = hovering || isDragging;
            if (visible) {
               int strokeColor = 0xC0FFFFFF;
               drawContext.fill(boxLeft, boxTop, boxRight, boxTop + 1, strokeColor);
               drawContext.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, strokeColor);
               drawContext.fill(boxLeft, boxTop, boxLeft + 1, boxBottom, strokeColor);
               drawContext.fill(boxRight - 1, boxTop, boxRight, boxBottom, strokeColor);
            }

            int color = isDragging ? 0xFF66FF66 : (hovering ? 0xFFFFFFFF : 0xFFCCCCCC);
            drawContext.drawString(minecraft.font, current.component(), current.x(), current.y(), color, true);
         }

         if (!snappingStateInitialized) {
            lastSnappingActive = snappingActive;
            snappingStateInitialized = true;
         } else if (snappingActive != lastSnappingActive) {
            SoundUtils.playUiClick();
            lastSnappingActive = snappingActive;
         }

         if (draggingId != null && lastLeftDown && !leftDown) {
            HudElementManager.saveLayout();
            draggingId = null;
         }

         lastLeftDown = leftDown;
      };
   }

   /**
    * Calculates pixel coordinate from normalized position.
    */
   private static int pixelCoordinate(float normalized, int guiSize, int elementSize) {
      int available = Math.max(guiSize - elementSize, 0);
      float clamped = Mth.clamp(normalized, 0F, 1F);
      return Mth.floor(clamped * available);
   }

   /**
    * Determines if snapping should be active, accounting for the shift modifier.
    *
    * @param snapEnabled the base snap toggle state
    * @param shiftDown   whether the shift key is currently held
    * @return true if snapping should be applied
    */
   private static boolean isSnappingActive(boolean snapEnabled, boolean shiftDown) {
      return snapEnabled ^ shiftDown;
   }

   /**
    * Snaps a horizontal pixel coordinate to the snap step from the left edge.
    *
    * @param pixelX       the current x position
    * @param guiWidth     the gui width
    * @param elementWidth the element width
    * @param snapStep     the snap step in pixels
    * @return snapped x coordinate
    */
   private static float snapPixelX(float pixelX, int guiWidth, int elementWidth, int snapStep) {
      float max = Math.max(guiWidth - elementWidth, 0);
      float clamped = Mth.clamp(pixelX, 0F, max);
      float snapped = snapToStep(clamped, snapStep);
      return Mth.clamp(snapped, 0F, max);
   }

   /**
    * Snaps a vertical pixel coordinate to the snap step from the bottom edge.
    *
    * @param pixelY        the current y position
    * @param guiHeight     the gui height
    * @param elementHeight the element height
    * @param snapStep      the snap step in pixels
    * @return snapped y coordinate
    */
   private static float snapPixelY(float pixelY, int guiHeight, int elementHeight, int snapStep) {
      float max = Math.max(guiHeight - elementHeight, 0);
      float clamped = Mth.clamp(pixelY, 0F, max);
      float fromBottom = max - clamped;
      float snappedFromBottom = snapToStep(fromBottom, snapStep);
      float snapped = max - snappedFromBottom;
      return Mth.clamp(snapped, 0F, max);
   }

   /**
    * Snaps a value to the nearest snap step.
    *
    * @param value    the value to snap
    * @param snapStep the snap step in pixels
    * @return snapped value
    */
   private static float snapToStep(float value, int snapStep) {
      if (snapStep <= 1) {
         return value;
      }
      return Math.round(value / (float) snapStep) * snapStep;
   }

   /**
    * Checks if either shift key is currently held.
    *
    * @param windowHandle the glfw window handle
    * @return true if shift is held, false otherwise
    */
   private static boolean isShiftDown(long windowHandle) {
      return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
              || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
   }

   /**
    * Draws a labeled editor button and returns whether it is hovered.
    *
    * @param drawContext the gui graphics
    * @param minecraft   the minecraft client
    * @param label       the button label
    * @param x           the x position
    * @param y           the y position
    * @param width       the button width
    * @param height      the button height
    * @param mouseX      the mouse x position
    * @param mouseY      the mouse y position
    * @return true if the mouse is over the button
    */
   private static boolean drawEditorButton(GuiGraphics drawContext, Minecraft minecraft, Component label, int x, int y, int width, int height, double mouseX, double mouseY) {
      boolean hovering = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
      int background = hovering ? SNAP_BUTTON_BACKGROUND_HOVER : SNAP_BUTTON_BACKGROUND;
      int borderColor = 0xC0FFFFFF;

      drawContext.fill(x, y, x + width, y + height, background);
      drawContext.fill(x, y, x + width, y + SNAP_BUTTON_BORDER, borderColor);
      drawContext.fill(x, y + height - SNAP_BUTTON_BORDER, x + width, y + height, borderColor);
      drawContext.fill(x, y, x + SNAP_BUTTON_BORDER, y + height, borderColor);
      drawContext.fill(x + width - SNAP_BUTTON_BORDER, y, x + width, y + height, borderColor);
      drawContext.drawString(minecraft.font, label, x + SNAP_BUTTON_PADDING_X, y + SNAP_BUTTON_PADDING_Y, SNAP_BUTTON_TEXT, true);

      return hovering;
   }

   /**
    * Sends the config command using the client connection.
    *
    * @param minecraft the minecraft client
    */
   private static void sendConfigCommand(Minecraft minecraft) {
      if (minecraft.player == null || minecraft.player.connection == null) {
         return;
      }
      minecraft.player.connection.sendCommand("lsu config");
   }
}
