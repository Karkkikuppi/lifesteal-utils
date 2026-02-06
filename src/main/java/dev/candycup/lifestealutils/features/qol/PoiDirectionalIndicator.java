package dev.candycup.lifestealutils.features.qol;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.hud.HudElementManager;
import dev.candycup.lifestealutils.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renders a directional compass indicator that points toward the currently tracked POI.
 * Uses Minecraft's compass sprite textures (compass_00 to compass_31) to show direction.
 * <p>
 * Compass sprite mapping:
 * - compass_00: pointing down (south, behind player)
 * - compass_16: pointing up (north, in front of player)
 * - sprites interpolate clockwise from 0 to 31
 * <p>
 */
public final class PoiDirectionalIndicator {
   /**
    * HUD element id for the directional indicator - used for position storage.
    */
   public static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath(
           "lifestealutils",
           PoiWaypointTracker.CONFIG_ID + "_indicator"
   );

   /**
    * Total number of compass sprite frames.
    */
   private static final int COMPASS_FRAME_COUNT = 32;

   /**
    * Size of the compass texture in pixels.
    */
   private static final int TEXTURE_SIZE = 16;

   /**
    * Default horizontal offset from the text element when first placed.
    */
   private static final float DEFAULT_INDICATOR_X = 0.457F;

   /**
    * Default vertical offset to align with the text element.
    */
   private static final float DEFAULT_INDICATOR_Y = 0.645F;

   private final PoiWaypointTracker tracker;

   /**
    * Creates a new directional indicator tied to the given tracker.
    *
    * @param tracker the poi waypoint tracker to get target information from
    */
   public PoiDirectionalIndicator(PoiWaypointTracker tracker) {
      this.tracker = tracker;
   }

   /**
    * Gets the default position for the indicator, relative to the text element.
    *
    * @param guiWidth  the current gui width
    * @param guiHeight the current gui height
    * @return the default position
    */
   public HudPosition getDefaultPosition(int guiWidth, int guiHeight) {
      return HudPosition.clamp(DEFAULT_INDICATOR_X, DEFAULT_INDICATOR_Y);
   }

   /**
    * Ensures the indicator has a position registered in the HUD manager.
    * If no position exists, uses the default position next to the text element.
    *
    * @param guiWidth  the current gui width
    * @param guiHeight the current gui height
    */
   public void ensurePositionRegistered(int guiWidth, int guiHeight) {
      if (!HudElementManager.hasPosition(HUD_ELEMENT_ID)) {
         HudPosition defaultPos = getDefaultPosition(guiWidth, guiHeight);
         HudElementManager.registerPosition(HUD_ELEMENT_ID, defaultPos);
      }
   }

   /**
    * Renders the directional indicator if a POI is being tracked.
    *
    * @param graphics  the gui graphics context
    * @param guiWidth  the current gui width
    * @param guiHeight the current gui height
    */
   public void render(GuiGraphics graphics, int guiWidth, int guiHeight) {
      if (!Config.isPoiHudCompassEnabled()) {
         return;
      }
      if (tracker.isIndicatorsSuppressedForShard()) {
         return;
      }

      PoiRepository.Poi target = tracker.getCurrentTarget();
      if (target == null) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return;
      }

      ensurePositionRegistered(guiWidth, guiHeight);

      // calculate angle from player to POI
      double dx = target.x() - client.player.getX();
      double dz = target.z() - client.player.getZ();
      double angleToTarget = Math.atan2(-dx, dz); // angle in radians, 0 = north (+Z)

      // get player's yaw and convert to radians
      float playerYaw = client.player.getYRot();
      double playerYawRad = Math.toRadians(playerYaw);

      // relative angle: difference between where player is looking and where POI is
      // positive = POI is to the right, negative = POI is to the left
      double relativeAngle = angleToTarget - playerYawRad;

      // normalize to [0, 2*PI)
      relativeAngle = relativeAngle % (2 * Math.PI);
      if (relativeAngle < 0) {
         relativeAngle += 2 * Math.PI;
      }

      // convert to compass frame index
      // frame 0 = down (PI), frame 16 = up (0)
      // the compass starts at "down" and goes clockwise
      // shift by PI to make 0 = down, then scale to frame count
      double shiftedAngle = (relativeAngle + Math.PI) % (2 * Math.PI);
      int frameIndex = (int) Math.round(shiftedAngle / (2 * Math.PI) * COMPASS_FRAME_COUNT) % COMPASS_FRAME_COUNT;

      // get the compass texture for this frame
      Identifier compassTexture = getCompassTexture(frameIndex);

      // get position from HUD manager
      HudPosition position = HudElementManager.positionFor(HUD_ELEMENT_ID);
      int indicatorX = pixelCoordinate(position.x(), guiWidth, TEXTURE_SIZE);
      int indicatorY = pixelCoordinate(position.y(), guiHeight, TEXTURE_SIZE);

      // draw the compass texture
      graphics.blit(
              RenderPipelines.GUI_TEXTURED,
              compassTexture,
              indicatorX, indicatorY,
              0, 0,
              TEXTURE_SIZE, TEXTURE_SIZE,
              TEXTURE_SIZE, TEXTURE_SIZE
      );
   }

   /**
    * Gets the compass texture identifier for the given frame index.
    *
    * @param frameIndex the frame index (0-31)
    * @return the texture identifier
    */
   private Identifier getCompassTexture(int frameIndex) {
      // format: compass_00 to compass_31
      String frameName = String.format("compass_%02d", frameIndex);
      return Identifier.fromNamespaceAndPath("minecraft", "textures/item/" + frameName + ".png");
   }

   /**
    * Calculates pixel coordinate from normalized position.
    *
    * @param normalized  the normalized coordinate (0-1)
    * @param guiSize     the total gui size
    * @param elementSize the element size
    * @return the pixel coordinate
    */
   private int pixelCoordinate(float normalized, int guiSize, int elementSize) {
      int available = Math.max(guiSize - elementSize, 0);
      float clamped = Mth.clamp(normalized, 0F, 1F);
      return Mth.floor(clamped * available);
   }

   /**
    * Gets the size of the indicator texture.
    *
    * @return the texture size in pixels
    */
   public int getTextureSize() {
      return TEXTURE_SIZE;
   }

   /**
    * Gets the HUD element id for this indicator.
    *
    * @return the hud element id
    */
   public Identifier getHudElementId() {
      return HUD_ELEMENT_ID;
   }

   /**
    * Checks if the indicator should be visible.
    *
    * @return true if the indicator should render
    */
   public boolean isVisible() {
      if (!Config.isPoiHudCompassEnabled()) {
         return false;
      }
      if (tracker.isIndicatorsSuppressedForShard()) {
         return false;
      }
      return tracker.getCurrentTarget() != null;
   }
}
