package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.ecology.Ecology;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * What a fish does when it is not biting: keep its depth, hunt, and as the leader of a shoal (or alone) follow the
 * behaviour of the moment. All of it only runs near players and checks rarely, so large waters stay cheap.
 */
final class FishBehaviour {
    private FishBehaviour() { }

    /** The shoal behaviours (Anforderung 16.3). */
    enum School { FEEDING, SHY, MIGRATING, SPAWNING, HUNTING, DEEP, TERRITORY }

    /** Picks what a fish (or the shoal it leads) does now, from species, time, weather, moon, food and danger. */
    static School pick(AtzenFishEntity fish) {
        FishSpecies species = fish.species();
        long time = Math.floorMod(fish.level().getOverworldClockTime(), 24000L);
        boolean day = time < 12500 || time >= 23500;
        if (fish.shyness() > 0.6f) return School.SHY;
        if (species.diet().predatory()) return fish.hunger() > 0.5f ? School.HUNTING : School.TERRITORY;
        if (species.salt() && day) return species.diet() == FishSpecies.Diet.SHOAL ? School.MIGRATING : School.DEEP;
        if (species.diet() == FishSpecies.Diet.COARSE && !day && fish.level() instanceof ServerLevel server && server.getMoonBrightness(fish.blockPosition()) > 0.9f) return School.SPAWNING;
        return School.FEEDING;
    }

    /** Leaders and loners move with purpose; followers just follow their leader (vanilla schooling). */
    static final class SchoolGoal extends Goal {
        private final AtzenFishEntity fish;
        private Vec3 target;
        private int ticks;
        private Vec3 territory;

        SchoolGoal(AtzenFishEntity fish) {
            this.fish = fish;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!fish.active() || fish.isBattleDummy() || fish.isFollower() || fish.pen() != null
                    || fish.getRandom().nextInt(reducedTickDelay(80)) != 0) return false;
            if (territory == null) territory = fish.position();
            target = destination(pick(fish));
            return target != null;
        }

        private Vec3 destination(School school) {
            var level = fish.level();
            var random = fish.getRandom();
            return switch (school) {
                case SHY -> {
                    Player player = level.getNearestPlayer(fish, 16);
                    yield player == null ? null : fish.position().add(fish.position().subtract(player.position()).normalize().scale(8));
                }
                case FEEDING -> {
                    BlockPos chum = Ecology.nearestChum((ServerLevel) level, fish.blockPosition(), 16);
                    if (chum != null) yield Vec3.atCenterOf(chum);
                    BlockPos plant = nearest(fish, 8, pos -> level.getBlockState(pos).is(Blocks.SEAGRASS)
                            || level.getBlockState(pos).is(Blocks.TALL_SEAGRASS) || level.getBlockState(pos).is(Blocks.KELP_PLANT));
                    yield plant == null ? null : Vec3.atCenterOf(plant);
                }
                case SPAWNING -> {
                    BlockPos plant = nearest(fish, 10, pos -> level.getBlockState(pos).is(Blocks.SEAGRASS) || level.getBlockState(pos).is(Blocks.LILY_PAD));
                    yield plant == null ? null : Vec3.atCenterOf(plant);
                }
                case MIGRATING -> {
                    // A shoal wanders the same way all day long.
                    double angle = (level.getOverworldClockTime() / 24000 % 8) * Math.PI / 4;
                    yield fish.position().add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12);
                }
                case HUNTING -> null;
                case DEEP -> fish.position().add(random.nextInt(9) - 4, -4, random.nextInt(9) - 4);
                case TERRITORY -> territory.add(random.nextInt(13) - 6, random.nextInt(3) - 1, random.nextInt(13) - 6);
            };
        }

        private static BlockPos nearest(AtzenFishEntity fish, int radius, java.util.function.Predicate<BlockPos> test) {
            BlockPos origin = fish.blockPosition();
            for (int i = 0; i < 24; i++) {
                BlockPos pos = origin.offset(fish.getRandom().nextInt(radius * 2 + 1) - radius,
                        fish.getRandom().nextInt(5) - 3, fish.getRandom().nextInt(radius * 2 + 1) - radius);
                if (test.test(pos)) return pos;
            }
            return null;
        }

        @Override
        public void start() {
            ticks = 0;
            fish.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return ++ticks < 120 && !fish.getNavigation().isDone();
        }
    }

    /** Keeps a fish at its depth: bottom fish near the ground, shoal fish near the surface, sea fish deeper by day. */
    static final class DepthGoal extends Goal {
        private final AtzenFishEntity fish;
        private double wanted;

        DepthGoal(AtzenFishEntity fish) {
            this.fish = fish;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!fish.active() || fish.isBattleDummy() || fish.getRandom().nextInt(reducedTickDelay(60)) != 0) return false;
            var level = fish.level();
            BlockPos pos = fish.blockPosition();
            int bottom = 0, top = 0;
            while (bottom < 12 && level.getFluidState(pos.below(bottom + 1)).is(FluidTags.WATER)) bottom++;
            while (top < 12 && level.getFluidState(pos.above(top + 1)).is(FluidTags.WATER)) top++;
            if (bottom + top < 2) return false;
            int depth = fish.species().preferredDepth();
            double low = pos.getY() - bottom + 0.6, high = pos.getY() + top + 0.2;
            wanted = depth < 0 ? low : depth > 0 ? high - 0.4 : (low + high) / 2;
            return Math.abs(fish.getY() - wanted) > 1.5;
        }

        @Override
        public void start() {
            fish.getNavigation().moveTo(fish.getX() + fish.getRandom().nextInt(5) - 2, wanted, fish.getZ() + fish.getRandom().nextInt(5) - 2, 0.8);
        }

        @Override
        public boolean canContinueToUse() { return !fish.getNavigation().isDone(); }
    }

    /** A hungry predator stalks a much smaller fish nearby; it catches it now and then, not always. */
    static final class HuntGoal extends Goal {
        private final AtzenFishEntity fish;
        private AbstractFish prey;
        private int ticks;

        HuntGoal(AtzenFishEntity fish) {
            this.fish = fish;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!fish.active() || fish.isBattleDummy() || fish.pen() != null || !fish.species().diet().predatory()
                    || fish.hunger() < 0.5f || fish.getRandom().nextInt(reducedTickDelay(40)) != 0) return false;
            double size = fish.specimen().lengthCm();
            prey = fish.level().getEntitiesOfClass(AbstractFish.class, fish.getBoundingBox().inflate(8), other -> other != fish
                            && other.isAlive() && !(other instanceof AtzenFishEntity a && (a.isBattleDummy() || a.pen() != null
                            || a.specimen().lengthCm() > size * 0.4)))
                    .stream().min(Comparator.comparingDouble(fish::distanceToSqr)).orElse(null);
            return prey != null;
        }

        @Override
        public void start() { ticks = 0; }

        @Override
        public boolean canContinueToUse() { return prey != null && prey.isAlive() && ++ticks < 100; }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (ticks % 10 == 0) fish.getNavigation().moveTo(prey, 1.6);
            if (fish.distanceToSqr(prey) < 1.5) {
                if (fish.getRandom().nextFloat() < 0.3f) {
                    prey.discard();
                    fish.feed(1);
                } else if (prey instanceof AtzenFishEntity startled) {
                    startled.startle(0.4f);
                }
                prey = null;
            }
        }

        @Override
        public void stop() {
            prey = null;
            fish.getNavigation().stop();
        }
    }
}
