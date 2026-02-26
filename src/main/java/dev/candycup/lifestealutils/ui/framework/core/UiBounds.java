package dev.candycup.lifestealutils.ui.framework.core;

/**
 * represents a rectangular region in screen space for layout and hit testing.
 */
public record UiBounds(int x, int y, int width, int height) {
   private static final int EMPTY_SIZE = 0;

   public static UiBounds empty() {
      return new UiBounds(EMPTY_SIZE, EMPTY_SIZE, EMPTY_SIZE, EMPTY_SIZE);
   }

   public boolean contains(double pointX, double pointY) {
      return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
   }

   public UiBounds withPosition(int newX, int newY) {
      return new UiBounds(newX, newY, width, height);
   }

   public UiBounds withSize(int newWidth, int newHeight) {
      return new UiBounds(x, y, newWidth, newHeight);
   }
}
