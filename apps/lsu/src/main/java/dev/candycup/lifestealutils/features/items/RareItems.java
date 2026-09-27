package dev.candycup.lifestealutils.features.items;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.candycup.configura.serial.SerialEntry;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.ConfigurableFloat;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents;
import dev.candycup.lifestealutils.event.LifestealUtilsEvents.ItemRenderEvent;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * highlights rare items (netherite, custom enchants, artifacts) with increased scale.
 * <p>
 * performance: this feature is called on every item render. the isRare check
 * is done in the mixin to avoid overhead in the event system hot path.
 */
public final class RareItems {
    private static final float HOLD_PROGRESS_PER_TICK = 0.08f;

    @Getter
    @Setter
    @SerialEntry(comment = "Hold drop key to confirm dropping")
    @ConfigurableBoolean(location = "qol.rareitems.drophold")
    public static boolean dropConfirmEnabled = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Enable increased scale for rare items such as neth and custom enchants.")
    @ConfigurableBoolean(location = "qol.rareitems.rareitemscaleenabled")
    private static boolean rareItemScaleEnabled = true;

    @Getter
    @Setter
    @SerialEntry(comment = "Increased scale of the rare items.")
    @ConfigurableFloat(location = "qol.rareitems.rareitemscale", min = 1.0f, max = 5.0f)
    private static float rareItemScale = 2.0f;

    private static float inventoryHoldProgress = 0f;
    private static float worldHoldProgress = 0f;
    private static ItemStack trackingStack = ItemStack.EMPTY;
    private static ItemStack worldTrackingStack = ItemStack.EMPTY;
    private static boolean deferTooltipTick = false;
    private static boolean worldProgressBarVisible = false;

