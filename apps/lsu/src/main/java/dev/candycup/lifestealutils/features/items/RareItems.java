package dev.candycup.lifestealutils.features.items;

import com.google.common.base.Strings;
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

    public static Float holdKeyProgress = 0f;
    public static ItemStack hoveredStack = ItemStack.EMPTY;
    public static ItemStack trackingStack = ItemStack.EMPTY;
    public static boolean deferTick = false;

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

    public void onItemRender(ItemRenderEvent event) {
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

    public static void tick() {
        // Called on client tick; defer processing to render thread during tooltip render
        deferTick = true;
    }

    public static void deferredTick() {
        deferTick = false;
        if (!RenderSystem.isOnRenderThread()) return;

        if (hoveredStack.isEmpty() || trackingStack.isEmpty()) {
            trackingStack = ItemStack.EMPTY;
            holdKeyProgress = 0f;
            return;
        }

        boolean down = InputConstants.isKeyDown(
                //? if >1.21.8 {
                Minecraft.getInstance().getWindow(),
                //?} else {
                /*Minecraft.getInstance().getWindow().handle(),
                 *///?}
                KeyBindingHelper.getBoundKeyOf(Minecraft.getInstance().options.keyDrop).getValue()
        );
        float delta = 0.08f;
        if (down) {
            holdKeyProgress = Math.min(1.0f, holdKeyProgress + delta);
        } else {
            holdKeyProgress = Math.max(0.0f, holdKeyProgress - delta);
        }

        hoveredStack = ItemStack.EMPTY;
    }

    public void appendTooltip(List<Component> toolTip, ItemStack stack) {
        if (!dropConfirmEnabled) return;
        if (!isRare(stack)) return;
        updateHovered(stack);
        if (deferTick)
            deferredTick();

        Component component = makeProgressBar(holdKeyProgress);
        toolTip.add(toolTip.size() < 2 ? toolTip.size() : 1, component);
    }

    private static void updateHovered(ItemStack stack) {
        ItemStack prev = trackingStack;
        hoveredStack = ItemStack.EMPTY;

        if (stack.isEmpty()) return;

        if (!prev.equals(stack)) {
            holdKeyProgress = 0f;
        }

        hoveredStack = stack;
        trackingStack = stack;
    }

    private Component makeProgressBar(Float progress) {
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
            progressBar.append(ChatFormatting.GRAY).append(Strings.repeat(barChar, current));

            if (progress < 1f) {
                progressBar.append(ChatFormatting.DARK_GRAY).append(Strings.repeat(barChar, total - current));
            }

            return Component.literal(progressBar.toString());
        }

        return holdMessage;
    }
}
