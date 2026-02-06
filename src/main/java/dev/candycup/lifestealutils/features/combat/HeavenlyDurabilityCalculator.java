package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.api.CustomEnchantUtilities;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * calculates the expected helmet durability after triggering heavenly.
 */
public final class HeavenlyDurabilityCalculator {
   public static final String CONFIG_ID = "heavenly_durability";
   public static final String DEFAULT_FORMAT = "<gold><bold>Heavenly dura:</bold></gold><white> {{durability}}</white>";

   private static final String HEAVENLY_ENCHANT_KEY = "enchants:heavenly";
   private static final String NO_HEAVENLY_TEXT = "No heavenly";
   private static final float HEAVENLY_DURABILITY_FRACTION = 0.30f;
   private static final float DEFAULT_TEXT_X = 0.5F;
   private static final float DEFAULT_TEXT_Y = 0.285F;

   private final HudElementDefinition hudDefinition;

   /**
    * creates the hud definition for the calculator.
    */
   public HeavenlyDurabilityCalculator() {
      Config.ensureHeavenlyDurabilityFormat(DEFAULT_FORMAT);

      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_calculator"),
              "Heavenly Durability Calculator",
              this::getDisplayText,
              HudPosition.clamp(DEFAULT_TEXT_X, DEFAULT_TEXT_Y)
      );
   }

   /**
    * @return the hud definition for this calculator
    */
   public HudElementDefinition getHudDefinition() {
      return hudDefinition;
   }

   private String getDisplayText() {
      if (!Config.isHeavenlyDurabilityCalculatorEnabled()) {
         return "";
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return "";
      }

      ItemStack helmet = client.player.getItemBySlot(EquipmentSlot.HEAD);
      if (helmet.isEmpty() || !CustomEnchantUtilities.hasCustomEnchant(helmet, HEAVENLY_ENCHANT_KEY)) {
         return formatDisplayValue("<red>" + NO_HEAVENLY_TEXT + "</red>");
      }

      int maxDurability = helmet.getMaxDamage();
      int currentDamage = helmet.getDamageValue();
      int currentDurability = maxDurability - currentDamage;
      int heavenlyLoss = Math.round(maxDurability * HEAVENLY_DURABILITY_FRACTION);
      int durabilityAfterHeavenly = currentDurability - heavenlyLoss;

      String durabilityText = String.valueOf(durabilityAfterHeavenly);
      if (durabilityAfterHeavenly <= 0) {
         durabilityText = "<red>" + durabilityText + "</red>";
      }

      return formatDisplayValue(durabilityText);
   }

   private String formatDisplayValue(String durabilityValue) {
      String format = Config.getHeavenlyDurabilityFormat(DEFAULT_FORMAT);
      if (format == null || format.isBlank()) {
         format = DEFAULT_FORMAT;
      }

      if (format.contains("{{durability}}")) {
         return format.replace("{{durability}}", durabilityValue);
      }
      return format + " " + durabilityValue;
   }
}
