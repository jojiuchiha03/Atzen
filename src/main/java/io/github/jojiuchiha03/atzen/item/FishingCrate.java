package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/**
 * Crates come up instead of a fish now and then (as in Tide): opened they give what lies in that water – some of
 * it Atzen tackle, some of it a note on where a fish bites.
 */
public final class FishingCrate extends Item {
    /** Chance that a bite turns out to be a crate. */
    public static final double CHANCE = 0.06;

    public FishingCrate(Properties properties) { super(properties); }

    /** A crate from this water. */
    public static ItemStack create(Habitat habitat) {
        ItemStack crate = new ItemStack(AtzenItems.FISHING_CRATE);
        CompoundTag tag = new CompoundTag();
        tag.putString("habitat", habitat.name());
        crate.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        crate.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.atzen.fishing_crate"),
                Component.translatable("crate.atzen.from", Component.translatable(habitat.translationKey())).withStyle(ChatFormatting.GRAY))));
        return crate;
    }

    static Habitat habitat(ItemStack crate) {
        CustomData data = crate.get(DataComponents.CUSTOM_DATA);
        String name = data == null ? "" : data.copyTag().getStringOr("habitat", "");
        try {
            return Habitat.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Habitat.LAKE;
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        ItemStack crate = player.getItemInHand(hand);
        Random random = new Random(player.getRandom().nextLong());
        for (ItemStack stack : loot(habitat(crate), random)) player.getInventory().placeItemBackInInventory(stack);
        crate.consume(1, player);
        level.playSound(null, player.blockPosition(), SoundEvents.BARREL_OPEN, SoundSource.PLAYERS, 0.8f, 1.1f);
        serverPlayer.sendOverlayMessage(Component.translatable("crate.atzen.opened"));
        return InteractionResult.SUCCESS;
    }

    /** Two to four things from the water, a bait or a hook, and often a note. */
    public static List<ItemStack> loot(Habitat habitat, Random random) {
        List<ItemStack> loot = new ArrayList<>();
        Item[] water = switch (habitat) {
            case CAVE -> new Item[] {Items.AMETHYST_SHARD, Items.RAW_COPPER, Items.GLOW_BERRIES, Items.GLOW_INK_SAC, Items.POINTED_DRIPSTONE};
            case LAVA -> new Item[] {Items.MAGMA_CREAM, Items.QUARTZ, Items.GOLD_NUGGET, Items.BLAZE_POWDER, Items.GLOWSTONE_DUST};
            case END -> new Item[] {Items.CHORUS_FRUIT, Items.ENDER_PEARL, Items.POPPED_CHORUS_FRUIT, Items.END_ROD, Items.PURPUR_BLOCK};
            case OCEAN, WARM_OCEAN, COLD_OCEAN, DEEP_OCEAN -> new Item[] {Items.PRISMARINE_SHARD, Items.INK_SAC, Items.KELP, Items.SEA_PICKLE,
                    Items.NAUTILUS_SHELL};
            default -> new Item[] {Items.CLAY_BALL, Items.LILY_PAD, Items.STRING, Items.LEATHER, Items.IRON_NUGGET};
        };
        for (int i = 2 + random.nextInt(3); i > 0; i--) {
            Item item = water[random.nextInt(water.length)];
            loot.add(new ItemStack(item, item.getDefaultMaxStackSize() == 1 ? 1 : 1 + random.nextInt(4)));
        }
        if (random.nextDouble() < 0.6) {
            BaitType bait = BaitType.values()[random.nextInt(BaitType.values().length)];
            if (bait.boss() == null) loot.add(new ItemStack(AtzenItems.item(bait), bait.lure() ? 1 : 2 + random.nextInt(5)));
        } else {
            loot.add(new ItemStack(AtzenItems.HOOKS.get(random.nextInt(AtzenItems.HOOKS.size())), 1 + random.nextInt(3)));
        }
        if (random.nextDouble() < 0.12) loot.add(new ItemStack(Items.EMERALD, 1 + random.nextInt(3)));
        if (random.nextDouble() < 0.05) loot.add(new ItemStack(AtzenItems.MOTHER_OF_PEARL));
        if (habitat == Habitat.LAVA && random.nextDouble() < 0.03) loot.add(new ItemStack(Items.NETHERITE_SCRAP));
        if (habitat == Habitat.END && random.nextDouble() < 0.05) loot.add(new ItemStack(Items.SHULKER_SHELL));
        if (random.nextDouble() < 0.4) loot.add(FishyNote.create(random));
        return loot;
    }

    /**
     * A note on one fish: where it lives, when it feeds and what it takes. Rarer fish are written about more often –
     * nobody leaves notes about roach.
     */
    public static final class FishyNote extends Item {
        public FishyNote(Properties properties) { super(properties); }

        public static ItemStack create(Random random) {
            List<FishSpecies> candidates = new ArrayList<>();
            for (FishSpecies species : FishSpecies.values()) {
                if (species.boss() != null || species.habitats().isEmpty()) continue;
                for (int i = 0; i <= species.rarity().ordinal(); i++) candidates.add(species);
            }
            FishSpecies species = candidates.get(random.nextInt(candidates.size()));
            ItemStack note = new ItemStack(AtzenItems.FISHY_NOTE);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("note.atzen.fish", Component.translatable(species.translationKey())
                    .withStyle(io.github.jojiuchiha03.atzen.fish.FishCatch.colour(species.rarity()))));
            MutableComponent where = Component.empty();
            for (Habitat habitat : species.habitats()) {
                if (!where.getSiblings().isEmpty()) where.append(", ");
                where.append(Component.translatable(habitat.translationKey()));
            }
            lines.add(Component.translatable("note.atzen.where", where).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("note.atzen.when." + species.feedingTime()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("note.atzen.bait", Component.translatable(bestBait(species).translationKey()))
                    .withStyle(ChatFormatting.GRAY));
            if (species.lava()) lines.add(Component.translatable("note.atzen.lava").withStyle(ChatFormatting.GOLD));
            note.set(DataComponents.LORE, new ItemLore(lines));
            return note;
        }

        /** The bait this fish likes best. */
        static BaitType bestBait(FishSpecies species) {
            BaitType best = BaitType.WORM;
            for (BaitType bait : BaitType.values()) {
                if (bait.boss() == null && bait.appeal(species) > best.appeal(species)) best = bait;
            }
            return best;
        }
    }
}
