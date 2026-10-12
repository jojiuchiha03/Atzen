package io.github.jojiuchiha03.atzen.block;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.Performance;
import io.github.jojiuchiha03.atzen.ecology.Ecology;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import io.github.jojiuchiha03.atzen.item.GearSlot;
import io.github.jojiuchiha03.atzen.item.ModuleType;
import io.github.jojiuchiha03.atzen.item.RodMods;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** The rods in a holder: where each one's bait lies, when the next bite comes and whether a fish is on now. */
public final class RodHolderBlockEntity extends BlockEntity {
    /** How long a bite waits for the angler; a bite sensor gives more time. */
    static final int BITE_WINDOW = 160;
    private final ItemStack[] rods = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final BlockPos[] targets = new BlockPos[3];
    private final int[] timers = new int[3];
    private final int[] biting = {-1, -1, -1};
    private UUID owner;

    public RodHolderBlockEntity(BlockPos pos, BlockState state) { super(AtzenBlocks.ROD_HOLDER_ENTITY, pos, state); }

    static boolean isRod(ItemStack stack) { return stack.getItem() instanceof FishingRodItem; }

    private RodHolderBlock.Kind kind() {
        return getBlockState().getBlock() instanceof RodHolderBlock block ? block.kind() : RodHolderBlock.Kind.SINGLE;
    }

