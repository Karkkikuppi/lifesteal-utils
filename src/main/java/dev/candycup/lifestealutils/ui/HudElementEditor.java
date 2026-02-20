package dev.candycup.lifestealutils.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.ui.framework.screens.DrawableScreen;
import dev.candycup.lifestealutils.features.qol.PoiDirectionalIndicator;
import dev.candycup.lifestealutils.interapi.SoundUtils;
import dev.candycup.lifestealutils.ui.util.UiInteractionUtils;
import dev.candycup.lifestealutils.ui.util.UiRenderUtils;
import dev.candycup.lifestealutils.ui.editor.HudEditorBackdrop;
import dev.candycup.lifestealutils.ui.editor.HudEditorCanvas;
import dev.candycup.lifestealutils.ui.editor.HudEditorGrid;
import dev.candycup.lifestealutils.ui.editor.HudEditorState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * provides the HUD editor screen using the drawable UI system.
 */
public class HudElementEditor extends DrawableScreen {
   public static final Identifier EDITOR_LAYER_ID = Identifier.fromNamespaceAndPath("lifestealutils", "hud_editor");
   private static final WidgetSprites PRIMARY_BUTTON_SPRITES = new WidgetSprites(
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_primary/button"),
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_primary/button_disabled"),
           Identifier.fromNamespaceAndPath("lifestealutils", "widget/button_primary/button_highlighted")
   );
   private static final int GRID_SPACING_PIXELS = 32;
   private static final int SNAP_STEP_PIXELS = GRID_SPACING_PIXELS / 2;
   private static final int SNAP_BUTTON_SPACING = 6;
   private static final int HEADER_PADDING = 8;
   private static final int TITLE_SPACING = 2;
   private static final int BUTTON_ROW_SPACING = SNAP_BUTTON_SPACING + 2;

   private static PoiDirectionalIndicator poiDirectionalIndicator;

   private final HudEditorState state = new HudEditorState();

   public HudElementEditor(Component component) {
      super(component);
   }

   /**
    * sets the POI directional indicator for preview rendering in the editor.
    *
    * @param indicator the directional indicator to render
    */
   public static void setPoiDirectionalIndicator(PoiDirectionalIndicator indicator) {
      poiDirectionalIndicator = indicator;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(null);
      state.reset();
   }

   @Override
   public void renderBlurredBackground(GuiGraphics guiGraphics) {
   }

   /**
    * builds the hud editor ui tree.
    *
    * @return the root drawable
    */
   @Override
   protected Drawable buildUi() {
      HudEditorCanvas canvas = HudEditorCanvas.builder()
              .state(state)
              .poiIndicator(poiDirectionalIndicator)
              .snapStepPixels(SNAP_STEP_PIXELS)
              .build();

      HudEditorActionButton snapButton = new HudEditorActionButton(
              () -> Component.translatable(state.isSnappingActive() ? "lsu.hudEditor.snap.on" : "lsu.hudEditor.snap.off"),
              state::toggleSnapEnabled
      );
      HudEditorActionButton configButton = new HudEditorActionButton(
              () -> Component.translatable("lsu.hudEditor.openConfig"),
              () -> {
                 SoundUtils.playUiClick();
                 sendConfigCommand(Minecraft.getInstance());
              }
      );

      return new HudEditorRoot(HudEditorBackdrop.create(), HudEditorGrid.builder().spacing(GRID_SPACING_PIXELS).build(), canvas, snapButton, configButton);
   }

   public static HudElement editorLayer() {
      return (drawContext, tickCounter) -> {
      };
   }

   /**
    * sends the config command using the client connection.
    *
    * @param minecraft the minecraft client
    */
   private static void sendConfigCommand(Minecraft minecraft) {
      if (minecraft.player == null || minecraft.player.connection == null) {
         return;
      }
      minecraft.player.connection.sendCommand("lsu config");
   }

   private static final class HudEditorRoot implements Drawable {
      private static final Component TITLE_TEXT = Component.translatable("lsu.hudEditor.title");
      private static final Component SUBTITLE_TEXT = Component.translatable("lsu.hudEditor.subtitle");

      private final Drawable backdrop;
      private final Drawable grid;
      private final Drawable canvas;
      private final HudEditorActionButton snapButton;
      private final HudEditorActionButton configButton;

      private UiBounds bounds = UiBounds.empty();
      private UiBounds titleBounds = UiBounds.empty();
      private UiBounds subtitleBounds = UiBounds.empty();

