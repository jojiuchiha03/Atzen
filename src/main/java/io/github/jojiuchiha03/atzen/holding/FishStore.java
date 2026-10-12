package io.github.jojiuchiha03.atzen.holding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import net.minecraft.core.UUIDUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The fish kept in a keepnet: limited in number, total weight and size. Every fish keeps its identity (an id, its
 * measurements, its condition); the same fish can never be stored twice, so nothing is duplicated when fish move
 * between the world, the hand and the net.
 */
public final class FishStore {
    /** One kept fish. Condition 0..100; at 0 it is dead. */
    public record Kept(UUID id, FishSpecies species, double lengthCm, double weightKg, double condition, int quality, String angler) {
        public static final Codec<FishSpecies> SPECIES = Codec.STRING.comapFlatMap(id -> {
            for (FishSpecies species : FishSpecies.values()) if (species.id().equals(id)) return DataResult.success(species);
            return DataResult.error(() -> "Unknown fish " + id);
        }, FishSpecies::id);
        public static final Codec<Kept> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Kept::id),
                SPECIES.fieldOf("species").forGetter(Kept::species),
                Codec.DOUBLE.fieldOf("length").forGetter(Kept::lengthCm),
                Codec.DOUBLE.fieldOf("weight").forGetter(Kept::weightKg),
                Codec.DOUBLE.fieldOf("condition").forGetter(Kept::condition),
                Codec.INT.optionalFieldOf("quality", -1).forGetter(Kept::quality),
                Codec.STRING.optionalFieldOf("angler", "").forGetter(Kept::angler)).apply(instance, Kept::new));

        public Kept withCondition(double value) {
            return new Kept(id, species, lengthCm, weightKg, Math.clamp(value, 0, 100), quality, angler);
        }
    }

    public enum Add { OK, FULL, TOO_HEAVY, TOO_BIG, DUPLICATE, DEAD }

    private final int capacity;
    private final double maxWeight, maxLength;
    private final List<Kept> fish = new ArrayList<>();

    public FishStore(int capacity, double maxWeight, double maxLength) {
        this.capacity = capacity;
        this.maxWeight = maxWeight;
        this.maxLength = maxLength;
    }

    public Add add(Kept kept) {
        if (kept.condition() <= 0) return Add.DEAD;
        if (fish.stream().anyMatch(other -> other.id().equals(kept.id()))) return Add.DUPLICATE;
        if (fish.size() >= capacity) return Add.FULL;
        if (kept.lengthCm() > maxLength) return Add.TOO_BIG;
        if (totalWeight() + kept.weightKg() > maxWeight) return Add.TOO_HEAVY;
        fish.add(kept);
        return Add.OK;
    }

    public Kept takeLast() { return fish.isEmpty() ? null : fish.removeLast(); }

    public List<Kept> takeAll() {
        List<Kept> all = List.copyOf(fish);
        fish.clear();
        return all;
    }

    public List<Kept> fish() { return Collections.unmodifiableList(fish); }
    public int size() { return fish.size(); }
    public int capacity() { return capacity; }
    public double maxWeight() { return maxWeight; }
    public double totalWeight() { return fish.stream().mapToDouble(Kept::weightKg).sum(); }
    public double density() { return Math.max(fish.size() / (double) capacity, totalWeight() / maxWeight); }

    public double averageCondition() {
        return fish.stream().mapToDouble(Kept::condition).average().orElse(100);
    }

    /** Lets time pass in this water; returns the fish that died. */
    public List<Kept> age(WaterConditions water, double minutes) {
        List<Kept> dead = new ArrayList<>();
        double change = water.perMinute() * minutes;
        for (int i = fish.size() - 1; i >= 0; i--) {
            Kept kept = fish.get(i).withCondition(fish.get(i).condition() + change);
            if (kept.condition() <= 0) dead.add(fish.remove(i));
            else fish.set(i, kept);
        }
        return dead;
    }

    /** Restores stored fish (from the block entity's save data); skips duplicates. */
    public void restore(Kept kept) {
        if (fish.stream().noneMatch(other -> other.id().equals(kept.id()))) fish.add(kept);
    }
}
