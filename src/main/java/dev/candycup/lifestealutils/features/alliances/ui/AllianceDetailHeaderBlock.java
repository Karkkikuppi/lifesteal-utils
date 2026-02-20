package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * renders the alliance detail header.
 */
final class AllianceDetailHeaderBlock implements Drawable {
   private final Component title;
   private final Supplier<Component> nameSupplier;
   private final AllianceDetailButton refreshButton;
   private final AllianceDetailButton backButton;

   private UiBounds bounds = UiBounds.empty();
   private UiBounds titleBounds = UiBounds.empty();
   private UiBounds nameBounds = UiBounds.empty();

   AllianceDetailHeaderBlock(
           Component title,
           Supplier<Component> nameSupplier,
           Component refreshLabel,
           Runnable onRefresh,
           Component backLabel,
           Runnable onBack,
           BooleanSupplier enabledSupplier
   ) {
      this.title = title;
      this.nameSupplier = nameSupplier;
      this.refreshButton = AllianceDetailButton.secondary(
              () -> refreshLabel,
              onRefresh,
              enabledSupplier
      );
      this.backButton = AllianceDetailButton.secondary(
              () -> backLabel,
              onBack,
              enabledSupplier
      );
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      Font font = layoutContext.font();

      Component name = nameSupplier.get();
      int titleWidth = font.width(title);
      int nameWidth = name == null ? 0 : font.width(name);
      int textHeight = font.lineHeight;
      int titleX = bounds.x() + AllianceDetailStyle.HEADER_PADDING_X;
      int titleY = bounds.y() + (bounds.height() - textHeight) / 2;
      this.titleBounds = new UiBounds(titleX, titleY, titleWidth, textHeight);
      this.nameBounds = new UiBounds(titleX + titleWidth + AllianceDetailStyle.TAB_GAP, titleY, nameWidth, textHeight);

      UiSize refreshSize = refreshButton.preferredSize(layoutContext);
      UiSize backSize = backButton.preferredSize(layoutContext);
      int rightX = bounds.x() + bounds.width() - AllianceDetailStyle.HEADER_PADDING_X;
      int buttonY = bounds.y() + (bounds.height() - refreshSize.height()) / 2;

      int backX = rightX - backSize.width();
      backButton.layout(layoutContext.withBounds(new UiBounds(backX, buttonY, backSize.width(), backSize.height())));

      int refreshX = backX - AllianceDetailStyle.TAB_GAP - refreshSize.width();
      refreshButton.layout(layoutContext.withBounds(new UiBounds(refreshX, buttonY, refreshSize.width(), refreshSize.height())));
   }

   @Override
   public void render(UiContext context) {
      AlliancesListStyle.renderHeaderBackgroundSprite(context.graphics(), bounds);
      context.graphics().drawString(context.minecraft().font, title, titleBounds.x(), titleBounds.y(), AllianceDetailStyle.TEXT_PRIMARY, true);

      Component name = nameSupplier.get();
      if (name != null && !name.getString().isEmpty()) {
         context.graphics().drawString(context.minecraft().font, name, nameBounds.x(), nameBounds.y(), AllianceDetailStyle.TEXT_MUTED, true);
      }

      refreshButton.render(context);
      backButton.render(context);
   }

   @Override
   public void handleInput(UiInputState input) {
      refreshButton.handleInput(input);
      backButton.handleInput(input);
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(layoutContext.availableBounds().width(), AllianceDetailStyle.HEADER_HEIGHT);
   }
}
