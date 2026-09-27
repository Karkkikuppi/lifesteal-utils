package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.features.qol.AutofocusDialogField;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
//? if >1.21.8 {
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class DialogAutofocusMixin {
    @Unique
    private AbstractWidget lifestealutils$autofocusedField;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lifestealutils$clickFirstDialogField(CallbackInfo ci) {
        if (!AutofocusDialogField.isEnabled() || !((Object) this instanceof DialogScreen<?> dialogScreen)) {
            return;
        }

        AbstractWidget field = AutofocusDialogField.firstField(dialogScreen);
        if (field != null && field != lifestealutils$autofocusedField) {
            double x = field.getX() + field.getWidth() / 2.0;
            double y = field.getY() + field.getHeight() / 2.0;
            lifestealutils$autofocusedField = field;
            //? if >1.21.8 {
            MouseButtonEvent click = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
            if (dialogScreen.mouseClicked(click, false)) {
                dialogScreen.mouseReleased(click);
            }
            //?} else {
            /*if (dialogScreen.mouseClicked(x, y, 0)) {
                dialogScreen.mouseReleased(x, y, 0);
            }
            *///?}
        }
    }
}
