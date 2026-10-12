package io.github.jojiuchiha03.atzen.fish;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FishCatch {
    private FishCatch() { }


    public static ItemStack create(FishSpecimen fish, ServerLevel level, BlockPos pos, int quality, String angler) {
        return create(fish, level, pos, quality, angler, UUID.randomUUID(), 100);
    }

    /** A caught fish as an item: measurements, where and when, by whom, its identity and its condition (0..100). */
    public static ItemStack create(FishSpecimen fish, ServerLevel level, BlockPos pos, int quality, String angler, UUID id,
                                   double condition) {
        ItemStack stack = new ItemStack(AtzenFishRegistry.fishItem(fish.species()));
        CompoundTag data = measurements(fish);
        data.putString("id", id.toString());
        data.putDouble("condition", Math.clamp(condition, 0, 100));
        data.putInt("quality", quality);
        data.putString("angler", angler);
        data.putString("biome", level.getBiome(pos).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown"));
        data.putLong("day", level.getOverworldClockTime() / 24000 + 1);
        data.putInt("x", pos.getX());
        data.putInt("z", pos.getZ());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("atzen_fish", data));

        List<Component> lines = new ArrayList<>(measurementLore(fish));
        lines.add(Component.translatable("catch.atzen.location", Component.translatable(Habitat.at(level, pos).translationKey()), pos.getX(), pos.getZ()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("catch.atzen.day", level.getOverworldClockTime() / 24000 + 1).withStyle(ChatFormatting.GRAY));
        lines.add(quality < 0 ? Component.translatable("catch.atzen.wild").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("catch.atzen.quality", quality, angler).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("catch.atzen.condition", Math.round(condition)).withStyle(
                condition >= 60 ? ChatFormatting.GREEN : condition >= 25 ? ChatFormatting.YELLOW : ChatFormatting.RED));
        stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }

    /** A caught fish read back from its item: specimen, identity, condition, control and angler. */
    public record Catch(FishSpecimen specimen, UUID id, double condition, int quality, String angler) { }

    /** Reads an Atzen fish item; null for anything else. Older items without identity get a fresh one. */
    public static Catch read(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        CompoundTag data = custom.copyTag().getCompoundOrEmpty("atzen_fish");
        if (data.isEmpty()) return null;
        FishSpecies species = null;
        for (FishSpecies candidate : FishSpecies.values()) if (candidate.id().equals(data.getStringOr("species", ""))) species = candidate;
        if (species == null) return null;
        double length = Math.clamp(data.getDoubleOr("length_cm", species.minLength()), species.minLength(), species.maxLength());
        double weight = data.getDoubleOr("weight_kg", 1);
        if (!(weight > 0) || !Double.isFinite(weight)) return null;
        UUID id;
        try {
            id = UUID.fromString(data.getStringOr("id", ""));
        } catch (IllegalArgumentException e) {
            id = UUID.nameUUIDFromBytes((species.id() + length + weight + data.getLongOr("day", 0)).getBytes());
        }
        return new Catch(new FishSpecimen(species, length, weight), id, data.getDoubleOr("condition", 100),
                data.getIntOr("quality", -1), data.getStringOr("angler", ""));
    }

    public static CompoundTag measurements(FishSpecimen fish) {
        CompoundTag data = new CompoundTag();
        data.putString("species", fish.species().id());
        data.putDouble("length_cm", fish.lengthCm());
        data.putDouble("weight_kg", fish.weightKg());
        return data;
    }

    public static ChatFormatting colour(FishSpecies.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
            case MYTHIC -> ChatFormatting.RED;
        };
    }

    public static List<Component> measurementLore(FishSpecimen fish) {
        return List.of(
                Component.translatable(fish.species().rarityKey()).append(" · ")
                        .append(Component.translatable(fish.sizeKey())).withStyle(colour(fish.species().rarity())),
                Component.translatable("catch.atzen.measurements", String.format(Locale.ROOT, "%.1f", fish.lengthCm()),
                        String.format(Locale.ROOT, "%.2f", fish.weightKg())).withStyle(ChatFormatting.GRAY));
    }
}
