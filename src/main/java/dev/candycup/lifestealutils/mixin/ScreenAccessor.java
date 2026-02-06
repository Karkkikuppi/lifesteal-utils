package dev.candycup.lifestealutils.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * accessor for protected methods in {@link Screen}.
 */
@Mixin(Screen.class)
public interface ScreenAccessor {

   /**
    * invokes the protected {@link Screen#addRenderableWidget(GuiEventListener)} method.
    */
   @Invoker("addRenderableWidget")
   <T extends GuiEventListener & Renderable & NarratableEntry> T invokeAddRenderableWidget(T widget);
}
