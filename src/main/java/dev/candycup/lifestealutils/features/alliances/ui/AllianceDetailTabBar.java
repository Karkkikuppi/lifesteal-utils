package dev.candycup.lifestealutils.features.alliances.ui;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;

import java.util.List;

/**
 * renders the info and members tabs.
 */
final class AllianceDetailTabBar implements Drawable {
   private final List<AllianceDetailTabButton> tabs;

   private UiBounds bounds = UiBounds.empty();

   AllianceDetailTabBar(AllianceDetailTabButton infoTab, AllianceDetailTabButton membersTab) {
      this.tabs = List.of(infoTab, membersTab);
   }

   @Override
   public void layout(UiLayoutContext layoutContext) {
      this.bounds = layoutContext.availableBounds();
      int cursorX = bounds.x();
      int y = bounds.y();

      for (AllianceDetailTabButton tab : tabs) {
         UiSize tabSize = tab.preferredSize(layoutContext);
         UiBounds tabBounds = new UiBounds(cursorX, y, tabSize.width(), AllianceDetailStyle.TAB_HEIGHT);
         tab.layout(layoutContext.withBounds(tabBounds));
         cursorX += tabSize.width() + AllianceDetailStyle.TAB_GAP;
      }
   }

   @Override
   public void render(UiContext context) {
      for (AllianceDetailTabButton tab : tabs) {
         tab.render(context);
      }
   }

   @Override
   public void handleInput(UiInputState input) {
      for (AllianceDetailTabButton tab : tabs) {
         tab.handleInput(input);
      }
   }

   @Override
   public UiBounds bounds() {
      return bounds;
   }

   @Override
   public UiSize preferredSize(UiLayoutContext layoutContext) {
      return new UiSize(layoutContext.availableBounds().width(), AllianceDetailStyle.TAB_HEIGHT);
   }
}
