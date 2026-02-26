package dev.candycup.lifestealutils.features.resources;

import dev.candycup.lifestealutils.config.configurables.ConfigurableBoolean;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import lombok.Getter;

public class ResourcePackOverrides {
   @SerialEntry(comment = "Whether to remove server-provided emoji model/font mappings")
   @ConfigurableBoolean(location = "customization.resourcepack.removeemojis")
   @Getter
   private static boolean removeEmojis = false;

   @SerialEntry(comment = "Whether to remove all server-provided shield item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removeallshieldoverrides")
   @Getter
   private static boolean removeAllShieldOverrides = false;

   @SerialEntry(comment = "Whether to remove all server-provided sword item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removeswordskins")
   @Getter
   private static boolean removeSwordSkins = false;

   @SerialEntry(comment = "Whether to remove all server-provided axe item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removeaxeskins")
   @Getter
   private static boolean removeAxeSkins = false;

   @SerialEntry(comment = "Whether to remove all server-provided pickaxe item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removepickaxeskins")
   @Getter
   private static boolean removePickaxeSkins = false;

   @SerialEntry(comment = "Whether to remove all server-provided shovel item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removeshovelskins")
   @Getter
   private static boolean removeShovelSkins = false;

   @SerialEntry(comment = "Whether to remove all server-provided mace item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removemaceskins")
   @Getter
   private static boolean removeMaceSkins = false;

   @SerialEntry(comment = "Whether to remove all server-provided bow item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removebowskins")
   @Getter
   private static boolean removeBowSkins = false;

   @SerialEntry(comment = "Whether to remove all server-provided crossbow item overrides")
   @ConfigurableBoolean(location = "customization.resourcepack.removecrossbowskins")
   @Getter
   private static boolean removeCrossbowSkins = false;
}
