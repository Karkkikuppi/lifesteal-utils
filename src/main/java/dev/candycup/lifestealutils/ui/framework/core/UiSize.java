package dev.candycup.lifestealutils.ui.framework.core;

/**
 * describes a preferred width and height for layout.
 */
public record UiSize(int width, int height) {
   private static final int EMPTY_SIZE = 0;

   public static UiSize empty() {
      return new UiSize(EMPTY_SIZE, EMPTY_SIZE);
   }
}
