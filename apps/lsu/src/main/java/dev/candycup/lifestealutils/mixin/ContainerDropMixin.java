package dev.candycup.lifestealutils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.candycup.lifestealutils.features.items.RareItems;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerScreen.class)
public class ContainerDropMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    // KeyEvent replaced the separate key code and scan code arguments in 1.21.9.
    //? if >1.21.8 {
    @ModifyExpressionValue(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;matches(Lnet/minecraft/client/input/KeyEvent;)Z", ordinal = 2))
            //?} else {
    /*@ModifyExpressionValue(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;matches(II)Z", ordinal = 2))
     *///?}
    private boolean keyPressed(boolean original) {
        if (hoveredSlot == null) return original;

        if (RareItems.isRare(hoveredSlot.getItem()) && RareItems.dropConfirmEnabled) {
            if (RareItems.holdKeyProgress >= 1f) {
                return original;
            }
            return false;
        }
        return original;
    }
}
