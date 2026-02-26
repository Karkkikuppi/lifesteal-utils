package dev.candycup.lifestealutils.ui.framework.components;

import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import dev.candycup.lifestealutils.ui.framework.core.UiSize;

import java.util.List;

/**
 * base interface for drawable ui components.
 */
public interface Drawable {
   void layout(UiLayoutContext layoutContext);

   void render(UiContext context);

   void handleInput(UiInputState input);

   UiBounds bounds();

   UiSize preferredSize(UiLayoutContext layoutContext);

   default List<Drawable> children() {
      return List.of();
   }
}
