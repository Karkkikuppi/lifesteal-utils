package dev.candycup.lifestealutils.config.controllers;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import net.minecraft.network.chat.Component;

public record MinimessageController(Option<String> option) implements IStringController<String> {

   @Override
   public String getString() {
      return option().pendingValue();
   }

   @Override
   public void setFromString(String value) {
      option().requestSet(value);
   }

   @Override
   public Component formatValue() {
      String value = getString();
      if (value == null || value.isEmpty()) {
         return Component.literal("...");
      }
      return Component.literal(value);
   }

   @Override
   public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
      return new MinimessageControllerElement(this, screen, widgetDimension);
   }
}
