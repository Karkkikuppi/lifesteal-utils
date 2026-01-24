package dev.candycup.lifestealutils.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.candycup.lifestealutils.event.LSUEvent;
import net.minecraft.world.item.ItemStack;

/**
 * fired when an item entity is about to be rendered.
 * <p>
 * performance note: this event fires extremely frequently (60-144+ times per second per item).
 * listeners should be highly optimized and avoid allocations in this hot path.
 * <p>
 * can be cancelled to prevent rendering.
 * features can modify the poseStack to apply transforms (e.g., scaling).
 */
public class ItemRenderEvent extends LSUEvent {
    private final ItemStack itemStack;
    private final PoseStack poseStack;
    private final boolean isRare;

    /**
     * Creates a new ItemRenderEvent representing an item entity about to be rendered.
     *
     * @param itemStack the item stack being rendered
     * @param poseStack the pose stack for applying rendering transforms; listeners may modify it
     * @param isRare    true if the item should be rendered with rare-item visuals, false otherwise
     */
    public ItemRenderEvent(ItemStack itemStack, PoseStack poseStack, boolean isRare) {
        this.itemStack = itemStack;
        this.poseStack = poseStack;
        this.isRare = isRare;
    }

    /**
     * Indicates that this event supports cancellation.
     *
     * @return `true` if the event can be cancelled, `false` otherwise.
     */
    @Override
    public boolean isCancellable() {
        return true;
    }

    /**
     * Gets the item stack that is being rendered by this event.
     *
     * @return the ItemStack for the item about to be rendered
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Gets the PoseStack used for applying transforms during item rendering.
     *
     * @return the PoseStack for applying rendering transforms; modifying it affects how the item is rendered
     */
    public PoseStack getPoseStack() {
        return poseStack;
    }

    /**
     * Indicates whether the item being rendered is marked as rare.
     *
     * @return `true` if the item is rare, `false` otherwise.
     */
    public boolean isRare() {
        return isRare;
    }
}