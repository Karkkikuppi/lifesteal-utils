package dev.candycup.lifestealutils.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.candycup.lifestealutils.Config;
import dev.candycup.lifestealutils.LifestealServerDetector;
import dev.candycup.lifestealutils.features.alliances.Alliances;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
   @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
   public Component appendWithAllianceColor(Component original) {
      if (!Config.getEnableAlliances()) return original;
      if (!LifestealServerDetector.isOnLifestealServer()) return original;
      if (original == null) return null;

      Component nameComponent = ((Player) (Object) this).getName();
      String plainName = nameComponent != null ? nameComponent.getString() : null;
      if (plainName == null || plainName.isBlank()) return original;
      if (!Alliances.isAlliedName(plainName)) return original;

      return Alliances.colorizeNameTag(original);
   }

}
