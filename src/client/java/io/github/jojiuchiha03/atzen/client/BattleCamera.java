package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic camera for a fight, over the shoulder: it stands behind and beside the angler, looks past them at
 * the fish and swings slowly from one shoulder to the other. During a run it moves in towards the fish, when the
 * line is about to snap it shakes. Everything it follows is smoothed per frame, so the float's coarse network
 * updates do not make it jitter. It blends in and out of the player's own view.
 */
public final class BattleCamera {
    static final int INTRO = 24;
    static final int OUTRO = 30;

    private static Vec3 smoothFocus;
    private static Vec3 smoothPosition;
    private static float smoothPull;
    private static long lastFrame;

    private BattleCamera() { }

    static void reset() {
        smoothFocus = null;
        smoothPosition = null;
        smoothPull = 0;
    }

    /** One step of exponential smoothing; {@code alpha} comes from the frame time, so any frame rate looks the same. */
    private static Vec3 follow(Vec3 current, Vec3 target, double alpha) {
        return current == null ? target : current.lerp(target, alpha);
    }

    /** Returns {x, y, z, yRot, xRot} for the camera, or null to keep vanilla's view. */
    public static double[] orbit(Entity entity, float partial, Vec3 vanilla, float vanillaYaw, float vanillaPitch) {
        Minecraft client = Minecraft.getInstance();
        var state = AtzenClient.state;
        if (state == null || entity != client.player || client.level == null) {
            reset();
            return null;
        }
        var player = client.player;
        long now = System.nanoTime();
        double seconds = lastFrame == 0 ? 0.016 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        float time = AtzenClient.cameraTicks + partial;

        Vec3 eye = player.getEyePosition(partial);
        Vec3 hook = player.fishing != null ? player.fishing.getPosition(partial) : eye.add(player.getLookAngle().scale(8));
        boolean active = state.result() == FishingBattle.Result.ACTIVE;
        boolean surge = active && state.phase() == FishingBattle.Phase.SURGE;

        // During a run the camera leans towards the fish; it eases there and back.
        float pull = surge ? 1 : 0;
        smoothPull += (pull - smoothPull) * (float) (1 - Math.exp(-seconds * 2.5));

        // Look past the angler at the fish: the focus sits between them, closer to the fish when it runs.
        Vec3 focus = eye.lerp(hook, 0.55 + 0.2 * smoothPull).add(0, -0.3, 0);
        smoothFocus = follow(smoothFocus, focus, 1 - Math.exp(-seconds * 6));

        // Behind the angler, seen from the fish, swinging slowly from shoulder to shoulder.
        Vec3 away = eye.subtract(hook).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) away = player.getLookAngle().scale(-1).multiply(1, 0, 1);
        away = away.normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x);
        double sway = Math.sin(time * 0.025) * 0.9;
        double back = 3.2 - 0.8 * smoothPull - AtzenClient.displayedProgress / 100.0 * 0.6;
        Vec3 wanted = eye.add(away.scale(back)).add(side.scale(1.4 * sway + 0.6)).add(0, 0.9 + 0.2 * Math.sin(time * 0.04), 0);

        // Never through a wall: stop short of the first block between the angler and the camera.
        var hit = client.level.clip(new ClipContext(eye, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            wanted = hit.getLocation().add(eye.subtract(hit.getLocation()).normalize().scale(0.3));
        }
        smoothPosition = follow(smoothPosition, wanted, 1 - Math.exp(-seconds * 4));

        Vec3 look = smoothFocus.subtract(smoothPosition);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));

        float shake = Math.max(0, AtzenClient.displayedTension - 75) / 25f * 1.1f + (surge ? 0.4f : 0);
        yaw += shake * (float) Math.sin(time * 2.7);
        pitch += shake * (float) Math.cos(time * 3.3) * 0.6f;

        float blend = active ? Mth.clamp(time / INTRO, 0, 1) : Mth.clamp((AtzenClient.outroTicks - partial) / OUTRO, 0, 1);
        blend = blend * blend * (3 - 2 * blend);

        Vec3 position = vanilla.lerp(smoothPosition, blend);
        return new double[] {position.x, position.y, position.z,
                vanillaYaw + Mth.wrapDegrees(yaw - vanillaYaw) * blend, Mth.lerp(blend, vanillaPitch, pitch)};
    }
}
