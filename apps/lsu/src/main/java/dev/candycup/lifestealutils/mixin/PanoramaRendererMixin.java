package dev.candycup.lifestealutils.mixin;

import dev.candycup.lifestealutils.features.titlescreen.CustomPanorama;
import net.minecraft.client.renderer.CubeMap;
//? if >=26.1 {
/*import net.minecraft.client.gui.render.GuiRenderer;
 *///?} else {
import net.minecraft.client.renderer.PanoramaRenderer;
//?}
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
/*@Mixin(GuiRenderer.class)
 *///?} else {
@Mixin(PanoramaRenderer.class)
//?}
public class PanoramaRendererMixin {

    @Shadow
    @Final
    @Mutable
    private CubeMap cubeMap;

    @Inject(
            //? if >=26.1 {
            /*method = "<init>",
            *///?} else {
            method = "<init>",
            //?}
            at = @At("TAIL"))
            //? if >=26.1 {
    /*private void replaceCubeMap(net.minecraft.client.renderer.state.gui.GuiRenderState renderState, net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureRenderDispatcher, java.util.List<net.minecraft.client.gui.render.pip.PictureInPictureRenderer<?>> pictureInPictureRenderers, CallbackInfo ci) {
     *///?} else {
    private void replaceCubeMap(CubeMap original, CallbackInfo ci) {
        //?}
        if (CustomPanorama.isCustomPanoramaEnabled()) {
            this.cubeMap = new CubeMap(Identifier.fromNamespaceAndPath("lifestealutils", "textures/gui/title/background/panorama"));
        }
    }
}
