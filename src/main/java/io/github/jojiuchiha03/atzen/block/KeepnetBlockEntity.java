package io.github.jojiuchiha03.atzen.block;

import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import io.github.jojiuchiha03.atzen.holding.FishStore;
import io.github.jojiuchiha03.atzen.holding.WaterConditions;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** The fish in a keepnet and the water they live in; checked once a minute. */
public final class KeepnetBlockEntity extends BlockEntity {
    public static final int CAPACITY = 8;
    public static final double MAX_WEIGHT = 40, MAX_LENGTH = 120;
    private final FishStore store = new FishStore(CAPACITY, MAX_WEIGHT, MAX_LENGTH);
    /** Water quality: drops with the load, recovers in moving water and rain. */
    private double quality = 1;
    private long lastCheck = -1;
    private WaterConditions last;

    public KeepnetBlockEntity(BlockPos pos, BlockState state) { super(AtzenBlocks.KEEPNET_ENTITY, pos, state); }

    static void tick(Level level, BlockPos pos, BlockState state, KeepnetBlockEntity net) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) return;
        if (net.lastCheck < 0) net.lastCheck = level.getGameTime();
        if (level.getGameTime() - net.lastCheck < 1200) return;
        double minutes = (level.getGameTime() - net.lastCheck) / 1200.0;
        net.lastCheck = level.getGameTime();
        if (net.store.size() == 0) {
            net.quality = Math.min(1, net.quality + 0.05 * minutes);
            return;
        }
        net.check(server, minutes);
    }

    private WaterConditions conditions(ServerLevel level) {
        Holding.Survey survey = Holding.survey(level, worldPosition, 1, 1);
        boolean inWater = getBlockState().getValue(KeepnetBlock.WATERLOGGED);
        double temperature = Holding.temperature(level, worldPosition, store.fish().stream().map(FishStore.Kept::species).toList());
        WaterConditions water = Holding.conditions(survey, 12, store.density(), temperature, quality);
        return inWater ? water : new WaterConditions(0, 0, water.temperature(), water.density(), water.quality());
    }

    private void check(ServerLevel level, double minutes) {
        Holding.Survey survey = Holding.survey(level, worldPosition, 1, 1);
        quality = survey.moving() || level.isRainingAt(worldPosition.above())
                ? Math.min(1, quality + 0.05 * minutes) : Math.max(0, quality - 0.004 * store.size() * minutes);
        WaterConditions water = conditions(level);
        last = water;
        List<FishStore.Kept> dead = store.age(water, minutes);
        for (FishStore.Kept fish : dead) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, item(level, fish));
            notify(level, Component.translatable("holding.atzen.died", Component.translatable(fish.species().translationKey()))
                    .withStyle(ChatFormatting.RED));
        }
        List<String> problems = water.problems();
        if (!problems.isEmpty() && store.size() > 0) {
            notify(level, Component.translatable("holding.atzen.warning", Component.translatable(problems.getFirst()),
                    Math.round(store.averageCondition())).withStyle(ChatFormatting.GOLD));
        }
        setChanged();
    }

    private void notify(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 24 * 24) player.sendSystemMessage(message, true);
        }
    }

    private ItemStack item(ServerLevel level, FishStore.Kept fish) {
        return FishCatch.create(new FishSpecimen(fish.species(), fish.lengthCm(), fish.weightKg()), level, worldPosition,
                fish.quality(), fish.angler(), fish.id(), fish.condition());
    }

    /** A fish from the hand goes in; true if the item was a fish. */
    boolean put(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FishCatch.Catch fish = FishCatch.read(stack);
        if (fish == null) return false;
        FishSpecimen specimen = fish.specimen();
        FishStore.Add result = store.add(new FishStore.Kept(fish.id(), specimen.species(), specimen.lengthCm(), specimen.weightKg(),
                fish.condition(), fish.quality(), fish.angler()));
        if (result == FishStore.Add.OK) {
            stack.shrink(1);
            level.playSound(null, worldPosition, SoundEvents.FISH_SWIM, SoundSource.BLOCKS, 0.8f, 1.0f);
            setChanged();
        }
        player.sendSystemMessage(Component.translatable("holding.atzen.add." + result.name().toLowerCase(java.util.Locale.ROOT),
                store.size(), CAPACITY, String.format(java.util.Locale.GERMAN, "%.1f", store.totalWeight()), MAX_WEIGHT), true);
        return true;
    }

    void takeOut(ServerPlayer player) {
        if (store.size() == 0) {
            status(player);
            return;
        }
        FishStore.Kept fish = store.takeLast();
        player.getInventory().placeItemBackInInventory(item((ServerLevel) level, fish));
        setChanged();
        player.sendSystemMessage(Component.translatable("holding.atzen.taken", Component.translatable(fish.species().translationKey()),
                store.size(), CAPACITY), true);
    }

    void releaseAll(ServerPlayer player) {
        ServerLevel server = (ServerLevel) level;
        List<FishStore.Kept> all = store.takeAll();
        Vec3 at = Vec3.atCenterOf(worldPosition);
        for (FishStore.Kept fish : all) {
            AtzenFishEntity.spawn(server, new FishSpecimen(fish.species(), fish.lengthCm(), fish.weightKg()), fish.id(), fish.condition(), at);
        }
        setChanged();
        player.sendSystemMessage(Component.translatable("holding.atzen.released_all", all.size()), true);
    }

    private void status(ServerPlayer player) {
        WaterConditions water = last != null ? last : conditions((ServerLevel) level);
        Component problems = water.problems().isEmpty() ? Component.translatable("holding.atzen.water_good")
                : Component.translatable(water.problems().getFirst());
        player.sendSystemMessage(Component.translatable("holding.atzen.status", store.size(), CAPACITY,
                String.format(java.util.Locale.GERMAN, "%.1f", store.totalWeight()), MAX_WEIGHT, problems), true);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            for (FishStore.Kept fish : store.takeAll()) Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item(server, fish));
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Fish", FishStore.Kept.CODEC.listOf(), List.copyOf(store.fish()));
        output.putDouble("Quality", quality);
        output.putLong("LastCheck", lastCheck);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        store.takeAll();
        input.read("Fish", FishStore.Kept.CODEC.listOf()).ifPresent(list -> list.forEach(store::restore));
        quality = input.getDoubleOr("Quality", 1);
        lastCheck = input.getLongOr("LastCheck", -1);
    }
}
