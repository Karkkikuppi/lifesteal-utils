package dev.candycup.lifestealutils.ui.framework.screens;

import dev.candycup.lifestealutils.ui.framework.components.Drawable;
import dev.candycup.lifestealutils.ui.framework.core.UiBounds;
import dev.candycup.lifestealutils.ui.framework.core.UiContext;
import dev.candycup.lifestealutils.ui.framework.core.UiInputState;
import dev.candycup.lifestealutils.ui.framework.core.UiLayoutContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * base screen that renders a tree of drawable ui components.
 */
public abstract class DrawableScreen extends Screen {
   private static final String NULL_UI_MESSAGE = "buildUi returned null";

   private Drawable root;
   private UiInputState previousInput;
   private double pendingScrollX;
   private double pendingScrollY;

   protected DrawableScreen(Component title) {
      super(title);
   }

   protected abstract Drawable buildUi();

   @Override
   protected void init() {
      this.root = Objects.requireNonNull(buildUi(), NULL_UI_MESSAGE);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      Minecraft minecraft = Minecraft.getInstance();
      UiInputState input = UiInputState.from(minecraft, previousInput, pendingScrollX, pendingScrollY);
      UiBounds bounds = new UiBounds(0, 0, width, height);
      UiLayoutContext layoutContext = new UiLayoutContext(minecraft, minecraft.font, bounds);
      UiContext context = new UiContext(minecraft, graphics, input, width, height, partialTick);

      if (root == null) {
         root = Objects.requireNonNull(buildUi(), NULL_UI_MESSAGE);
      }

      root.layout(layoutContext);
      root.handleInput(input);
      root.render(context);
      if (enableVanillaWidgets()) {
         renderVanillaWidgets(graphics, mouseX, mouseY, partialTick);
      }

      previousInput = input;
      pendingScrollX = 0;
      pendingScrollY = 0;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (enableVanillaWidgets()) {
         super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
      pendingScrollX += scrollX;
      pendingScrollY += scrollY;
      return true;
   }

   protected boolean enableVanillaWidgets() {
      return false;
   }

   protected void renderVanillaWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
   }
}
