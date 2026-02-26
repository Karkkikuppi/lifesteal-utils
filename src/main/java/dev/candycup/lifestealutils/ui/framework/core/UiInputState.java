package dev.candycup.lifestealutils.ui.framework.core;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * captures input state for ui interactions.
 */
public final class UiInputState {
   private static final int LEFT_BUTTON = GLFW.GLFW_MOUSE_BUTTON_1;
   private static final int RIGHT_BUTTON = GLFW.GLFW_MOUSE_BUTTON_2;

   private final double mouseX;
   private final double mouseY;
   private final boolean leftDown;
   private final boolean rightDown;
   private final boolean leftClicked;
   private final boolean leftReleased;
   private final boolean shiftDown;
   private final double scrollX;
   private final double scrollY;

   private UiInputState(
           double mouseX,
           double mouseY,
           boolean leftDown,
           boolean rightDown,
           boolean leftClicked,
           boolean leftReleased,
           boolean shiftDown,
           double scrollX,
           double scrollY
   ) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.leftDown = leftDown;
      this.rightDown = rightDown;
      this.leftClicked = leftClicked;
      this.leftReleased = leftReleased;
      this.shiftDown = shiftDown;
      this.scrollX = scrollX;
      this.scrollY = scrollY;
   }

   public static UiInputState from(Minecraft minecraft, UiInputState previous) {
      return from(minecraft, previous, 0, 0);
   }

   public static UiInputState from(Minecraft minecraft, UiInputState previous, double scrollX, double scrollY) {
      long handle = minecraft.getWindow().handle();
      double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
      double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
      boolean leftDown = GLFW.glfwGetMouseButton(handle, LEFT_BUTTON) == GLFW.GLFW_PRESS;
      boolean rightDown = GLFW.glfwGetMouseButton(handle, RIGHT_BUTTON) == GLFW.GLFW_PRESS;
      boolean shiftDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
              || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

      boolean leftClicked = leftDown && (previous == null || !previous.leftDown);
      boolean leftReleased = !leftDown && previous != null && previous.leftDown;

      return new UiInputState(mouseX, mouseY, leftDown, rightDown, leftClicked, leftReleased, shiftDown, scrollX, scrollY);
   }

   public boolean isHovering(UiBounds bounds) {
      return bounds.contains(mouseX, mouseY);
   }

   public double mouseX() {
      return mouseX;
   }

   public double mouseY() {
      return mouseY;
   }

   public boolean leftDown() {
      return leftDown;
   }

   public boolean rightDown() {
      return rightDown;
   }

   public boolean leftClicked() {
      return leftClicked;
   }

   public boolean leftReleased() {
      return leftReleased;
   }

   public boolean shiftDown() {
      return shiftDown;
   }

   public double scrollX() {
      return scrollX;
   }

   public double scrollY() {
      return scrollY;
   }
}
