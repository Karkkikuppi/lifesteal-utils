package dev.candycup.lifestealutils.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
//? if >1.21.8 {
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * very cool radar
 */
public class RadarScreen extends Screen {
   /**
    * half of the total world size to display (40k total = -20k to +20k)
    */
   private static final int WORLD_HALF_SIZE = 20000;

   /**
    * chunk size in blocks
    */
   private static final int CHUNK_SIZE = 16;

   /**
    * minimum number of chunks visible on screen (roughly 20 chunks across)
    */
   private static final int MIN_CHUNKS_VISIBLE = 20;

   /**
    * minimum blocks per screen (zoomed in) - at least 20 chunks visible
    */
   private static final int MIN_BLOCKS_VISIBLE = MIN_CHUNKS_VISIBLE * CHUNK_SIZE; // 320 blocks

   /**
    * maximum blocks per screen (zoomed out) - shows the full 40k area
    */
   private static final int MAX_BLOCKS_VISIBLE = WORLD_HALF_SIZE * 2;

   /**
    * size of the player marker in pixels
    */
   private static final int PLAYER_MARKER_SIZE = 8;

   /**
    * header vertical padding
    */
   private static final int HEADER_TOP_PADDING = 10;

   /**
    * zoom level as the number of blocks visible across the shorter screen dimension
    */
   private double blocksVisible = 1000.0;

   /**
    * smooth zoom target for interpolation
    */
   private double targetBlocksVisible = 1000.0;

   /**
    * zoom interpolation speed (0-1, higher = faster) - snappy, minimal bounce
    */
   private static final double ZOOM_LERP_SPEED = 0.5;

   /**
    * zoom factor per scroll notch
    */
   private static final double ZOOM_SCROLL_FACTOR = 1.3;

   /**
    * camera center x in world coordinates
    */
   private double cameraX = 0.0;

   /**
    * camera center z in world coordinates
    */
   private double cameraZ = 0.0;

   /**
    * whether dragging the map
    */
   private boolean dragging = false;

   /**
    * last mouse x for drag calculations
    */
   private double lastMouseX;

   /**
    * last mouse y for drag calculations
    */
   private double lastMouseY;

   /**
    * current mouse x for hover detection
    */
   private int currentMouseX;

   /**
    * current mouse y for hover detection
    */
   private int currentMouseY;

   /**
    * whether a zoom focus point is active during smooth zoom
    */
   private boolean zoomFocusActive = false;

   /**
    * world x for zoom focus point
    */
   private double zoomFocusWorldX;

   /**
    * world z for zoom focus point
    */
   private double zoomFocusWorldZ;

   /**
    * screen x for zoom focus point
    */
   private double zoomFocusScreenX;

   /**
    * screen y for zoom focus point
    */
   private double zoomFocusScreenY;

   /**
    * time in millis when last zoom/pan activity occurred
    */
   private long lastActivityTime = 0;

   /**
    * whether zoom bar should be visible
    */
   private boolean zoomBarVisible = false;

   /**
    * zoom bar fade duration in millis
    */
   private static final long ZOOM_BAR_FADE_DELAY = 1500;

   /**
    * zoom bar fade out duration in millis
    */
   private static final long ZOOM_BAR_FADE_DURATION = 300;

   /**
    * border thickness for world bounds
    */
   private static final int WORLD_BORDER_THICKNESS = 3;

   public RadarScreen() {
      super(Component.translatable("lifestealutils.radar.title"));
   }

   @Override
   protected void init() {
      super.init();
      // center the camera on the player's position
      LocalPlayer player = this.minecraft.player;
      if (player != null) {
         cameraX = player.getX();
         cameraZ = player.getZ();
      }
   }

