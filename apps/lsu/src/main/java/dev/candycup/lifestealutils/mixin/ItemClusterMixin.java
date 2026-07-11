package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.ItemClusterRenderStateDuck;
import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.features.items.RareItems;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ItemClusterRenderState.class)
public class ItemClusterMixin implements ItemClusterRenderStateDuck {

    @Unique
    private boolean lifestealutils$isRare = false;
    @Unique
    private ItemStack lifestealutils$itemStack = ItemStack.EMPTY;

    @Inject(method = "extractItemGroupRenderState", at = @At("HEAD"))

    private void lifestealutils$captureRare(Entity entity, ItemStack stack, ItemModelResolver resolver, CallbackInfo ci) {
        if (!LifestealAPI.isOnLifestealNetwork()) return;
        lifestealutils$setItemStack(stack.copy());

        if (stack.isEmpty()) return;
        lifestealutils$setRare(RareItems.isRare(stack));
    }

    @Override
    public boolean lifestealutils$isRare() {
        return lifestealutils$isRare;
    }

    @Override
    public void lifestealutils$setRare(boolean rare) {
        this.lifestealutils$isRare = rare;
    }

    @Override
    public ItemStack lifestealutils$getItemStack() {
        return lifestealutils$itemStack;
    }

    @Override
    public void lifestealutils$setItemStack(ItemStack stack) {
        this.lifestealutils$itemStack = stack;
    }
}
