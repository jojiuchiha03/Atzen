package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.FishAction;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.BiteTarget;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The fishing camera: one state machine for the whole cast. Every state has its own shot; the camera glides
 * between them and blends in and out of the player's own view.
 * <ul>
 *   <li>CASTING: over the shoulder, following the flying float.</li>
 *   <li>WAITING: low beside the angler, on the float — only while the angler stands still; moving or looking
 *       around gives the view back at once.</li>
 *   <li>BITE: under water beside the float, watching the fish come in and take the bait.</li>
 *   <li>HOOKED / FIGHTING: over the shoulder while the fish holds; on the fish when it runs or dives; wide when it
 *       heads for cover; tight behind the angler when it jumps. Shakes when the line is close to breaking.</li>
 *   <li>LANDING: low at the water, on the fish being drawn in.</li>
 *   <li>TROPHY: a slow turn around the angler with the catch.</li>
 * </ul>
 * The camera key turns the cinematic camera off and on; it never makes a decision for the player.
 */
public final class CameraController {
    public enum State { IDLE, CASTING, WAITING, BITE, HOOKED, FIGHTING, LANDING, TROPHY }

    static final int TROPHY_TICKS = 80;
    private static State state = State.IDLE;
    private static int stateTicks;
    private static boolean enabled = true;
    private static float blend;
    private static float previousBlend;
    private static int restless;
    private static Vec3 lastPosition;
    private static float lastYaw, lastPitch;
    private static int trophyTicks;
    private static Vec3 smoothPosition, smoothFocus;
    /** Whether the current shot is seen from the angler's side (clipped from the eye) rather than from the water. */
    private static boolean fromEye = true;
    private static long lastFrame;
    private static AtzenFishEntity watched;
    /** The fight shot on screen; it only changes after it has been held a while, so the camera does not jump around. */
    private static FishAction shotAction = FishAction.CALM;
    private static int shotHeld;
    /** Smoothed direction from the angler to the fish, so a fish swimming past does not swing the camera around. */
    private static Vec3 heading;
    private static double frameSeconds = 0.016;
    /** The cast: where the angler stood and looked when the float left, so the shot does not wander with the mouse. */
    private static FishingHook castHook;
    private static Vec3 castEye, castDir;
    private static int landedAt = -1;

    private CameraController() { }

    static State state() { return state; }
    /** True while the cinematic camera is on screen, even partly; the first-person hand and crosshair are hidden. */
    public static boolean active() { return blend > 0 || previousBlend > 0; }
    static boolean enabled() { return enabled; }

