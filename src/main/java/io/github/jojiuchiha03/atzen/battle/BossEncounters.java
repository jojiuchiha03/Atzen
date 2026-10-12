package io.github.jojiuchiha03.atzen.battle;

import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.item.BaitType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;

/**
 * When a boss takes the bait. Every boss needs its own bait, cast in its own water at its own time:
 * <ul>
 *   <li>The Old One of the Deep: at night, in a lake or swamp at least five blocks deep.</li>
 *   <li>The Reed King: in a swamp in the rain, or at dawn or dusk.</li>
 *   <li>The Black Sturgeon: by day, in a river at least four blocks deep.</li>
 *   <li>The Deep Warden: in hidden water, below y 0 and out of sight of the sky.</li>
 * </ul>
 * Even then only about every third bite is the boss, and each angler meets each boss once per day.
 */
public final class BossEncounters {
    private static final double CHANCE = 0.4;
    private final Map<UUID, Map<Boss, Long>> lastCaught = new HashMap<>();

    public Boss check(ServerLevel level, BlockPos hook, BaitType bait, UUID angler, Random random) {
        if (bait == null || bait.boss() == null || !conditions(level, hook, bait.boss())) return null;
        Long day = lastCaught.getOrDefault(angler, Map.of()).get(bait.boss());
        if (day != null && day == day(level)) return null;
        return random.nextDouble() < CHANCE ? bait.boss() : null;
    }

    public void caught(ServerLevel level, UUID angler, Boss boss) {
        lastCaught.computeIfAbsent(angler, key -> new EnumMap<>(Boss.class)).put(boss, day(level));
    }

    public static boolean conditions(ServerLevel level, BlockPos hook, Boss boss) {
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        boolean night = time >= 13000 && time < 23000;
        boolean twilight = time >= 22500 || time < 1500 || (time >= 11500 && time < 13500);
        Habitat habitat = Habitat.at(level, hook);
        return switch (boss) {
            case ANCIENT_CATFISH -> night && (habitat == Habitat.LAKE || habitat == Habitat.SWAMP) && depth(level, hook) >= 5;
            case REED_KING -> habitat == Habitat.SWAMP && (level.isRaining() || twilight);
            case BLACK_STURGEON -> !night && (habitat == Habitat.RIVER || habitat == Habitat.COLD) && depth(level, hook) >= 4;
            case DEEP_WARDEN -> hook.getY() < 0 && !level.canSeeSkyFromBelowWater(hook);
        };
    }

    private static int depth(ServerLevel level, BlockPos hook) {
        int depth = 0;
        BlockPos pos = hook;
        while (depth < 16 && level.getFluidState(pos).is(FluidTags.WATER)) {
            depth++;
            pos = pos.below();
        }
        return depth;
    }

    private static long day(ServerLevel level) { return level.getOverworldClockTime() / 24000; }
}