   @Override
   public void tick() {
      super.tick();
      // interpolate zoom level
      double diff = Math.abs(blocksVisible - targetBlocksVisible);
      if (diff > 1.0) {
         blocksVisible = Mth.lerp(ZOOM_LERP_SPEED, blocksVisible, targetBlocksVisible);
      } else {
         blocksVisible = targetBlocksVisible;
      }

      boolean isAnimating = Math.abs(blocksVisible - targetBlocksVisible) > 1.0;
      if (!isAnimating || dragging) {
         zoomFocusActive = false;
      }

      if (zoomFocusActive) {
         int headerHeight = calculateHeaderHeight();
         int gridTop = headerHeight;
         int gridHeight = this.height - gridTop;
         int gridWidth = this.width;
         double scale = Math.min(gridWidth, gridHeight) / blocksVisible;
         alignCameraToZoomFocus(gridTop, gridWidth, gridHeight, scale);
      }
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      // render blur like the multiplayer screen (this calls renderBlurredBackground internally)
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      // darken the background
      guiGraphics.fill(0, 0, this.width, this.height, 0x60000000);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      // render the screen background with blur
      super.render(guiGraphics, mouseX, mouseY, partialTick);

      // track mouse position for hover detection
      this.currentMouseX = mouseX;
      this.currentMouseY = mouseY;

      LocalPlayer player = this.minecraft.player;
      if (player == null) return;

      // calculate the grid area (below the header)
      int headerHeight = calculateHeaderHeight();
      int gridTop = headerHeight;
      int gridHeight = this.height - gridTop;
      int gridWidth = this.width;

      // calculate scale for this frame
      double scale = Math.min(gridWidth, gridHeight) / blocksVisible;

      // render grid
      renderGrid(guiGraphics, gridTop, gridWidth, gridHeight, scale);

      // render world border
      renderWorldBorder(guiGraphics, gridTop, gridWidth, gridHeight, scale);

      // render hovered chunk highlight and tooltip
      renderHoveredChunk(guiGraphics, player, gridTop, gridWidth, gridHeight, scale);

      // render player marker
      renderPlayerMarker(guiGraphics, player, gridTop, gridWidth, gridHeight, scale);

      // render header (after grid so it's on top)
      renderHeader(guiGraphics, player);

      // render zoom bar
      renderZoomBar(guiGraphics);
   }

   /**
    * Calculates the header height based on font metrics.
    */
   private int calculateHeaderHeight() {
      int lineHeight = this.font.lineHeight;
      // title + coordinates + some padding
      return HEADER_TOP_PADDING + lineHeight + 2 + lineHeight + 10;
   }

   /**
    * Renders the header with title and player coordinates.
    */
   private void renderHeader(GuiGraphics guiGraphics, LocalPlayer player) {
      // semi-transparent background for header
      int headerHeight = calculateHeaderHeight();
      guiGraphics.fill(0, 0, this.width, headerHeight, 0xD0000000);

      // title
      Component title = Component.translatable("lifestealutils.radar.title");
      int titleWidth = this.font.width(title);
      int titleX = (this.width - titleWidth) / 2;
      int titleY = HEADER_TOP_PADDING;
      guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFFFF, true);

