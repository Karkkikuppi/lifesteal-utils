package dev.candycup.lifestealutils.hud;

public record HudPosition(float x, float y, HudAnchor anchor) {
   public HudPosition(float x, float y) {
      this(x, y, HudAnchor.LEFT);
   }

   public static HudPosition clamp(float x, float y) {
      return new HudPosition(clamp01(x), clamp01(y), HudAnchor.LEFT);
   }

   public static HudPosition clamp(float x, float y, HudAnchor anchor) {
      return new HudPosition(clamp01(x), clamp01(y), safeAnchor(anchor));
   }

   public HudPosition {
      anchor = safeAnchor(anchor);
   }

   private static float clamp01(float value) {
      return Math.max(0F, Math.min(1F, value));
   }

   private static HudAnchor safeAnchor(HudAnchor anchor) {
      return anchor == null ? HudAnchor.LEFT : anchor;
   }
}
