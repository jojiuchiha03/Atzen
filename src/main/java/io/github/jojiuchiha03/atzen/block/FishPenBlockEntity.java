package io.github.jojiuchiha03.atzen.block;

import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import io.github.jojiuchiha03.atzen.holding.WaterConditions;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The fish living in a pen are real fish entities bound to it (they keep their identity, age, hunger and health);
 * the pen only checks the water every minute and lets healthy pairs breed.
 */
public final class FishPenBlockEntity extends BlockEntity {
    /** Water blocks one fish needs. */
    public static final int WATER_PER_FISH = 24;
    private double quality = 1;
    private long lastCheck = -1;
    private long lastBreeding;

    public FishPenBlockEntity(BlockPos pos, BlockState state) { super(AtzenBlocks.FISH_PEN_ENTITY, pos, state); }

    List<AtzenFishEntity> fish(ServerLevel level) {
        return level.getEntitiesOfClass(AtzenFishEntity.class, new AABB(worldPosition).inflate(10),
                fish -> worldPosition.equals(fish.pen()) && fish.isAlive());
    }

    int capacity(ServerLevel level) { return Holding.survey(level, worldPosition, 3, 2).water() / WATER_PER_FISH; }

    static void tick(Level level, BlockPos pos, BlockState state, FishPenBlockEntity pen) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) return;
        if (pen.lastCheck < 0) pen.lastCheck = level.getGameTime();
        if (level.getGameTime() - pen.lastCheck < 1200) return;
        double minutes = (level.getGameTime() - pen.lastCheck) / 1200.0;
        pen.lastCheck = level.getGameTime();
        pen.check(server, minutes);
    }

    private WaterConditions conditions(ServerLevel level, List<AtzenFishEntity> fish) {
        Holding.Survey survey = Holding.survey(level, worldPosition, 3, 2);
        int capacity = Math.max(1, survey.water() / WATER_PER_FISH);
        double temperature = Holding.temperature(level, worldPosition, fish.stream().map(AtzenFishEntity::species).toList());
        WaterConditions water = Holding.conditions(survey, fish.size() * WATER_PER_FISH, fish.size() / (double) capacity, temperature, quality);
        // Hungry fish are stressed fish: feeding counts like water quality.
        double hunger = fish.stream().mapToDouble(AtzenFishEntity::hunger).average().orElse(0);
        return new WaterConditions(water.volume(), water.oxygen(), water.temperature(), water.density(),
                Math.min(water.quality(), 1.2 - hunger));
    }

    private void check(ServerLevel level, double minutes) {
        List<AtzenFishEntity> fish = fish(level);
        if (fish.isEmpty()) {
            quality = Math.min(1, quality + 0.05 * minutes);
            return;
        }
        Holding.Survey survey = Holding.survey(level, worldPosition, 3, 2);
        quality = survey.moving() ? Math.min(1, quality + 0.05 * minutes) : Math.max(0, quality - 0.002 * fish.size() * minutes);
        WaterConditions water = conditions(level, fish);
        double change = water.perMinute() * minutes;
        for (AtzenFishEntity one : fish) {
            // The condition in per cent of the fish's health.
            float health = (float) (one.getHealth() + one.getMaxHealth() * change / 100);
            if (health <= 0) one.kill(level);
            else one.setHealth(Math.min(one.getMaxHealth(), health));
        }
        if (!water.problems().isEmpty()) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 32 * 32) {
                    player.sendSystemMessage(Component.translatable("holding.atzen.pen_warning", Component.translatable(water.problems().getFirst()))
                            .withStyle(ChatFormatting.GOLD), true);
                }
            }
        }
        breed(level, fish, survey.water() / WATER_PER_FISH);
        setChanged();
    }

    /** Every ten minutes one well-fed, healthy pair of a species may have young, if there is room. */
    private void breed(ServerLevel level, List<AtzenFishEntity> fish, int capacity) {
        if (level.getGameTime() - lastBreeding < 12000 || fish.size() >= capacity) return;
        for (AtzenFishEntity a : fish) {
            if (!ready(a)) continue;
            for (AtzenFishEntity b : fish) {
                if (a == b || b.species() != a.species() || !ready(b)) continue;
                FishSpecies species = a.species();
                Random random = new Random(level.getRandom().nextLong());
                FishSpecimen young = species.roll(random, -1);
                FishSpecimen small = new FishSpecimen(species, species.minLength(), young.weightKg() * Math.pow(species.minLength() / young.lengthCm(), 3));
                AtzenFishEntity child = AtzenFishEntity.spawn(level, small, UUID.randomUUID(), 100, a.position());
                if (child != null) child.setPen(worldPosition);
                a.startle(0.1f);
                b.startle(0.1f);
                lastBreeding = level.getGameTime();
                level.sendParticles(ParticleTypes.HEART, a.getX(), a.getY() + 0.5, a.getZ(), 4, 0.3, 0.3, 0.3, 0);
                return;
            }
        }
    }

    private static boolean ready(AtzenFishEntity fish) {
        return fish.species().boss() == null && fish.hunger() < 0.35f && fish.condition() > 80 && fish.ageDays() >= 1;
    }

    boolean put(ServerPlayer player, InteractionHand hand) {
        ServerLevel level = (ServerLevel) this.level;
        ItemStack stack = player.getItemInHand(hand);
        List<AtzenFishEntity> fish = fish(level);
        if (BattleEquipment.isBait(stack) || stack.is(AtzenItems.GROUNDBAIT)) {
            if (fish.isEmpty()) return false;
            fish.forEach(one -> one.feed(stack.is(AtzenItems.GROUNDBAIT) ? 1f : 0.4f));
            stack.consume(1, player);
            level.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 12, 1, 0.2, 1, 0.1);
            player.sendSystemMessage(Component.translatable("holding.atzen.fed", fish.size()), true);
            return true;
        }
        FishCatch.Catch caught = FishCatch.read(stack);
        if (caught == null) return false;
        if (caught.condition() <= 0) {
            player.sendSystemMessage(Component.translatable("holding.atzen.add.dead"), true);
            return true;
        }
        if (fish.size() >= capacity(level)) {
            player.sendSystemMessage(Component.translatable("holding.atzen.pen_full", fish.size(), capacity(level)), true);
            return true;
        }
        // One individual exists only once: the item turns into the fish.
        for (AtzenFishEntity other : fish) if (other.identity().equals(caught.id())) return true;
        AtzenFishEntity entity = AtzenFishEntity.spawn(level, caught.specimen(), caught.id(), caught.condition(),
                Vec3.atCenterOf(worldPosition).add(0, 0.6, 0));
        if (entity == null) return true;
        entity.setPen(worldPosition);
        stack.shrink(1);
        level.playSound(null, worldPosition, SoundEvents.FISH_SWIM, SoundSource.BLOCKS, 0.8f, 1.0f);
        player.sendSystemMessage(Component.translatable("holding.atzen.pen_added", Component.translatable(caught.specimen().species().translationKey()),
                fish.size() + 1, capacity(level)), true);
        return true;
    }

    void status(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        List<AtzenFishEntity> fish = fish(level);
        WaterConditions water = conditions(level, fish);
        Component problems = water.problems().isEmpty() ? Component.translatable("holding.atzen.water_good")
                : Component.translatable(water.problems().getFirst());
        double condition = fish.stream().mapToDouble(AtzenFishEntity::condition).average().orElse(100);
        player.sendSystemMessage(Component.translatable("holding.atzen.pen_status", fish.size(), capacity(level),
                Math.round(condition), problems), true);
    }

    void releaseAll(ServerPlayer player) {
        List<AtzenFishEntity> fish = fish((ServerLevel) level);
        fish.forEach(one -> one.setPen(null));
        player.sendSystemMessage(Component.translatable("holding.atzen.released_all", fish.size()), true);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) fish(server).forEach(one -> one.setPen(null));
        super.preRemoveSideEffects(pos, state);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("Quality", quality);
        output.putLong("LastCheck", lastCheck);
        output.putLong("LastBreeding", lastBreeding);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        quality = input.getDoubleOr("Quality", 1);
        lastCheck = input.getLongOr("LastCheck", -1);
        lastBreeding = input.getLongOr("LastBreeding", 0);
    }

}
