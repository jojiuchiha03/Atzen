package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.ecology.Ecology;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.progress.Progression;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The aids from Anforderung 11: groundbait, echo sounder, bait box, angler's tool and the fish lexicon. */
public final class FishingTools {
    private FishingTools() { }

    /** The water surface the player looks at, within reach; null if none. */
    public static BlockPos water(Level level, Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return null;
        return hit.getBlockPos();
    }

    /** Groundbait: thrown on water it makes a feeding spot for five minutes. */
    public static final class Groundbait extends Item {
        public Groundbait(Properties properties) { super(properties); }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            BlockPos water = water(level, player, 12);
            if (water == null) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                Ecology.chum(server, water, 6000);
                server.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 30, 0.6, 0.1, 0.6, 0.1);
                server.playSound(null, water, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.6f, 1.2f);
                player.getItemInHand(hand).consume(1, player);
                ((ServerPlayer) player).sendSystemMessage(Component.translatable("message.atzen.groundbait"), true);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Echo sounder: lists the fish below within 24 blocks, the depth, and how fished-out the spot is. */
    public static final class EchoSounder extends Item {
        public EchoSounder(Properties properties) { super(properties); }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
            BlockPos water = water(level, player, 24);
            BlockPos center = water != null ? water : player.blockPosition();
            int depth = 0;
            while (depth < 64 && level.getFluidState(center.below(depth)).is(FluidTags.WATER)) depth++;
            Map<io.github.jojiuchiha03.atzen.fish.FishSpecies, Integer> counts = new EnumMap<>(io.github.jojiuchiha03.atzen.fish.FishSpecies.class);
            for (AtzenFishEntity fish : server.getEntitiesOfClass(AtzenFishEntity.class, new net.minecraft.world.phys.AABB(center).inflate(24),
                    fish -> !fish.isBattleDummy())) {
                counts.merge(fish.species(), 1, Integer::sum);
            }
            serverPlayer.sendSystemMessage(Component.translatable("message.atzen.sonar", depth,
                    Math.round(Ecology.pressure(server, center) * 100)).withStyle(ChatFormatting.AQUA));
            if (counts.isEmpty()) serverPlayer.sendSystemMessage(Component.translatable("message.atzen.sonar_empty").withStyle(ChatFormatting.GRAY));
            counts.entrySet().stream().sorted(Map.Entry.<io.github.jojiuchiha03.atzen.fish.FishSpecies, Integer>comparingByValue().reversed()).limit(8)
                    .forEach(entry -> serverPlayer.sendSystemMessage(Component.literal("  " + entry.getValue() + " × ")
                            .append(Component.translatable(entry.getKey().translationKey())).withStyle(ChatFormatting.GRAY)));
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 60);
            server.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 1.6f);
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * Bait box: right-click packs all bait from the inventory into it (up to nine stacks); sneak-right-click
     * empties it. When the rod's bait runs out, the box refills it with the same bait.
     */
    public static final class BaitBox extends Item {
        public static final int STACKS = 9;

        public BaitBox(Properties properties) { super(properties); }

        static List<ItemStack> contents(ItemStack box) {
            List<ItemStack> list = new ArrayList<>();
            ItemContainerContents contents = box.get(DataComponents.CONTAINER);
            if (contents != null) for (ItemStack stack : (Iterable<ItemStack>) contents.allItemsCopyStream()::iterator) if (!stack.isEmpty()) list.add(stack);
            return list;
        }

        static void store(ItemStack box, List<ItemStack> stacks) {
            if (stacks.isEmpty()) box.remove(DataComponents.CONTAINER);
            else box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            ItemStack box = player.getItemInHand(hand);
            List<ItemStack> stacks = contents(box);
            if (player.isShiftKeyDown()) {
                for (ItemStack stack : stacks) player.getInventory().placeItemBackInInventory(stack);
                store(box, List.of());
                player.sendSystemMessage(Component.translatable("message.atzen.bait_box_emptied"));
                return InteractionResult.SUCCESS;
            }
            NonNullList<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
            int moved = 0;
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (!BattleEquipment.isBait(stack)) continue;
                for (ItemStack stored : stacks) {
                    if (ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() < stored.getMaxStackSize()) {
                        int move = Math.min(stack.getCount(), stored.getMaxStackSize() - stored.getCount());
                        stored.grow(move);
                        stack.shrink(move);
                        moved += move;
                    }
                }
                if (!stack.isEmpty() && stacks.size() < STACKS) {
                    moved += stack.getCount();
                    stacks.add(stack.copy());
                    inventory.set(i, ItemStack.EMPTY);
                }
            }
            store(box, stacks);
            player.sendSystemMessage(Component.translatable("message.atzen.bait_box_filled", moved, stacks.size(), STACKS));
            return InteractionResult.SUCCESS;
        }

        /** Refills an empty bait slot from a box in the inventory: the same bait first, otherwise nothing. */
        public static void refill(Player player, ItemStack rod, BaitType wanted) {
            if (wanted == null || !AtzenRodItem.bait(rod).isEmpty()) return;
            for (ItemStack box : player.getInventory().getNonEquipmentItems()) {
                if (!(box.getItem() instanceof BaitBox)) continue;
                List<ItemStack> stacks = contents(box);
                for (int i = 0; i < stacks.size(); i++) {
                    if (AtzenItems.bait(stacks.get(i)) != wanted) continue;
                    AtzenRodItem.set(rod, GearSlot.BAIT, stacks.remove(i));
                    store(box, stacks);
                    player.sendOverlayMessage(Component.translatable("message.atzen.bait_refilled"));
                    return;
                }
            }
        }
    }

    /** Angler's tool: with a rod in the other hand, takes all its gear off at once into the inventory. */
    public static final class AnglerTool extends Item {
        public AnglerTool(Properties properties) { super(properties); }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack rod = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!(rod.getItem() instanceof AtzenRodItem) || player.fishing != null) {
                if (!level.isClientSide()) player.sendSystemMessage(Component.translatable("message.atzen.tool_instruction"));
                return InteractionResult.FAIL;
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            int parts = 0;
            for (GearSlot slot : GearSlot.values()) {
                ItemStack part = AtzenRodItem.get(rod, slot);
                if (part.isEmpty()) continue;
                AtzenRodItem.set(rod, slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(part);
                parts++;
            }
            player.getItemInHand(hand).hurtAndBreak(1, player, hand);
            player.sendOverlayMessage(Component.translatable("message.atzen.tool_stripped", parts));
            return InteractionResult.SUCCESS;
        }
    }

    /** The fish lexicon: species, orders, records, ranking and statistics. */
    public static final class Lexicon extends Item {
        public Lexicon(Properties properties) { super(properties); }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) Progression.openLexicon(serverPlayer);
            return InteractionResult.SUCCESS;
        }
    }

    /** Is a rod item (vanilla or Atzen). */
    public static boolean isRod(ItemStack stack) { return stack.getItem() instanceof FishingRodItem; }
}
