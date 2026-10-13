package io.github.jojiuchiha03.atzen.mixin.client;

import io.github.jojiuchiha03.atzen.client.CameraController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The crosshair belongs to the player's own view, not to the cinematic fishing shots. */
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void atzen$hideCrosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (CameraController.active()) ci.cancel();
    }
}
