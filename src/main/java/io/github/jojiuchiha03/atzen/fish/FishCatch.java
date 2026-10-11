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
import net.minecraft.world.level.biome.Biomes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FishCatch {
    private FishCatch() { }

    public static boolean isSwamp(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    public static ItemStack create(FishSpecimen fish, ServerLevel level, BlockPos pos, int quality, String angler) {
        ItemStack stack = new ItemStack(AtzenFishRegistry.fishItem(fish.species()));
        CompoundTag data = measurements(fish);
        data.putInt("quality", quality);
        data.putString("angler", angler);
        data.putString("biome", level.getBiome(pos).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown"));
        data.putLong("day", level.getOverworldClockTime() / 24000 + 1);
        data.putInt("x", pos.getX());
        data.putInt("z", pos.getZ());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("atzen_fish", data));

        List<Component> lines = new ArrayList<>(measurementLore(fish));
        lines.add(Component.translatable("catch.atzen.location", Component.translatable(isSwamp(level, pos)
                ? "habitat.atzen.swamp" : "habitat.atzen.freshwater"), pos.getX(), pos.getZ()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("catch.atzen.day", level.getOverworldClockTime() / 24000 + 1).withStyle(ChatFormatting.GRAY));
        lines.add(quality < 0 ? Component.translatable("catch.atzen.wild").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("catch.atzen.quality", quality, angler).withStyle(ChatFormatting.AQUA));
        stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }

    public static CompoundTag measurements(FishSpecimen fish) {
        CompoundTag data = new CompoundTag();
        data.putString("species", fish.species().id());
        data.putDouble("length_cm", fish.lengthCm());
        data.putDouble("weight_kg", fish.weightKg());
        return data;
    }

    public static List<Component> measurementLore(FishSpecimen fish) {
        return List.of(
                Component.translatable(fish.species().rarityKey()).append(" · ")
                        .append(Component.translatable(fish.sizeKey())).withStyle(fish.species() == FishSpecies.GOLDEN_KOI
                                ? ChatFormatting.GOLD : ChatFormatting.AQUA),
                Component.translatable("catch.atzen.measurements", String.format(Locale.ROOT, "%.1f", fish.lengthCm()),
                        String.format(Locale.ROOT, "%.2f", fish.weightKg())).withStyle(ChatFormatting.GRAY));
    }
}
