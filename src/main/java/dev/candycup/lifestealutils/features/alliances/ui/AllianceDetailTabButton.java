package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import dev.candycup.lifestealutils.ui.util.UiInteractionUtils;
import dev.candycup.lifestealutils.ui.util.UiRenderUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * renders a tab button with active styling.
 */
final class AllianceDetailTabButton implements Drawable {
   private final Supplier<Component> labelSupplier;
   private final Runnable onClick;
   private final BooleanSupplier activeSupplier;

   private UiBounds bounds = UiBounds.empty();
   private boolean hovered;
   private boolean pressed;

   AllianceDetailTabButton(Supplier<Component> labelSupplier, Runnable onClick, BooleanSupplier activeSupplier) {
      this.labelSupplier = labelSupplier;
      this.onClick = onClick;
      this.activeSupplier = activeSupplier;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      UiBounds available = layoutContext.availableBounds();
      this.bounds = new UiBounds(available.x(), available.y(), available.width(), AllianceDetailStyle.TAB_HEIGHT);
   }

   @Override
   public void render(UiContext context) {
      boolean active = activeSupplier.getAsBoolean();
      context.graphics().blitSprite(
              RenderPipelines.GUI_TEXTURED,
              (active ? AllianceDetailStyle.BUTTON_PRIMARY_SPRITES : AllianceDetailStyle.BUTTON_SECONDARY_SPRITES).get(true, hovered),
              bounds.x(),
              bounds.y(),
              bounds.width(),
              bounds.height()
      );

      Component label = labelSupplier.get();
      int textX = UiRenderUtils.centeredTextX(context.minecraft().font, label, bounds);
      int textY = UiRenderUtils.centeredTextY(context.minecraft().font, bounds);
      context.graphics().drawString(context.minecraft().font, label, textX, textY, AllianceDetailStyle.BUTTON_TEXT, true);
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
      int textWidth = layoutContext.font().width(label);
      int width = Math.max(textWidth + AllianceDetailStyle.BUTTON_PADDING_X * 2, AllianceDetailStyle.TAB_MIN_WIDTH);
      return new UiSize(width, AllianceDetailStyle.TAB_HEIGHT);
   }
}
