package io.github.jojiuchiha03.atzen.mixin.client;

import io.github.jojiuchiha03.atzen.client.CameraController;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No first-person hand floating in front of the cinematic fishing shots. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void atzen$hideHand(CameraRenderState camera, float partialTicks, Matrix4fc projection, CallbackInfo ci) {
        if (CameraController.active()) ci.cancel();
    }
}
