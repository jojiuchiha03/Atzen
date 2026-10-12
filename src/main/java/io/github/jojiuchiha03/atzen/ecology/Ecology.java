package io.github.jojiuchiha03.atzen.ecology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jojiuchiha03.atzen.Atzen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The simplified water ecology (Anforderung 17), kept per chunk so it costs nothing where nobody fishes:
 * <ul>
 *   <li>Fishing pressure: every fish taken out of a chunk raises it, time lets it settle (see {@link Pressure}).
 *       Bites and new fish get rarer while it is high; fish there are shyer.</li>
 *   <li>Feeding spots: groundbait draws coarse fish and makes them bite faster for a while.</li>
 * </ul>
 * Predators eat smaller fish and shoals move with the time of day (see the fish behaviour), so what lives in a
 * stretch of water changes with fishing, food and predators, and recovers on its own.
 */
public final class Ecology {
    private Ecology() { }

    /** Pressure of one chunk and when it was last updated (game time). */
    public record ChunkPressure(double value, long time) {
        public static final Codec<ChunkPressure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("value").forGetter(ChunkPressure::value),
                Codec.LONG.fieldOf("time").forGetter(ChunkPressure::time)).apply(instance, ChunkPressure::new));
    }

    public static final AttachmentType<ChunkPressure> PRESSURE = AttachmentRegistry.createPersistent(Atzen.id("pressure"), ChunkPressure.CODEC);

    private record Chum(BlockPos pos, long until) { }

    private static final Map<ResourceKey<Level>, List<Chum>> CHUM = new HashMap<>();

    public static void initialize() { }

    public static double pressure(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkPressure stored = chunk.getAttached(PRESSURE);
        return stored == null ? 0 : Pressure.decayed(stored.value(), level.getGameTime() - stored.time());
    }

    /** A fish was taken out here. */
    public static void caught(ServerLevel level, BlockPos pos, boolean released) {
        double now = Pressure.afterCatch(pressure(level, pos), released);
        level.getChunkAt(pos).setAttached(PRESSURE, new ChunkPressure(now, level.getGameTime()));
    }

    /** A fish was put back here: half of what its catch cost the water comes back. */
    public static void released(ServerLevel level, BlockPos pos) {
        double now = Math.max(0, pressure(level, pos) - Pressure.PER_CATCH / 2);
        level.getChunkAt(pos).setAttached(PRESSURE, new ChunkPressure(now, level.getGameTime()));
    }

    /** Factor on bites and spawns here. */
    public static double factor(ServerLevel level, BlockPos pos) { return Pressure.biteFactor(pressure(level, pos)); }

    /** Groundbait thrown here: a feeding spot for five minutes. */
    public static void chum(ServerLevel level, BlockPos pos, int ticks) {
        List<Chum> spots = CHUM.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        spots.removeIf(spot -> spot.until() < level.getGameTime());
        if (spots.size() > 256) spots.removeFirst();
        spots.add(new Chum(pos.immutable(), level.getGameTime() + ticks));
    }

    public static BlockPos nearestChum(ServerLevel level, BlockPos pos, int radius) {
        List<Chum> spots = CHUM.get(level.dimension());
        if (spots == null) return null;
        BlockPos best = null;
        double bestDistance = radius * radius;
        for (Chum spot : spots) {
            if (spot.until() < level.getGameTime()) continue;
            double distance = spot.pos().distSqr(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = spot.pos();
            }
        }
        return best;
    }

    /** Near a feeding spot coarse fish bite more readily. */
    public static boolean nearChum(ServerLevel level, BlockPos pos) { return nearestChum(level, pos, 10) != null; }
}
