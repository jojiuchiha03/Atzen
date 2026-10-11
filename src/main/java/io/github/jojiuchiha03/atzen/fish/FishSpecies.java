package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.battle.FishStyle;
import java.util.Locale;
import java.util.Random;

public enum FishSpecies {
    RIVER_TROUT(18, 80, 0.000014, FishStyle.DARTER, 30, 8),
    PERCH(12, 55, 0.000018, FishStyle.FIGHTER, 35, 22),
    PIKE(35, 150, 0.000008, FishStyle.FIGHTER, 12, 15),
    CARP(25, 110, 0.000026, FishStyle.BRUISER, 16, 30),
    CATFISH(40, 220, 0.000009, FishStyle.BRUISER, 6, 24),
    GOLDEN_KOI(15, 75, 0.000019, FishStyle.DARTER, 1, 1);

    private final double minLength, maxLength, weightFactor;
    private final FishStyle style;
    private final int riverWeight, swampWeight;

    FishSpecies(double minLength, double maxLength, double weightFactor, FishStyle style, int riverWeight, int swampWeight) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.weightFactor = weightFactor;
        this.style = style;
        this.riverWeight = riverWeight;
        this.swampWeight = swampWeight;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "entity.atzen." + id(); }
    public double minLength() { return minLength; }
    public double maxLength() { return maxLength; }
    public FishStyle style() { return style; }
    public float modelWidth() { return switch (this) { case PIKE -> 1.6f; case CARP, CATFISH -> 3; default -> 2; }; }
    public float modelHeight() { return switch (this) { case CARP, PERCH -> 4.8f; case PIKE, CATFISH -> 2.8f; default -> 3.5f; }; }
    public float modelBodyLength() { return switch (this) { case PIKE -> 10; case CATFISH -> 9; case RIVER_TROUT -> 8; default -> 6.5f; }; }
    public float modelNoseLength() { return this == PIKE ? 2.5f : 1; }
    public float modelTailLength() { return this == GOLDEN_KOI ? 5 : 4; }
    public double modelLengthCm() { return (modelBodyLength() + 3 + modelNoseLength() + modelTailLength()) * 100.0 / 16; }
    public String rarityKey() {
        return "rarity.atzen." + (this == GOLDEN_KOI ? "legendary" : this == PIKE || this == CATFISH ? "rare" : "common");
    }

    public FishSpecimen roll(Random random) {
        double fraction = Math.pow(random.nextDouble(), 2);
        double length = Math.round((minLength + (maxLength - minLength) * fraction) * 10) / 10.0;
        double weight = Math.max(0.01, Math.round(weightFactor * Math.pow(length, 3)
                * (0.88 + random.nextDouble() * 0.24) * 100) / 100.0);
        return new FishSpecimen(this, length, weight);
    }

    public static FishSpecies choose(boolean swamp, Random random) {
        int total = 0;
        for (FishSpecies species : values()) total += swamp ? species.swampWeight : species.riverWeight;
        int choice = random.nextInt(total);
        for (FishSpecies species : values()) {
            choice -= swamp ? species.swampWeight : species.riverWeight;
            if (choice < 0) return species;
        }
        return PERCH;
    }
}