    /** The water spot in front of the holder where a rod's bait lies (each rod a little to the side). */
    private BlockPos findWater(ServerLevel level, int slot, double reach) {
        Direction facing = getBlockState().getValue(RodHolderBlock.FACING);
        Direction side = facing.getClockWise();
        int offset = slot - (kind().rods() - 1) / 2;
        for (int distance = (int) Math.round(reach); distance >= 3; distance--) {
            BlockPos column = worldPosition.relative(facing, distance).relative(side, offset * 2);
            for (int dy = 2; dy >= -6; dy--) {
                BlockPos pos = column.above(dy);
                if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) return pos;
            }
        }
        return null;
    }

    void place(ServerPlayer player, InteractionHand hand) {
        ServerLevel level = (ServerLevel) this.level;
        int slot = -1;
        for (int i = 0; i < kind().rods(); i++) if (rods[i].isEmpty()) { slot = i; break; }
        if (slot < 0) {
            player.sendSystemMessage(Component.translatable("holder.atzen.full"), true);
            return;
        }
        ItemStack rod = player.getItemInHand(hand);
        BlockPos water = findWater(level, slot, 6 + 6 * BattleEquipment.castFactor(player, rod));
        if (water == null) {
            player.sendSystemMessage(Component.translatable("holder.atzen.no_water"), true);
            return;
        }
        rods[slot] = rod.copy();
        player.setItemInHand(hand, ItemStack.EMPTY);
        targets[slot] = water;
        timers[slot] = waitTicks(level, player, rods[slot], water);
        biting[slot] = -1;
        owner = player.getUUID();
        level.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.1);
        level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6f, 0.8f);
        update();
        player.sendSystemMessage(Component.translatable("holder.atzen.placed", slot + 1, kind().rods()), true);
    }

    /** Waiting time until the next bite: bait, pressure on the spot, groundbait, Lure, a float. */
    private int waitTicks(ServerLevel level, ServerPlayer player, ItemStack rod, BlockPos water) {
        double ticks = 600 + level.getRandom().nextInt(1800);
        boolean bait = !AtzenRodItem.bait(rod).isEmpty();
        if (!bait) ticks *= 3;
        ticks /= Ecology.factor(level, water);
        if (Ecology.nearChum(level, water) || RodMods.has(rod, ModuleType.FEEDER)) ticks *= 0.6;
        if (player != null) ticks -= 100 * BattleEquipment.lure(player, rod);
        if (RodMods.has(rod, ModuleType.FLOAT)) ticks *= 0.9;
        return (int) Math.max(200, ticks);
    }

    void take(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level;
        for (int i = 0; i < kind().rods(); i++) {
            if (biting[i] < 0 || rods[i].isEmpty()) continue;
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty()) {
                if (player.getInventory().getFreeSlot() < 0) {
                    player.sendSystemMessage(Component.translatable("holder.atzen.hands_full"), true);
                    return;
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.getInventory().add(held);
            }
            ItemStack rod = rods[i];
            BlockPos target = targets[i];
            player.setItemInHand(InteractionHand.MAIN_HAND, rod);
            clear(i);
            update();
            if (!Atzen.BATTLES.startFromHolder(player, rod, Vec3.atCenterOf(target).add(0, 0.4, 0),
                    RodMods.has(rod, ModuleType.BITE_SENSOR))) {
                player.sendSystemMessage(Component.translatable("holder.atzen.missed"), true);
            }
            return;
        }
        // No bite: reel the last rod in.
        for (int i = kind().rods() - 1; i >= 0; i--) {
            if (rods[i].isEmpty()) continue;
            player.getInventory().placeItemBackInInventory(rods[i]);
            clear(i);
            update();
            level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.6f, 1f);
            return;
        }
        player.sendSystemMessage(Component.translatable("holder.atzen.empty"), true);
    }

    private void clear(int slot) {
        rods[slot] = ItemStack.EMPTY;
        targets[slot] = null;
        biting[slot] = -1;
        timers[slot] = 0;
    }

    static void tick(Level level, BlockPos pos, BlockState state, RodHolderBlockEntity holder) {
        if (!(level instanceof ServerLevel server)) return;
        int step = Performance.current().holderInterval();
        if (level.getGameTime() % step != 0) return;
        boolean changed = false;
        for (int i = 0; i < holder.kind().rods(); i++) {
            if (holder.rods[i].isEmpty() || holder.targets[i] == null) continue;
            if (holder.biting[i] >= 0) {
                holder.biting[i] += step;
                holder.alarm(server, i);
                int window = RodMods.has(holder.rods[i], ModuleType.BITE_SENSOR) ? BITE_WINDOW * 3 / 2 : BITE_WINDOW;
                if (holder.biting[i] > window) {
                    holder.unattended(server, i);
                    changed = true;
                }
            } else if ((holder.timers[i] -= step) <= 0) {
                holder.bite(server, i);
                changed = true;
            }
        }
        if (changed) holder.update();
    }

    private void bite(ServerLevel level, int slot) {
        // Two baits lying close together: the fish runs across both lines and they tangle.
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-8, -3, -8), worldPosition.offset(8, 3, 8))) {
            if (!(level.getBlockEntity(pos) instanceof RodHolderBlockEntity other)) continue;
            for (int j = 0; j < other.kind().rods(); j++) {
                if ((other == this && j == slot) || other.targets[j] == null || other.rods[j].isEmpty()) continue;
                if (other.targets[j].distSqr(targets[slot]) <= 4 && level.getRandom().nextFloat() < 0.3f) {
                    tangle(level, slot);
                    other.tangle(level, j);
                    other.update();
                    message(level, Component.translatable("holder.atzen.tangled").withStyle(ChatFormatting.GOLD));
                    return;
                }
            }
        }
        biting[slot] = 0;
        BlockPos water = targets[slot];
        level.sendParticles(ParticleTypes.FISHING, water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 20, 0.3, 0.1, 0.3, 0.2);
        level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6f, 1f);
    }

    private void tangle(ServerLevel level, int slot) {
        AtzenRodItem.set(rods[slot], GearSlot.BAIT, AtzenRodItem.use(AtzenRodItem.bait(rods[slot])));
        timers[slot] = waitTicks(level, null, rods[slot], targets[slot]) * 2;
        biting[slot] = -1;
    }

    /** The indicator: a click and the rod tip for every holder, the pod's alarm beeps louder and faster. */
    private void alarm(ServerLevel level, int slot) {
        int t = biting[slot];
        if (kind().alarm()) {
            if (t % 10 == 0) level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.2f, 1.4f + slot * 0.2f);
        } else if (t % 20 == 0) {
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.7f, 1.2f);
        }
        if (t % 20 == 0) {
            BlockPos water = targets[slot];
            level.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.1);
        }
    }

    /** Nobody came: the fish gets away with the bait, sometimes the lure, and a big one can take the rod. */
    private void unattended(ServerLevel level, int slot) {
        ItemStack rod = rods[slot];
        float roll = level.getRandom().nextFloat();
        Habitat habitat = Habitat.at(level, targets[slot]);
        boolean big = FishSpecies.choose(habitat, new java.util.Random(level.getRandom().nextLong())).maxLength() > 100;
        if (big && roll < 0.2f && !kind().alarm()) {
            ItemEntity drop = new ItemEntity(level, targets[slot].getX() + 0.5, targets[slot].getY() + 0.5, targets[slot].getZ() + 0.5, rod);
            level.addFreshEntity(drop);
            clear(slot);
            message(level, Component.translatable("holder.atzen.rod_pulled").withStyle(ChatFormatting.RED));
            return;
        }
        if (roll < 0.45f) {
            AtzenRodItem.loseLure(rod);
            rod.setDamageValue(Math.min(rod.getMaxDamage() - 1, rod.getDamageValue() + 1));
            message(level, Component.translatable("holder.atzen.line_taken").withStyle(ChatFormatting.GOLD));
        } else {
            message(level, Component.translatable("holder.atzen.escaped").withStyle(ChatFormatting.GRAY));
        }
        AtzenRodItem.set(rod, GearSlot.BAIT, AtzenRodItem.use(AtzenRodItem.bait(rod)));
        biting[slot] = -1;
        timers[slot] = waitTicks(level, null, rod, targets[slot]);
    }

    private void message(ServerLevel level, Component text) {
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player != null && player.level() == level && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 96 * 96) player.sendSystemMessage(text, true);
    }

    private void update() {
        boolean bite = false, loaded = false;
        for (int i = 0; i < rods.length; i++) {
            bite |= biting[i] >= 0 && !rods[i].isEmpty();
            loaded |= !rods[i].isEmpty();
        }
        BlockState state = getBlockState();
        if (state.hasProperty(RodHolderBlock.BITE) && (state.getValue(RodHolderBlock.BITE) != bite || state.getValue(RodHolderBlock.LOADED) != loaded)) {
            level.setBlock(worldPosition, state.setValue(RodHolderBlock.BITE, bite).setValue(RodHolderBlock.LOADED, loaded), 3);
        }
        setChanged();
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) for (ItemStack rod : rods) if (!rod.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rod);
        super.preRemoveSideEffects(pos, state);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < rods.length; i++) {
            if (rods[i].isEmpty()) continue;
            ValueOutput child = output.child("Rod" + i);
            child.store("Item", ItemStack.CODEC, rods[i]);
            if (targets[i] != null) child.putLong("Target", targets[i].asLong());
            child.putInt("Timer", timers[i]);
            child.putInt("Biting", biting[i]);
        }
        if (owner != null) output.store("Owner", UUIDUtil.CODEC, owner);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < rods.length; i++) {
            Optional<ValueInput> child = input.child("Rod" + i);
            if (child.isEmpty()) {
                clear(i);
                continue;
            }
            rods[i] = child.get().read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            targets[i] = child.get().getLong("Target").map(BlockPos::of).orElse(null);
            timers[i] = child.get().getIntOr("Timer", 600);
            biting[i] = child.get().getIntOr("Biting", -1);
        }
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

}
