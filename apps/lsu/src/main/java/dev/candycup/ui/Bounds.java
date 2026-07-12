package dev.candycup.ui;

/**
 * Rectangular screen-space region used by layout, rendering, and hit testing.
 */
public record Bounds(int x, int y, int width, int height) {
    public static Bounds empty() {
        return new Bounds(0, 0, 0, 0);
    }

    public boolean contains(double pointX, double pointY) {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
    }
}
