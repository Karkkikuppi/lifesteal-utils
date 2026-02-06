package dev.candycup.lifestealutils.event.events;

import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * fired when the title screen is initialized.
 * features can use this to add custom buttons or modify the screen.
 */
public class TitleScreenInitEvent extends LSUEvent {
   private final TitleScreen titleScreen;

   public TitleScreenInitEvent(TitleScreen titleScreen) {
      this.titleScreen = titleScreen;
   }

   public TitleScreen getTitleScreen() {
      return titleScreen;
   }
}
