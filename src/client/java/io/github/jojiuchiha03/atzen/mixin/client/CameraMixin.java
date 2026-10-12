package io.github.jojiuchiha03.atzen.mixin.client;

import io.github.jojiuchiha03.atzen.client.BattleCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the camera to {@link BattleCamera} while a fishing fight is on. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Entity entity;
    @Shadow private boolean detached;
    @Shadow public abstract Vec3 position();
    @Shadow public abstract float xRot();
    @Shadow public abstract float yRot();
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void atzen$battleOrbit(float partialTicks, CallbackInfo ci) {
        double[] view = BattleCamera.orbit(entity, partialTicks, position(), yRot(), xRot());
        if (view == null) return;
        detached = true;
        setRotation((float) view[3], (float) view[4]);
        setPosition(view[0], view[1], view[2]);
    }
}
