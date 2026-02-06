package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.Config;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public class PanoramaRendererMixin {

   @Shadow
   @Final
   @Mutable
   private CubeMap cubeMap;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void replaceCubeMap(CubeMap original, CallbackInfo ci) {
      if (Config.getCustomPanoramaEnabled()) {
         this.cubeMap = new CubeMap(Identifier.fromNamespaceAndPath("lifestealutils", "textures/gui/title/background/panorama"));
      }
   }
}