      // player coordinates
      int playerX = Mth.floor(player.getX());
      int playerY = Mth.floor(player.getY());
      int playerZ = Mth.floor(player.getZ());
      String coordsText = String.format("X: %d  Y: %d  Z: %d", playerX, playerY, playerZ);
      int coordsWidth = this.font.width(coordsText);
      int coordsX = (this.width - coordsWidth) / 2;
      int coordsY = titleY + this.font.lineHeight + 2;
      guiGraphics.drawString(this.font, coordsText, coordsX, coordsY, 0xFFAAAAAA, true);
   }

   /**
    * Renders the coordinate grid.
    */
   private void renderGrid(GuiGraphics guiGraphics, int gridTop, int gridWidth, int gridHeight, double scale) {
      // determine grid spacing based on zoom level
      int gridSpacing = calculateGridSpacing(scale);

      // calculate world bounds visible on screen
      double worldLeft = cameraX - (gridWidth / 2.0) / scale;
      double worldRight = cameraX + (gridWidth / 2.0) / scale;
      double worldTop = cameraZ - (gridHeight / 2.0) / scale;
      double worldBottom = cameraZ + (gridHeight / 2.0) / scale;

      // clamp to world bounds for rendering
      double clampedWorldLeft = Math.max(-WORLD_HALF_SIZE, worldLeft);
      double clampedWorldRight = Math.min(WORLD_HALF_SIZE, worldRight);
      double clampedWorldTop = Math.max(-WORLD_HALF_SIZE, worldTop);
      double clampedWorldBottom = Math.min(WORLD_HALF_SIZE, worldBottom);

      // draw vertical grid lines (only within world bounds)
      int startX = Mth.floor(clampedWorldLeft / gridSpacing) * gridSpacing;
      for (int worldX = startX; worldX <= clampedWorldRight; worldX += gridSpacing) {
         if (worldX < -WORLD_HALF_SIZE || worldX > WORLD_HALF_SIZE) continue;
         int screenX = worldToScreenX(worldX, gridWidth, scale);
         if (screenX >= 0 && screenX < gridWidth) {
            int alpha = getGridLineAlpha(worldX, gridSpacing);
            int color = (alpha << 24) | 0xFFFFFF;
            int lineTop = Math.max(gridTop, worldToScreenY(-WORLD_HALF_SIZE, gridTop, gridHeight, scale));
            int lineBottom = Math.min(gridTop + gridHeight, worldToScreenY(WORLD_HALF_SIZE, gridTop, gridHeight, scale));
            if (lineBottom > lineTop) {
               guiGraphics.fill(screenX, lineTop, screenX + 1, lineBottom, color);
            }
         }
      }

      // draw horizontal grid lines (only within world bounds)
      int startZ = Mth.floor(clampedWorldTop / gridSpacing) * gridSpacing;
      for (int worldZ = startZ; worldZ <= clampedWorldBottom; worldZ += gridSpacing) {
         if (worldZ < -WORLD_HALF_SIZE || worldZ > WORLD_HALF_SIZE) continue;
         int screenY = worldToScreenY(worldZ, gridTop, gridHeight, scale);
         if (screenY >= gridTop && screenY < gridTop + gridHeight) {
            int alpha = getGridLineAlpha(worldZ, gridSpacing);
            int color = (alpha << 24) | 0xFFFFFF;
            int lineLeft = Math.max(0, worldToScreenX(-WORLD_HALF_SIZE, gridWidth, scale));
            int lineRight = Math.min(gridWidth, worldToScreenX(WORLD_HALF_SIZE, gridWidth, scale));
            if (lineRight > lineLeft) {
               guiGraphics.fill(lineLeft, screenY, lineRight, screenY + 1, color);
            }
         }
      }

      // draw origin axes with higher opacity (only within world bounds)
      int originScreenX = worldToScreenX(0, gridWidth, scale);
      int originScreenY = worldToScreenY(0, gridTop, gridHeight, scale);
      int worldLeftScreen = worldToScreenX(-WORLD_HALF_SIZE, gridWidth, scale);
      int worldRightScreen = worldToScreenX(WORLD_HALF_SIZE, gridWidth, scale);
      int worldTopScreen = worldToScreenY(-WORLD_HALF_SIZE, gridTop, gridHeight, scale);
      int worldBottomScreen = worldToScreenY(WORLD_HALF_SIZE, gridTop, gridHeight, scale);

      // x-axis (z = 0) - red
      if (originScreenY >= gridTop && originScreenY < gridTop + gridHeight) {
         int axisLeft = Math.max(0, worldLeftScreen);
         int axisRight = Math.min(gridWidth, worldRightScreen);
         if (axisRight > axisLeft) {
            guiGraphics.fill(axisLeft, originScreenY, axisRight, originScreenY + 2, 0xA0FF4444);
         }
      }
      // z-axis (x = 0) - blue
      if (originScreenX >= 0 && originScreenX < gridWidth) {
         int axisTop = Math.max(gridTop, worldTopScreen);
         int axisBottom = Math.min(gridTop + gridHeight, worldBottomScreen);
         if (axisBottom > axisTop) {
            guiGraphics.fill(originScreenX, axisTop, originScreenX + 2, axisBottom, 0xA04444FF);
         }
      }

      // draw coordinate labels
      renderGridLabels(guiGraphics, gridTop, gridWidth, gridHeight, scale, gridSpacing,
              clampedWorldLeft, clampedWorldRight, clampedWorldTop, clampedWorldBottom);
   }

   /**
    * Renders the blue world border at ±20,000 blocks.
    */
   private void renderWorldBorder(GuiGraphics guiGraphics, int gridTop, int gridWidth, int gridHeight, double scale) {
      int borderColor = 0xFF4488FF; // bright blue

      // calculate screen positions of world bounds
      int leftEdge = worldToScreenX(-WORLD_HALF_SIZE, gridWidth, scale);
      int rightEdge = worldToScreenX(WORLD_HALF_SIZE, gridWidth, scale);
      int topEdge = worldToScreenY(-WORLD_HALF_SIZE, gridTop, gridHeight, scale);
      int bottomEdge = worldToScreenY(WORLD_HALF_SIZE, gridTop, gridHeight, scale);

      // left border (x = -20000)
      if (leftEdge >= 0 && leftEdge < gridWidth) {
         int top = Math.max(gridTop, topEdge);
         int bottom = Math.min(gridTop + gridHeight, bottomEdge);
         if (bottom > top) {
            guiGraphics.fill(leftEdge, top, leftEdge + WORLD_BORDER_THICKNESS, bottom, borderColor);
         }
      }

      // right border (x = 20000)
      if (rightEdge >= 0 && rightEdge < gridWidth) {
         int top = Math.max(gridTop, topEdge);
         int bottom = Math.min(gridTop + gridHeight, bottomEdge);
         if (bottom > top) {
            guiGraphics.fill(rightEdge - WORLD_BORDER_THICKNESS, top, rightEdge, bottom, borderColor);
         }
      }

      // top border (z = -20000)
      if (topEdge >= gridTop && topEdge < gridTop + gridHeight) {
         int left = Math.max(0, leftEdge);
         int right = Math.min(gridWidth, rightEdge);
         if (right > left) {
            guiGraphics.fill(left, topEdge, right, topEdge + WORLD_BORDER_THICKNESS, borderColor);
         }
      }

      // bottom border (z = 20000)
      if (bottomEdge >= gridTop && bottomEdge < gridTop + gridHeight) {
         int left = Math.max(0, leftEdge);
         int right = Math.min(gridWidth, rightEdge);
         if (right > left) {
            guiGraphics.fill(left, bottomEdge - WORLD_BORDER_THICKNESS, right, bottomEdge, borderColor);
         }
      }

      // darken areas outside the world bounds
      int outsideColor = 0xA0000000;

      // left outside area
      if (leftEdge > 0) {
         guiGraphics.fill(0, gridTop, Math.min(leftEdge, gridWidth), gridTop + gridHeight, outsideColor);
      }
      // right outside area
      if (rightEdge < gridWidth) {
         guiGraphics.fill(Math.max(0, rightEdge), gridTop, gridWidth, gridTop + gridHeight, outsideColor);
      }
      // top outside area (between left and right borders)
      if (topEdge > gridTop) {
         int left = Math.max(0, leftEdge);
         int right = Math.min(gridWidth, rightEdge);
         if (right > left) {
            guiGraphics.fill(left, gridTop, right, Math.min(topEdge, gridTop + gridHeight), outsideColor);
         }
      }
      // bottom outside area (between left and right borders)
      if (bottomEdge < gridTop + gridHeight) {
         int left = Math.max(0, leftEdge);
         int right = Math.min(gridWidth, rightEdge);
         if (right > left) {
            guiGraphics.fill(left, Math.max(gridTop, bottomEdge), right, gridTop + gridHeight, outsideColor);
         }
      }
   }

   /**
    * Calculates an appropriate grid spacing based on the current scale.
    * Uses powers of 2 multiplied by common factors to create nice grid sizes.
    */
   private int calculateGridSpacing(double scale) {
      // target ~48-96 pixels between grid lines
      double targetWorldSpacing = 64.0 / scale;

      // snap to nice values
      int[] niceValues = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384};
      for (int value : niceValues) {
         if (value >= targetWorldSpacing) {
            return value;
         }
      }
      return 16384;
   }

   /**
    * Gets the alpha value for a grid line based on whether it's on a major grid line.
    */
   private int getGridLineAlpha(int coord, int gridSpacing) {
      // major lines (at larger intervals) are more visible
      if (coord == 0) return 0x90; // origin
      if (coord % (gridSpacing * 4) == 0) return 0x60;
      if (coord % (gridSpacing * 2) == 0) return 0x40;
      return 0x25;
   }

   /**
    * Renders coordinate labels at grid intersections.
    */
   private void renderGridLabels(GuiGraphics guiGraphics, int gridTop, int gridWidth, int gridHeight,
                                 double scale, int gridSpacing, double worldLeft, double worldRight,
                                 double worldTop, double worldBottom) {
      // determine label spacing based on zoom - more labels visible now
      int labelSpacing = gridSpacing * 2;
      // ensure minimum reasonable pixel spacing between labels
      double pixelSpacing = labelSpacing * scale;
      while (pixelSpacing < 60 && labelSpacing < 16384) {
         labelSpacing *= 2;
         pixelSpacing = labelSpacing * scale;
      }

      int startX = Mth.floor(worldLeft / labelSpacing) * labelSpacing;
      int startZ = Mth.floor(worldTop / labelSpacing) * labelSpacing;

      // draw x-axis labels (top of grid area)
      for (int worldX = startX; worldX <= worldRight; worldX += labelSpacing) {
         if (worldX < -WORLD_HALF_SIZE || worldX > WORLD_HALF_SIZE) continue;
         int screenX = worldToScreenX(worldX, gridWidth, scale);
         if (screenX >= 5 && screenX < gridWidth - 40) {
            String label = String.valueOf(worldX);
            // background for readability
            int labelWidth = this.font.width(label);
            guiGraphics.fill(screenX + 1, gridTop + 1, screenX + labelWidth + 3, gridTop + this.font.lineHeight + 2, 0xA0000000);
            guiGraphics.drawString(this.font, label, screenX + 2, gridTop + 2, 0xFFFFFFFF, false);
         }
      }

      // draw z-axis labels (left side of grid area)
      for (int worldZ = startZ; worldZ <= worldBottom; worldZ += labelSpacing) {
         if (worldZ < -WORLD_HALF_SIZE || worldZ > WORLD_HALF_SIZE) continue;
         int screenY = worldToScreenY(worldZ, gridTop, gridHeight, scale);
         if (screenY >= gridTop + this.font.lineHeight + 5 && screenY < gridTop + gridHeight - this.font.lineHeight) {
            String label = String.valueOf(worldZ);
            int labelWidth = this.font.width(label);
            // background for readability
            guiGraphics.fill(1, screenY + 1, labelWidth + 4, screenY + this.font.lineHeight + 2, 0xA0000000);
            guiGraphics.drawString(this.font, label, 2, screenY + 2, 0xFFFFFFFF, false);
         }
      }
   }

   /**
    * Renders the hovered chunk highlight and tooltip above the chunk.
    */
   private void renderHoveredChunk(GuiGraphics guiGraphics, LocalPlayer player, int gridTop, int gridWidth, int gridHeight, double scale) {
      // only if mouse is in grid area
      if (currentMouseY < gridTop || currentMouseY >= gridTop + gridHeight ||
              currentMouseX < 0 || currentMouseX >= gridWidth) {
         return;
      }

      // convert mouse position to world coordinates
      double worldX = screenToWorldX(currentMouseX, gridWidth, scale);
      double worldZ = screenToWorldZ(currentMouseY, gridTop, gridHeight, scale);

      // check if within world bounds
      if (worldX < -WORLD_HALF_SIZE || worldX > WORLD_HALF_SIZE ||
              worldZ < -WORLD_HALF_SIZE || worldZ > WORLD_HALF_SIZE) {
         return;
      }

      // find chunk boundaries
      int chunkX = Mth.floor(worldX / CHUNK_SIZE);
      int chunkZ = Mth.floor(worldZ / CHUNK_SIZE);
      int chunkWorldX = chunkX * CHUNK_SIZE;
      int chunkWorldZ = chunkZ * CHUNK_SIZE;

      // convert chunk boundaries to screen coordinates
      int chunkScreenLeft = worldToScreenX(chunkWorldX, gridWidth, scale);
      int chunkScreenTop = worldToScreenY(chunkWorldZ, gridTop, gridHeight, scale);
      int chunkScreenRight = worldToScreenX(chunkWorldX + CHUNK_SIZE, gridWidth, scale);
      int chunkScreenBottom = worldToScreenY(chunkWorldZ + CHUNK_SIZE, gridTop, gridHeight, scale);

      // clamp to grid bounds
      int clampedLeft = Math.max(0, chunkScreenLeft);
      int clampedTop = Math.max(gridTop, chunkScreenTop);
      int clampedRight = Math.min(gridWidth, chunkScreenRight);
      int clampedBottom = Math.min(gridTop + gridHeight, chunkScreenBottom);

      // only render if chunk is visible
      if (clampedRight > clampedLeft && clampedBottom > clampedTop) {
         // semi-transparent yellow highlight
         guiGraphics.fill(clampedLeft, clampedTop, clampedRight, clampedBottom, 0x40FFFF00);

         // yellow border
         int borderColor = 0xC0FFFF00;
         // top
         guiGraphics.fill(clampedLeft, clampedTop, clampedRight, clampedTop + 1, borderColor);
         // bottom
         guiGraphics.fill(clampedLeft, clampedBottom - 1, clampedRight, clampedBottom, borderColor);
         // left
         guiGraphics.fill(clampedLeft, clampedTop, clampedLeft + 1, clampedBottom, borderColor);
         // right
         guiGraphics.fill(clampedRight - 1, clampedTop, clampedRight, clampedBottom, borderColor);
      }

      // render tooltip above the hovered chunk
      int blockX = Mth.floor(worldX);
      int blockZ = Mth.floor(worldZ);
      double distanceToBlock = Math.sqrt(Math.pow(blockX - player.getX(), 2) + Math.pow(blockZ - player.getZ(), 2));
      String tooltipText = String.format("%d, %d | %d blocks away", blockX, blockZ, (int) distanceToBlock);
      int tooltipWidth = this.font.width(tooltipText);
      int tooltipPadding = 4;
      int tooltipHeight = this.font.lineHeight + tooltipPadding * 2;
      int totalWidth = tooltipWidth + tooltipPadding * 2;

      // position tooltip above the chunk (or above cursor if chunk is too small)
      int tooltipX = Math.max(2, Math.min(gridWidth - totalWidth - 2,
              (chunkScreenLeft + chunkScreenRight) / 2 - totalWidth / 2));
      int tooltipY = Math.max(gridTop + 2, chunkScreenTop - tooltipHeight - 4);

      // if tooltip would be off screen at top, put it below the chunk
      if (tooltipY < gridTop + 2) {
         tooltipY = Math.min(gridTop + gridHeight - tooltipHeight - 2, chunkScreenBottom + 4);
      }

      // draw tooltip background
      guiGraphics.fill(tooltipX, tooltipY, tooltipX + totalWidth, tooltipY + tooltipHeight, 0xE0000000);
      // draw tooltip border
      int tooltipBorderColor = 0xFF888888;
      guiGraphics.fill(tooltipX, tooltipY, tooltipX + totalWidth, tooltipY + 1, tooltipBorderColor);
      guiGraphics.fill(tooltipX, tooltipY + tooltipHeight - 1, tooltipX + totalWidth, tooltipY + tooltipHeight, tooltipBorderColor);
      guiGraphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, tooltipBorderColor);
      guiGraphics.fill(tooltipX + totalWidth - 1, tooltipY, tooltipX + totalWidth, tooltipY + tooltipHeight, tooltipBorderColor);

      // draw tooltip text
      guiGraphics.drawString(this.font, tooltipText, tooltipX + tooltipPadding, tooltipY + tooltipPadding, 0xFF88FFFF, false);
   }

   /**
    * Renders the zoom bar at the bottom center.
    */
   private void renderZoomBar(GuiGraphics guiGraphics) {
      long currentTime = System.currentTimeMillis();
      long timeSinceActivity = currentTime - lastActivityTime;

      // check if zoom is still animating
      boolean isAnimating = Math.abs(blocksVisible - targetBlocksVisible) > 1.0;
      if (isAnimating) {
         lastActivityTime = currentTime;
         timeSinceActivity = 0;
      }

      // determine alpha based on fade state
      float alpha;
      if (timeSinceActivity < ZOOM_BAR_FADE_DELAY) {
         alpha = 1.0f;
         zoomBarVisible = true;
      } else if (timeSinceActivity < ZOOM_BAR_FADE_DELAY + ZOOM_BAR_FADE_DURATION) {
         alpha = 1.0f - (float) (timeSinceActivity - ZOOM_BAR_FADE_DELAY) / ZOOM_BAR_FADE_DURATION;
         zoomBarVisible = true;
      } else {
         alpha = 0.0f;
         zoomBarVisible = false;
      }

      if (!zoomBarVisible) return;

      int alphaInt = (int) (alpha * 255);
      if (alphaInt <= 0) return;

      // zoom bar dimensions
      int barWidth = 200;
      int barHeight = 8;
      int barX = (this.width - barWidth) / 2;
      int barY = this.height - 30;

      // calculate zoom progress (logarithmic scale)
      double minLog = Math.log(MIN_BLOCKS_VISIBLE);
      double maxLog = Math.log(MAX_BLOCKS_VISIBLE);
      double currentLog = Math.log(blocksVisible);
      double progress = (currentLog - minLog) / (maxLog - minLog);
      progress = 1.0 - progress; // invert so zoomed in = more filled

      // colors with alpha
      int bgColor = (alphaInt * 3 / 4 << 24) | 0x000000;
      int borderColor = (alphaInt << 24) | 0x888888;
      int fillColor = (alphaInt << 24) | 0x44AAFF;
      int textColor = (alphaInt << 24) | 0xFFFFFF;

      // draw background
      guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, bgColor);

      // draw fill
      int fillWidth = (int) (barWidth * progress);
      if (fillWidth > 0) {
         guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
      }

      // draw border
      guiGraphics.fill(barX, barY, barX + barWidth, barY + 1, borderColor);
      guiGraphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, borderColor);
      guiGraphics.fill(barX, barY, barX + 1, barY + barHeight, borderColor);
      guiGraphics.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, borderColor);

      // draw zoom level text above the bar, left-aligned
      int zoomPercentage = (int) (progress * 100);
      String zoomText = "Zoom: " + zoomPercentage + "%";
      guiGraphics.drawString(this.font, zoomText, barX, barY - this.font.lineHeight - 2, textColor, false);
   }

   /**
    * Converts world X coordinate to screen X coordinate.
    */
   private int worldToScreenX(double worldX, int gridWidth, double scale) {
      return (int) ((worldX - cameraX) * scale + gridWidth / 2.0);
   }

   /**
    * Converts world Z coordinate to screen Y coordinate.
    */
   private int worldToScreenY(double worldZ, int gridTop, int gridHeight, double scale) {
      return (int) ((worldZ - cameraZ) * scale + gridHeight / 2.0) + gridTop;
   }

   /**
    * Converts screen X coordinate to world X coordinate.
    */
   private double screenToWorldX(double screenX, int gridWidth, double scale) {
      return (screenX - gridWidth / 2.0) / scale + cameraX;
   }

   /**
    * Converts screen Y coordinate to world Z coordinate.
    */
   private double screenToWorldZ(double screenY, int gridTop, int gridHeight, double scale) {
      return (screenY - gridTop - gridHeight / 2.0) / scale + cameraZ;
   }

   /**
    * Aligns the camera so the zoom focus world position stays under the focus screen position.
    */
   private void alignCameraToZoomFocus(int gridTop, int gridWidth, int gridHeight, double scale) {
      cameraX = zoomFocusWorldX - (zoomFocusScreenX - gridWidth / 2.0) / scale;
      cameraZ = zoomFocusWorldZ - (zoomFocusScreenY - gridTop - gridHeight / 2.0) / scale;
   }

   /**
    * Renders the player marker on the grid.
    */
   private void renderPlayerMarker(GuiGraphics guiGraphics, LocalPlayer player, int gridTop, int gridWidth, int gridHeight, double scale) {
      double playerX = player.getX();
      double playerZ = player.getZ();

      int screenX = worldToScreenX(playerX, gridWidth, scale);
      int screenY = worldToScreenY(playerZ, gridTop, gridHeight, scale);

      // marker is a fixed size regardless of zoom
      int halfSize = PLAYER_MARKER_SIZE / 2;
      int left = screenX - halfSize;
      int top = screenY - halfSize;
      int right = screenX + halfSize;
      int bottom = screenY + halfSize;

      // only render if within grid bounds
      if (screenX >= -halfSize && screenX < gridWidth + halfSize &&
              screenY >= gridTop - halfSize && screenY < gridTop + gridHeight + halfSize) {
         // green marker
         guiGraphics.fill(left, top, right, bottom, 0xFF00FF00);
         // darker green border
         guiGraphics.fill(left, top, right, top + 1, 0xFF008800);
         guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF008800);
         guiGraphics.fill(left, top, left + 1, bottom, 0xFF008800);
         guiGraphics.fill(right - 1, top, right, bottom, 0xFF008800);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0) {
         // mark activity for zoom bar
         lastActivityTime = System.currentTimeMillis();

         // calculate grid dimensions
         int headerHeight = calculateHeaderHeight();
         int gridTop = headerHeight;
         int gridHeight = this.height - gridTop;
         int gridWidth = this.width;
         double scale = Math.min(gridWidth, gridHeight) / blocksVisible;

         // zoom in/out
         if (scrollY > 0) {
            // scroll up = zoom in (fewer blocks visible)
            targetBlocksVisible = Math.max(MIN_BLOCKS_VISIBLE, targetBlocksVisible / ZOOM_SCROLL_FACTOR);
         } else {
            // scroll down = zoom out (more blocks visible)
            targetBlocksVisible = Math.min(MAX_BLOCKS_VISIBLE, targetBlocksVisible * ZOOM_SCROLL_FACTOR);
         }

         boolean inGrid = mouseX >= 0 && mouseX < gridWidth && mouseY >= gridTop && mouseY < gridTop + gridHeight;
         if (inGrid) {
            // set zoom focus to keep the cursor stable during the smooth zoom animation
            double worldXUnderCursor = screenToWorldX(mouseX, gridWidth, scale);
            double worldZUnderCursor = screenToWorldZ(mouseY, gridTop, gridHeight, scale);
            zoomFocusActive = true;
            zoomFocusWorldX = worldXUnderCursor;
            zoomFocusWorldZ = worldZUnderCursor;
            zoomFocusScreenX = mouseX;
            zoomFocusScreenY = mouseY;
         } else {
            zoomFocusActive = false;
         }

         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   //? if >1.21.8 {
   @Override
   public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
      if (mouseButtonEvent.button() == 0) { // left button
         double mouseX = mouseButtonEvent.x();
         double mouseY = mouseButtonEvent.y();
         handleDrag(mouseX, mouseY, dragX, dragY);
         return true;
      }
      return super.mouseDragged(mouseButtonEvent, dragX, dragY);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
      if (mouseButtonEvent.button() == 0) {
         dragging = false;
      }
      return super.mouseReleased(mouseButtonEvent);
   }
   //?} else {
    /*@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) { // left button
            handleDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?}

   /**
    * Handles map dragging logic.
    */
   private void handleDrag(double mouseX, double mouseY, double dragX, double dragY) {
      // mark activity for zoom bar
      lastActivityTime = System.currentTimeMillis();
      zoomFocusActive = false;

      if (!dragging) {
         dragging = true;
         lastMouseX = mouseX;
         lastMouseY = mouseY;
      }

      int headerHeight = calculateHeaderHeight();
      int gridHeight = this.height - headerHeight;
      int gridWidth = this.width;
      double scale = Math.min(gridWidth, gridHeight) / blocksVisible;

      // move camera opposite to drag direction
      cameraX -= dragX / scale;
      cameraZ -= dragY / scale;

      lastMouseX = mouseX;
      lastMouseY = mouseY;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
