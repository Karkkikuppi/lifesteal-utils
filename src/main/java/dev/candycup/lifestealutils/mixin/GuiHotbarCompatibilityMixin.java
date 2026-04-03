package dev.candycup.lifestealutils.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Gui.class)
public abstract class GuiHotbarCompatibilityMixin {
   @Unique
   private static final Logger LIFESTEALUTILS$LOGGER = LoggerFactory.getLogger("lifestealutils");
   @Unique
   private static boolean lifestealutils$loggedVariantsCitHotbarCrash;

   @WrapMethod(method = "renderSlot")
   private void lifestealutils$guardHotbarItemRender(
      GuiGraphics context,
      int x,
      int y,
      DeltaTracker tickCounter,
      Player player,
      ItemStack stack,
      int seed,
      Operation<Void> original
   ) {
      try {
         original.call(context, x, y, tickCounter, player, stack, seed);
      } catch (RuntimeException exception) {
         if (!lifestealutils$isVariantsCitCrash(exception)) {
            throw exception;
         }

         if (!lifestealutils$loggedVariantsCitHotbarCrash) {
            lifestealutils$loggedVariantsCitHotbarCrash = true;
            LIFESTEALUTILS$LOGGER.error(
               "[lsu-vcit] prevented a Variants CIT hotbar crash while rendering {}. This is usually caused by a broken CIT resource pack entry.",
               stack,
               exception
            );
         }
      }
   }

   @Unique
   private static boolean lifestealutils$isVariantsCitCrash(Throwable throwable) {
      Throwable current = throwable;
      while (current != null) {
         for (StackTraceElement element : current.getStackTrace()) {
            if (element.getClassName().startsWith("fr.estecka.variantscit.")) {
               return true;
            }
         }
         current = current.getCause();
      }
      return false;
   }
}
