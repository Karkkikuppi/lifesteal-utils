package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * draws a fixed-size detail button.
 */
final class AllianceDetailButton implements Drawable {
   private final Supplier<Component> labelSupplier;
   private final Runnable onClick;
   private final BooleanSupplier enabledSupplier;
   private final Integer fixedWidth;
   private final int height;
   private final boolean primary;

   private UiBounds bounds = UiBounds.empty();
   private boolean hovered;
   private boolean pressed;

   static AllianceDetailButton primary(Supplier<Component> labelSupplier, Runnable onClick, BooleanSupplier enabledSupplier) {
      return new AllianceDetailButton(
              labelSupplier,
              onClick,
              enabledSupplier,
              null,
              AllianceDetailStyle.BUTTON_HEIGHT,
              true
      );
   }

   static AllianceDetailButton primaryWithWidth(
           Supplier<Component> labelSupplier,
           Runnable onClick,
           BooleanSupplier enabledSupplier,
           int width
   ) {
      return new AllianceDetailButton(
              labelSupplier,
              onClick,
              enabledSupplier,
              Integer.valueOf(width),
              AllianceDetailStyle.BUTTON_HEIGHT,
              true
      );
   }

   static AllianceDetailButton secondary(Supplier<Component> labelSupplier, Runnable onClick, BooleanSupplier enabledSupplier) {
      return new AllianceDetailButton(
              labelSupplier,
              onClick,
              enabledSupplier,
              null,
              AllianceDetailStyle.BUTTON_HEIGHT,
              false
      );
   }

   static AllianceDetailButton secondaryWithWidth(
           Supplier<Component> labelSupplier,
           Runnable onClick,
           BooleanSupplier enabledSupplier,
           int width
   ) {
      return new AllianceDetailButton(
              labelSupplier,
              onClick,
              enabledSupplier,
              Integer.valueOf(width),
              AllianceDetailStyle.BUTTON_HEIGHT,
              false
      );
   }

   AllianceDetailButton(
           Supplier<Component> labelSupplier,
           Runnable onClick,
           BooleanSupplier enabledSupplier,
           Integer fixedWidth,
           int height,
           boolean primary
   ) {
      this.labelSupplier = labelSupplier;
      this.onClick = onClick;
      this.enabledSupplier = enabledSupplier;
      this.fixedWidth = fixedWidth;
      this.height = height;
      this.primary = primary;
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      UiBounds available = layoutContext.availableBounds();
      int width = resolvedWidth(layoutContext);
      this.bounds = new UiBounds(available.x(), available.y(), width, height);
   }

   @Override
   public void render(UiContext context) {
      boolean enabled = enabledSupplier.getAsBoolean();
      int textColor = AllianceButtonUiSupport.resolveTextColor(
              enabled,
              hovered,
              AllianceDetailStyle.BUTTON_TEXT,
              AllianceDetailStyle.BUTTON_TEXT_HOVER,
              AllianceDetailStyle.BUTTON_TEXT_DISABLED
      );
      context.graphics().blitSprite(
              RenderPipelines.GUI_TEXTURED,
              (primary ? AllianceDetailStyle.BUTTON_PRIMARY_SPRITES : AllianceDetailStyle.BUTTON_SECONDARY_SPRITES).get(enabled, hovered),
              bounds.x(),
              bounds.y(),
              bounds.width(),
              bounds.height()
      );

      Component label = labelSupplier.get();
      AllianceButtonUiSupport.drawCenteredLabel(context, bounds, label, textColor);
   }

   @Override
   public void handleInput(UiInputState input) {
      boolean enabled = enabledSupplier.getAsBoolean();
      hovered = AllianceButtonUiSupport.resolveHovered(input, bounds, enabled);
      if (!enabled) {
         pressed = false;
         return;
      }
      pressed = AllianceButtonUiSupport.resolvePressed(input, pressed, hovered, onClick);
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(resolvedWidth(layoutContext), height);
   }

   private int resolvedWidth(UiLayoutContext layoutContext) {
      if (fixedWidth != null) {
         return fixedWidth;
      }

      Component label = labelSupplier.get();
      int textWidth = layoutContext.font().width(label);
      return Math.max(textWidth + AllianceDetailStyle.BUTTON_PADDING_X * 2, AllianceDetailStyle.BUTTON_MIN_WIDTH);
   }
}