    static void toggle(Minecraft client) {
        enabled = !enabled;
        if (client.player != null) {
            client.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    enabled ? "message.atzen.camera_on" : "message.atzen.camera_off"));
        }
    }

    static void trophy() { trophyTicks = TROPHY_TICKS; }

    static void skip() {
        trophyTicks = 0;
        if (state == State.WAITING || state == State.BITE || state == State.CASTING || state == State.TROPHY) restless = 60;
    }

    /** Once per client tick: pick the state and run the camera-type switch. */
    static void tick(Minecraft client) {
        var player = client.player;
        if (player == null || client.level == null) {
            restore(client);
            state = State.IDLE;
            return;
        }
        // The angler moving or looking around takes the camera back while nothing is hooked.
        // A small twitch of the mouse is not "looking around".
        if (lastPosition != null && state != State.CASTING && (player.position().distanceToSqr(lastPosition) > 0.01
                || Math.abs(Mth.wrapDegrees(player.getYRot() - lastYaw)) > 6 || Math.abs(player.getXRot() - lastPitch) > 6)) {
            restless = Math.max(restless, 30);
        }
        lastPosition = player.position();
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
        if (restless > 0) restless--;
        if (trophyTicks > 0) trophyTicks--;

        BattlePayload fight = AtzenClient.state;
        FishAction wanted = fight == null || fight.pending() != null ? FishAction.CALM : switch (fight.action()) {
            case RUN, DIVE, SIDE, JUMP -> fight.action();
            default -> FishAction.CALM;
        };
        shotHeld++;
        // Jumps cut in at once (they are short); every other shot is held at least two seconds.
        if (wanted != shotAction && (wanted == FishAction.JUMP || shotAction == FishAction.JUMP || shotHeld >= 40)) {
            if (shotAction != FishAction.JUMP || shotHeld >= 20) {
                shotAction = wanted;
                shotHeld = 0;
            }
        }
        State next = choose(client);
        if (next != state) {
            state = next;
            stateTicks = 0;
        } else {
            stateTicks++;
        }
        previousBlend = blend;
        blend = state == State.IDLE ? Math.max(0, blend - 1 / 14f) : Math.min(1, blend + 1 / 12f);
        // The player's camera type is never touched: the shot blends out of their own view and back into it, so there
        // is no jump between first and third person. The angler's body is drawn because the camera is detached.
        if (blend == 0 && previousBlend == 0) restore(client);
    }

    private static State choose(Minecraft client) {
        if (!enabled) return State.IDLE;
        BattlePayload fight = AtzenClient.state;
        if (fight != null) {
            if (fight.result() == FishingBattle.Result.CAUGHT) return trophyTicks > 0 ? State.TROPHY : State.IDLE;
            if (fight.result() != FishingBattle.Result.ACTIVE) return State.IDLE;
            if (AtzenClient.cameraTicks < 16) return State.HOOKED;
            if (fight.has(BattlePayload.LANDABLE) || fight.distance() < 5) return State.LANDING;
            return State.FIGHTING;
        }
        if (trophyTicks > 0) return State.TROPHY;
        FishingHook hook = client.player.fishing;
        if (hook == null || hook.isRemoved()) {
            castHook = null;
            return State.IDLE;
        }
        if (hook != castHook) {
            // A new cast: fix the shot on where the angler stands and looks now.
            castHook = hook;
            castEye = client.player.getEyePosition();
            Vec3 look = client.player.getLookAngle().multiply(1, 0, 1);
            castDir = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
            landedAt = -1;
            if (restless < 60) restless = 0;
        }
        if (landedAt < 0 && hook.isInWater()) landedAt = hook.tickCount;
        // The flight and a moment on the landed float are one shot; moving the mouse meanwhile does not cut it.
        if (restless < 60 && (landedAt < 0 ? hook.tickCount < 60 : hook.tickCount < landedAt + 25)) {
            if (landedAt < 0 || hook.isInWater()) return State.CASTING;
        }
        if (state == State.CASTING) {
            // Start watching for restlessness from here, not from where the mouse was during the cast.
            lastYaw = client.player.getYRot();
            lastPitch = client.player.getXRot();
            lastPosition = client.player.position();
        }
        if (restless > 0 || !hook.isInWater()) return State.IDLE;
        watched = approaching(client, hook);
        if (((BiteTarget) hook).atzen$isBiting() || watched != null) return State.BITE;
        return State.WAITING;
    }

    /** A fish close to the float and swimming at it. */
    private static AtzenFishEntity approaching(Minecraft client, FishingHook hook) {
        return client.level.getEntitiesOfClass(AtzenFishEntity.class, hook.getBoundingBox().inflate(4)).stream()
                .filter(fish -> fish.getDeltaMovement().dot(hook.position().subtract(fish.position())) > 0 || fish.distanceToSqr(hook) < 2)
                .min(Comparator.comparingDouble(fish -> fish.distanceToSqr(hook))).orElse(null);
    }

    private static void restore(Minecraft client) {
        smoothPosition = null;
        smoothFocus = null;
    }

    /** A new fight: start on the calm shot; the camera glides there from wherever it is. */
    static void reset() {
        shotAction = FishAction.CALM;
        shotHeld = 0;
    }

    /** Returns {x, y, z, yRot, xRot} for the camera, or null to keep vanilla's view. */
    public static double[] view(Entity entity, float partial, Vec3 vanilla, float vanillaYaw, float vanillaPitch) {
        Minecraft client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || entity != player || client.level == null || (blend == 0 && previousBlend == 0)) return null;
        long now = System.nanoTime();
        double seconds = lastFrame == 0 ? 0.016 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        frameSeconds = seconds;
        float time = player.tickCount + partial;
        Vec3 eye = player.getEyePosition(partial);
        // The player's own view (first or third person, as they play) is where the shot starts and ends.
        Vec3 base = vanilla;
        float baseYaw = vanillaYaw, basePitch = vanillaPitch;

        Vec3[] shot = shot(client, player, eye, partial, time);
        if (shot != null) {
            fromEye = shot[2] == null || state == State.CASTING;
            Vec3 position = clip(client, shot[2] == null ? eye : shot[2], shot[0], player);
            double follow = 1 - Math.exp(-seconds * (state == State.HOOKED || shotAction == FishAction.JUMP ? 6 : 2.5));
            smoothPosition = smoothPosition == null ? position : smoothPosition.lerp(position, follow);
            // The flying float is fast: the focus keeps up with it instead of trailing behind.
            smoothFocus = smoothFocus == null ? shot[1] : smoothFocus.lerp(shot[1], 1 - Math.exp(-seconds * (state == State.CASTING ? 14 : 5)));
        }
        if (smoothPosition == null || smoothFocus == null) return null;

        Vec3 look = smoothFocus.subtract(smoothPosition);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = Math.clamp((float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance())), -75f, 75f);
        BattlePayload fight = AtzenClient.state;
        if (fight != null && fight.result() == FishingBattle.Result.ACTIVE) {
            // only a light tremor when the line is close to breaking
            float shake = Math.max(0, AtzenClient.displayedTension - 0.8f) / 0.2f * 0.6f;
            yaw += shake * (float) Math.sin(time * 1.9);
            pitch += shake * (float) Math.cos(time * 2.3) * 0.5f;
        }
        float b = Mth.lerp(partial, previousBlend, blend);
        b = b * b * (3 - 2 * b);
        // The glide itself must not pass through blocks either: clip the blended position from the eye.
        Vec3 position = base.lerp(smoothPosition, b);
        if (fromEye) position = clip(client, eye, position, player);
        return new double[] {position.x, position.y, position.z,
                baseYaw + Mth.wrapDegrees(yaw - baseYaw) * b, Mth.lerp(b, basePitch, pitch)};
    }

    /** {camera position, focus, clip origin (null = eye)} for the current state. */
    private static Vec3[] shot(Minecraft client, net.minecraft.client.player.LocalPlayer player, Vec3 eye, float partial, float time) {
        FishingHook hook = player.fishing;
        Vec3 hookPos = hook != null ? hook.getPosition(partial) : eye.add(player.getLookAngle().scale(8));
        Vec3 fish = fishPosition(client, partial, hookPos);
        Vec3 toFish = fish.subtract(eye).multiply(1, 0, 1);
        if (toFish.lengthSqr() < 0.25) toFish = heading != null ? heading : player.getLookAngle().multiply(1, 0, 1);
        toFish = toFish.normalize();
        if (state == State.CASTING && castDir != null) heading = castDir;
        else heading = heading == null || state == State.IDLE ? toFish : heading.lerp(toFish, 1 - Math.exp(-frameSeconds * 2.5)).normalize();
        toFish = heading;
        Vec3 right = new Vec3(-toFish.z, 0, toFish.x);
        BattlePayload fight = AtzenClient.state;
        return switch (state) {
            case IDLE -> null;
            case CASTING -> {
                Vec3 dir = castDir != null ? castDir : player.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 from = castEye != null ? castEye : eye;
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                yield new Vec3[] {from.subtract(dir.scale(2.6)).add(side.scale(0.8)).add(0, 0.6, 0), hookPos, from};
            }
            case WAITING -> {
                double drift = Math.sin(time * 0.01) * 0.4;
                yield new Vec3[] {eye.add(right.scale(1.7 + drift)).subtract(toFish.scale(0.9)).add(0, 0.05, 0),
                        eye.lerp(hookPos, 0.85), null};
            }
            case BITE -> {
                Vec3 target = watched != null && !watched.isRemoved() ? watched.getPosition(partial).lerp(hookPos, 0.4) : hookPos.add(0, -0.4, 0);
                Vec3 under = hookPos.add(right.scale(2.2)).subtract(toFish.scale(0.6)).add(0, -0.9, 0);
                boolean water = client.level.getFluidState(net.minecraft.core.BlockPos.containing(under)).is(FluidTags.WATER);
                yield water ? new Vec3[] {under, target, hookPos.add(0, -0.3, 0)}
                        : new Vec3[] {hookPos.subtract(toFish.scale(2.6)).add(right.scale(0.8)).add(0, 0.9, 0), target, null};
            }
            case HOOKED -> new Vec3[] {eye.subtract(toFish.scale(2.2)).add(right.scale(0.9)).add(0, 0.5, 0), eye.lerp(fish, 0.6), null};
            case FIGHTING -> fighting(eye, fish, toFish, right, fight, time);
            case LANDING -> new Vec3[] {eye.add(toFish.scale(0.8)).add(right.scale(1.7)).add(0, -0.7, 0), fish, null};
            case TROPHY -> {
                double angle = stateTicks * 0.045 + Math.atan2(toFish.z, toFish.x);
                yield new Vec3[] {eye.add(Math.cos(angle) * 2.8, -0.2, Math.sin(angle) * 2.8), eye.add(0, -0.35, 0), null};
            }
        };
    }

    private static Vec3[] fighting(Vec3 eye, Vec3 fish, Vec3 toFish, Vec3 right, BattlePayload fight, float time) {
        return switch (shotAction) {
            // on the fish: beside it, a little above the water, the angler in the background
            case RUN, DIVE -> new Vec3[] {fish.subtract(toFish.scale(2.6)).add(right.scale(2.2)).add(0, 1.4, 0), fish,
                    fish.add(0, 1.0, 0)};
            // wide and high: the fish heading for cover
            case SIDE -> new Vec3[] {eye.subtract(toFish.scale(4.5)).add(0, 3.2, 0), eye.lerp(fish, 0.7), null};
            // tight behind the angler, the leap in front
            case JUMP -> new Vec3[] {eye.subtract(toFish.scale(1.8)).add(right.scale(0.7)).add(0, 0.45, 0), fish.add(0, 0.6, 0), null};
            // over the shoulder, swaying slowly from one shoulder to the other
            default -> {
                double sway = Math.sin(time * 0.025) * 0.9;
                double back = 3.0 - Math.min(1, fight == null ? 0 : (1 - fight.stamina()) * 0.6);
                yield new Vec3[] {eye.subtract(toFish.scale(back)).add(right.scale(1.4 * sway + 0.6))
                        .add(0, 0.9 + 0.2 * Math.sin(time * 0.04), 0), eye.lerp(fish, 0.55).add(0, -0.3, 0), null};
            }
        };
    }

    private static Vec3 fishPosition(Minecraft client, float partial, Vec3 fallback) {
        BattlePayload fight = AtzenClient.state;
        if (fight != null && fight.fishEntityId() >= 0) {
            Entity fish = client.level.getEntity(fight.fishEntityId());
            if (fish != null) return fish.getPosition(partial);
        }
        return fallback;
    }

    /** Never through a wall: stop short of the first block between the origin and the wanted position. */
    private static Vec3 clip(Minecraft client, Vec3 origin, Vec3 wanted, Entity player) {
        var hit = client.level.clip(new ClipContext(origin, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return wanted;
        return hit.getLocation().add(origin.subtract(hit.getLocation()).normalize().scale(0.3));
    }
}
