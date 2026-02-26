package dev.candycup.lifestealutils.features.combat;

import dev.candycup.lifestealutils.api.LifestealAPI;
import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.candycup.lifestealutils.config.configurables.ConfigurableEnum;
import dev.candycup.lifestealutils.config.configurables.ConfigurableMinimessage;
import dev.candycup.lifestealutils.hud.HudAnchor;
import dev.candycup.lifestealutils.hud.HudElementDefinition;
import dev.candycup.lifestealutils.hud.HudPosition;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import lombok.Getter;
import lombok.Setter;
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

   @Getter
   @Setter
   @SerialEntry(comment = "Whether to enable the heavenly durability calculator HUD element")
   @ConfigurableBoolean(location = "timers.heavenly.enabled")
   public static boolean heavenlyDurabilityCalculatorEnabled = false;

   @Getter
   @Setter
   @SerialEntry(comment = "Custom format for the heavenly durability calculator display")
   @ConfigurableMinimessage(location = "timers.heavenly.format")
   private static String heavenlyDurabilityCalculatorFormat = "<gold><bold>Dura after heavenly:</bold></gold><white> {{durability}}</white>";

   @Getter
   @Setter
   @SerialEntry(comment = "Display heavenly durability as numeric value or as percentage")
   @ConfigurableEnum(location = "timers.heavenly.valuedisplay")
   private static HeavenlyValueDisplay heavenlyValueDisplay = HeavenlyValueDisplay.NUMERIC;

   private static final String HEAVENLY_ENCHANT_KEY = "enchants:heavenly";
   private static final String NO_HEAVENLY_TEXT = "No heavenly";
   private static final float HEAVENLY_DURABILITY_FRACTION = 0.30f;
   private static final float DEFAULT_TEXT_X = 0.5F;
   private static final float DEFAULT_TEXT_Y = 0.285F;

   @Getter
   private final HudElementDefinition hudDefinition;

   /**
    * creates the hud definition for the calculator.
    */
   public HeavenlyDurabilityCalculator() {
      this.hudDefinition = new HudElementDefinition(
              Identifier.fromNamespaceAndPath("lifestealutils", CONFIG_ID + "_calculator"),
              "Heavenly Durability Calculator",
              this::getDisplayText,
              HudPosition.clamp(DEFAULT_TEXT_X, DEFAULT_TEXT_Y, HudAnchor.CENTER)
      );
   }

   private String getDisplayText() {
      if (!heavenlyDurabilityCalculatorEnabled) {
         return "";
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return "";
      }

      ItemStack helmet = client.player.getItemBySlot(EquipmentSlot.HEAD);
      if (helmet.isEmpty() || !LifestealAPI.hasSpecificCustomEnchant(helmet, HEAVENLY_ENCHANT_KEY)) {
         return formatDisplayValue("<red>" + NO_HEAVENLY_TEXT + "</red>");
      }

      int maxDurability = helmet.getMaxDamage();
      if (maxDurability <= 0) {
         return formatDisplayValue("<red>0</red>");
      }

      int currentDamage = helmet.getDamageValue();
      int currentDurability = maxDurability - currentDamage;
      int heavenlyLoss = Math.round(maxDurability * HEAVENLY_DURABILITY_FRACTION);
      int durabilityAfterHeavenly = currentDurability - heavenlyLoss;

      String durabilityText;
      if (heavenlyValueDisplay == HeavenlyValueDisplay.PERCENTAGE) {
         int durabilityPercentage = Math.round((Math.max(durabilityAfterHeavenly, 0) * 100.0f) / maxDurability);
         durabilityText = durabilityPercentage + "%";
      } else {
         durabilityText = String.valueOf(durabilityAfterHeavenly);
      }

      if (durabilityAfterHeavenly <= 0) {
         durabilityText = "<red>" + durabilityText + "</red>";
      }

      return formatDisplayValue(durabilityText);
   }

   private String formatDisplayValue(String durabilityValue) {
      String format = heavenlyDurabilityCalculatorFormat;
      if (format == null || format.isBlank()) {
         format = DEFAULT_FORMAT;
      }

      if (format.contains("{{durability}}")) {
         return format.replace("{{durability}}", durabilityValue);
      }
      return format + " " + durabilityValue;
   }

   public enum HeavenlyValueDisplay {
      NUMERIC("lsu.config.timers.heavenly.valuedisplay.numeric"),
      PERCENTAGE("lsu.config.timers.heavenly.valuedisplay.percentage");

      @Getter
      private final String translationKey;

      HeavenlyValueDisplay(String translationKey) {
         this.translationKey = translationKey;
      }
   }
}