    public RareItems() {
        LifestealUtilsEvents.ITEM_RENDER.register(event -> {
            if (!rareItemScaleEnabled) {
                return;
            }
            onItemRender(event);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> appendTooltip(lines, stack));
    }

    private void onItemRender(ItemRenderEvent event) {
        // only scale if the item is marked as rare by the mixin
        if (!event.isRare()) return;

        float scale = rareItemScale;
        event.getPoseStack().scale(scale, scale, scale);
    }

    public static boolean isRare(ItemStack itemStack) {
        Item item = itemStack.getItem();

        if (item == Items.NETHERITE_HELMET ||
                item == Items.NETHERITE_CHESTPLATE ||
                item == Items.NETHERITE_LEGGINGS ||
                item == Items.NETHERITE_BOOTS ||
                item == Items.NETHERITE_SWORD ||
                item == Items.NETHERITE_AXE ||
                item == Items.NETHERITE_PICKAXE ||
                item == Items.NETHERITE_SHOVEL ||
                item == Items.NETHERITE_HOE ||
                item == Items.ANCIENT_DEBRIS ||
                item == Items.NETHERITE_SCRAP ||
                item == Items.NETHERITE_BLOCK ||
                item == Items.NETHERITE_INGOT) {
            return true;
        }

        Tag tag = encodeStack(
                itemStack,
                Minecraft.getInstance().player.registryAccess().createSerializationContext(NbtOps.INSTANCE)
        );

        if (!(tag instanceof CompoundTag nbt)) {
            return false;
        }

        Optional<CompoundTag> customOpt = nbt.getCompound("minecraft:custom_data");

        if (customOpt.isPresent()) {
            CompoundTag custom = customOpt.get();

            Optional<CompoundTag> pbvOpt = custom.getCompound("PublicBukkitValues");

            if (pbvOpt.isPresent()) {
                CompoundTag pbv = pbvOpt.get();

                if (pbv.contains("lifesteal:artifact")) {
                    return true;
                }

                for (String key : pbv.keySet()) {
                    if (key.startsWith("enchants:")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static CompoundTag encodeStack(ItemStack stack, DynamicOps<Tag> ops) {
        DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ops, stack.getComponentsPatch());
        result.ifError((e) -> {
        });
        Tag nbtElement = result.getOrThrow();
        return (CompoundTag) nbtElement;
    }

    private static void tick() {
        // Called on client tick; defer processing to render thread during tooltip render
        deferTooltipTick = true;
        if (Minecraft.getInstance().screen == null) {
            resetInventoryDropConfirmation();
        }
        tickWorldDropConfirmation();
    }

    private static void tickWorldDropConfirmation() {
        Minecraft client = Minecraft.getInstance();
        if (!dropConfirmEnabled || client.player == null || client.screen != null) {
            resetWorldDropConfirmation();
            return;
        }

        ItemStack selectedStack = client.player.getMainHandItem();
        if (selectedStack.isEmpty() || !isRare(selectedStack)) {
            resetWorldDropConfirmation();
            return;
        }

        if (!ItemStack.isSameItemSameComponents(worldTrackingStack, selectedStack)) {
            worldHoldProgress = 0f;
            worldTrackingStack = selectedStack.copy();
        }

        if (!isDropKeyDown()) {
            worldHoldProgress = 0f;
            clearWorldProgressBar(client);
            return;
        }

        worldHoldProgress = advanceProgress(worldHoldProgress);
        worldProgressBarVisible = true;
        //? if >=26.1 {
        /*client.player.sendOverlayMessage(makeProgressBar(worldHoldProgress));
         *///?} else {
        client.player.displayClientMessage(makeProgressBar(worldHoldProgress), true);
        //?}
    }

    private static void resetWorldDropConfirmation() {
        worldTrackingStack = ItemStack.EMPTY;
        worldHoldProgress = 0f;
        clearWorldProgressBar(Minecraft.getInstance());
    }

    private static void clearWorldProgressBar(Minecraft client) {
        if (!worldProgressBarVisible) return;
        worldProgressBarVisible = false;
        if (client.player == null) return;
        //? if >=26.1 {
        /*client.player.sendOverlayMessage(Component.empty());
         *///?} else {
        client.player.displayClientMessage(Component.empty(), true);
        //?}
    }

    private static void resetInventoryDropConfirmation() {
        trackingStack = ItemStack.EMPTY;
        inventoryHoldProgress = 0f;
    }

    public static boolean shouldBlockWorldDrop(ItemStack stack) {
        return shouldBlockDrop(stack, worldHoldProgress);
    }

    public static boolean shouldBlockInventoryDrop(ItemStack stack) {
        return shouldBlockDrop(stack, inventoryHoldProgress);
    }

    private static boolean shouldBlockDrop(ItemStack stack, float progress) {
        return dropConfirmEnabled && progress < 1f && isRare(stack);
    }

    private static float advanceProgress(float progress) {
        return Math.min(1f, progress + HOLD_PROGRESS_PER_TICK);
    }

    private static boolean isDropKeyDown() {
        Minecraft client = Minecraft.getInstance();
        return InputConstants.isKeyDown(
                //? if >1.21.8 {
                client.getWindow(),
                //?} else {
                /*client.getWindow().handle(),
                 *///?}
                KeyBindingHelper.getBoundKeyOf(client.options.keyDrop).getValue()
        );
    }

    private static void deferredTick() {
        deferTooltipTick = false;
        if (!RenderSystem.isOnRenderThread()) return;

        if (isDropKeyDown()) {
            inventoryHoldProgress = advanceProgress(inventoryHoldProgress);
        } else {
            inventoryHoldProgress = Math.max(0f, inventoryHoldProgress - HOLD_PROGRESS_PER_TICK);
        }

    }

    private void appendTooltip(List<Component> toolTip, ItemStack stack) {
        if (!dropConfirmEnabled) return;
        if (!isRare(stack)) {
            resetInventoryDropConfirmation();
            return;
        }
        updateHovered(stack);
        if (deferTooltipTick)
            deferredTick();

        Component component = makeProgressBar(inventoryHoldProgress);
        toolTip.add(toolTip.size() < 2 ? toolTip.size() : 1, component);
    }

    private static void updateHovered(ItemStack stack) {
        ItemStack prev = trackingStack;

        if (stack.isEmpty()) return;

        if (!ItemStack.isSameItemSameComponents(prev, stack)) {
            inventoryHoldProgress = 0f;
        }

        trackingStack = stack.copy();
    }

    private static Component makeProgressBar(float progress) {
        Component key = Minecraft.getInstance().options.keyDrop.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY);
        MutableComponent holdMessage = Component.translatable("lsu.rareitem.drop.confirm", key).withStyle(ChatFormatting.DARK_GRAY);
        Font fontRenderer = Minecraft.getInstance().font;
        String barChar = "|";
        float barWidth = fontRenderer.width(barChar);
        float textWidth = fontRenderer.width(holdMessage);

        if (barWidth <= 0f) return holdMessage;

        int total = Math.max(1, (int) (textWidth / barWidth));
        int current = Math.min(total, Math.max(0, (int) (progress * total)));

        if (progress > 0f) {
            StringBuilder progressBar = new StringBuilder();
            progressBar.append(ChatFormatting.GRAY).append(barChar.repeat(current));

            if (progress < 1f) {
                progressBar.append(ChatFormatting.DARK_GRAY).append(barChar.repeat(total - current));
            }

            return Component.literal(progressBar.toString());
        }

        return holdMessage;
    }
}