      private HudEditorRoot(
              Drawable backdrop,
              Drawable grid,
              Drawable canvas,
              HudEditorActionButton snapButton,
              HudEditorActionButton configButton
      ) {
         this.backdrop = backdrop;
         this.grid = grid;
         this.canvas = canvas;
         this.snapButton = snapButton;
         this.configButton = configButton;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         bounds = layoutContext.availableBounds();
         backdrop.layout(layoutContext.withBounds(bounds));
         grid.layout(layoutContext.withBounds(bounds));
         canvas.layout(layoutContext.withBounds(bounds));

         int titleX = bounds.x() + HEADER_PADDING;
         int titleY = bounds.y() + HEADER_PADDING;
         int titleWidth = layoutContext.font().width(TITLE_TEXT);
         int subtitleWidth = layoutContext.font().width(SUBTITLE_TEXT);
         int lineHeight = layoutContext.font().lineHeight;
         int subtitleY = titleY + lineHeight + TITLE_SPACING;
         titleBounds = new UiBounds(titleX, titleY, titleWidth, lineHeight);
         subtitleBounds = new UiBounds(titleX, subtitleY, subtitleWidth, lineHeight);

         UiSize snapSize = snapButton.preferredSize(layoutContext);
         int buttonsY = subtitleY + lineHeight + BUTTON_ROW_SPACING;
         snapButton.layout(layoutContext.withBounds(new UiBounds(titleX, buttonsY, snapSize.width(), snapSize.height())));

         UiSize configSize = configButton.preferredSize(layoutContext);
         int configX = titleX + snapSize.width() + SNAP_BUTTON_SPACING;
         configButton.layout(layoutContext.withBounds(new UiBounds(configX, buttonsY, configSize.width(), configSize.height())));
      }

      @Override
      public void render(UiContext context) {
         backdrop.render(context);
         grid.render(context);
         canvas.render(context);
         context.graphics().drawString(context.minecraft().font, TITLE_TEXT, titleBounds.x(), titleBounds.y(), 0xFFFFFFFF, true);
         context.graphics().drawString(context.minecraft().font, SUBTITLE_TEXT, subtitleBounds.x(), subtitleBounds.y(), 0xFFFFFFFF, true);
         snapButton.render(context);
         configButton.render(context);
      }

      @Override
      public void handleInput(UiInputState input) {
         snapButton.handleInput(input);
         configButton.handleInput(input);
         canvas.handleInput(input);
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         return new UiSize(layoutContext.availableBounds().width(), layoutContext.availableBounds().height());
      }
   }

   private static final class HudEditorActionButton implements Drawable {
      private static final int PADDING_X = 6;
      private static final int PADDING_Y = 5;

      private final Supplier<Component> labelSupplier;
      private final Runnable onClick;

      private UiBounds bounds = UiBounds.empty();
      private boolean hovered;
      private boolean pressed;

      private HudEditorActionButton(Supplier<Component> labelSupplier, Runnable onClick) {
         this.labelSupplier = labelSupplier;
         this.onClick = onClick;
      }

      @Override
      public void layout(UiLayoutContext layoutContext) {
         Component label = labelSupplier.get();
         int width = layoutContext.font().width(label) + PADDING_X * 2;
         int height = layoutContext.font().lineHeight + PADDING_Y * 2;
         UiBounds available = layoutContext.availableBounds();
         bounds = new UiBounds(available.x(), available.y(), width, height);
      }

      @Override
      public void render(UiContext context) {
         context.graphics().blitSprite(
                 RenderPipelines.GUI_TEXTURED,
                 PRIMARY_BUTTON_SPRITES.get(true, hovered),
                 bounds.x(),
                 bounds.y(),
                 bounds.width(),
                 bounds.height()
         );

         Component label = labelSupplier.get();
         int textX = UiRenderUtils.centeredTextX(context.minecraft().font, label, bounds);
         int textY = UiRenderUtils.centeredTextY(context.minecraft().font, bounds);
         context.graphics().drawString(context.minecraft().font, label, textX, textY, 0xFFFFFFFF, true);
      }

      @Override
      public void handleInput(UiInputState input) {
         hovered = UiInteractionUtils.isHovered(input, bounds);
         boolean wasPressed = pressed;
         pressed = UiInteractionUtils.nextPressedState(pressed, hovered, input);
         if (UiInteractionUtils.shouldClick(wasPressed, hovered, input) && onClick != null) {
            onClick.run();
         }
      }

      @Override
      public UiBounds bounds() {
         return bounds;
      }

      @Override
      public UiSize preferredSize(UiLayoutContext layoutContext) {
         Component label = labelSupplier.get();
         int width = layoutContext.font().width(label) + PADDING_X * 2;
         int height = layoutContext.font().lineHeight + PADDING_Y * 2;
         return new UiSize(width, height);
      }
   }
}
