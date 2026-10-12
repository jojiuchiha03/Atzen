package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic camera for a fight: it circles slowly around the angler and the float, pulls in as the fish
 * comes closer and shakes when the line is about to snap or the fish surges. Blends in and out smoothly.
 */
public final class BattleCamera {
    static final int INTRO = 20;
    static final int OUTRO = 30;

    private BattleCamera() { }

    /** Returns {x, y, z, yRot, xRot} for the camera, or null to keep vanilla's view. */
    public static double[] orbit(Entity entity, float partial, Vec3 vanilla, float vanillaYaw, float vanillaPitch) {
        Minecraft client = Minecraft.getInstance();
        var state = AtzenClient.state;
        if (state == null || entity != client.player || client.level == null) return null;
        var player = client.player;
        float time = AtzenClient.cameraTicks + partial;

        Vec3 eye = player.getEyePosition(partial);
        Vec3 focus = eye.subtract(0, 0.4, 0);
        double distance = 0;
        if (player.fishing != null) {
            Vec3 hook = player.fishing.getPosition(partial);
            distance = eye.distanceTo(hook);
            focus = focus.lerp(hook, 0.4);
        }
        double closeness = AtzenClient.displayedProgress / 100.0;
        double radius = Mth.clamp(4.0 + distance * 0.45, 4.0, 10.0) * (1 - closeness * 0.25);
        double angle = Math.toRadians(AtzenClient.orbitStartYaw + time * 0.8);
        double height = 1.7 + Math.sin(time * 0.045) * 0.35;
        Vec3 wanted = focus.add(-Math.sin(angle) * radius, height, Math.cos(angle) * radius);

        // Never through a wall: stop short of the first block between the focus point and the camera.
        var hit = client.level.clip(new ClipContext(focus, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            wanted = hit.getLocation().add(focus.subtract(hit.getLocation()).normalize().scale(0.35));
        }

        Vec3 look = focus.subtract(wanted);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));

        float shake = Math.max(0, AtzenClient.displayedTension - 70) / 30f * 1.6f;
        if (state.result() == FishingBattle.Result.ACTIVE && state.phase() == FishingBattle.Phase.SURGE) shake += 0.7f;
        yaw += shake * (float) Math.sin(time * 2.7);
        pitch += shake * (float) Math.cos(time * 3.3) * 0.7f;

        float blend;
        if (state.result() != FishingBattle.Result.ACTIVE) blend = Mth.clamp((AtzenClient.outroTicks - partial) / OUTRO, 0, 1);
        else blend = Mth.clamp(time / INTRO, 0, 1);
        blend = blend * blend * (3 - 2 * blend);

        Vec3 position = vanilla.lerp(wanted, blend);
        return new double[] {position.x, position.y, position.z,
                vanillaYaw + Mth.wrapDegrees(yaw - vanillaYaw) * blend, Mth.lerp(blend, vanillaPitch, pitch)};
    }
}
