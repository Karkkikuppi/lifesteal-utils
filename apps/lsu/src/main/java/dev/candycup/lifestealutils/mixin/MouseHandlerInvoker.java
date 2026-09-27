package dev.candycup.lifestealutils.mixin;

import net.minecraft.client.MouseHandler;
//? if >1.21.8 {
import net.minecraft.client.input.MouseButtonInfo;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    // MouseButtonInfo replaced the separate button and modifier arguments in 1.21.9.
    //? if >1.21.8 {
    @Invoker("onButton")
    void lifestealutils$onButton(long window, MouseButtonInfo button, int action);
    //?} else {
    /*@Invoker("onPress")
    void lifestealutils$onButton(long window, int button, int action, int modifiers);
    *///?}
}
