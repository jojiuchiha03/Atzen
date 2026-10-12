package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.battle.FishStyle;
import java.util.Locale;
import java.util.Random;

/**
 * The fish of Atzen: the freshwater fish of central Europe (salmonids, predators, coarse fish), the
 * edible sea fish of the North Atlantic and the Mediterranean, and the golden koi. Each has a size range,
 * a body shape for the 3D model, a fighting style and how common it is in each {@link Habitat}. A weight
 * of 0 means it does not live there.
 */
public enum FishSpecies {
    // weights per habitat: river, lake, swamp, cold, ocean, warm ocean, cold ocean, deep ocean
    // salmonids
    RIVER_TROUT(18, 80, 0.000014, FishStyle.DARTER, Rarity.COMMON, 2.0f, 3.5f, 8f, 1f, 4f,
            new int[] {8, 0, 0, 14, 0, 0, 0, 0}),
    RAINBOW_TROUT(20, 80, 0.0000115, FishStyle.DARTER, Rarity.COMMON, 2.0f, 3.5f, 8f, 1f, 4f,
            new int[] {7, 4, 0, 6, 0, 0, 0, 0}),
    GRAYLING(20, 55, 0.00001, FishStyle.DARTER, Rarity.COMMON, 1.8f, 3.2f, 8f, 1f, 4f,
            new int[] {4, 0, 0, 8, 0, 0, 0, 0}),
    SALMON(50, 150, 0.0000105, FishStyle.FIGHTER, Rarity.RARE, 2.4f, 4.0f, 10f, 1.4f, 4.5f,
            new int[] {4, 0, 0, 3, 2, 0, 4, 0}),
    SEA_TROUT(40, 110, 0.0000105, FishStyle.FIGHTER, Rarity.RARE, 2.2f, 3.8f, 9.5f, 1.2f, 4.5f,
            new int[] {4, 0, 0, 2, 3, 0, 3, 0}),
    // freshwater predators
    PIKE(35, 150, 0.000008, FishStyle.FIGHTER, Rarity.RARE, 1.6f, 2.8f, 10f, 2.5f, 4f,
            new int[] {5, 8, 8, 3, 0, 0, 0, 0}),
    ZANDER(30, 100, 0.0000085, FishStyle.FIGHTER, Rarity.COMMON, 1.8f, 3.2f, 9f, 1.5f, 4f,
            new int[] {6, 6, 2, 0, 0, 0, 0, 0}),
    PERCH(12, 55, 0.000018, FishStyle.FIGHTER, Rarity.COMMON, 2.0f, 4.8f, 6.5f, 1f, 4f,
            new int[] {10, 14, 10, 6, 0, 0, 0, 0}),
    CATFISH(40, 220, 0.000009, FishStyle.BRUISER, Rarity.RARE, 3.0f, 2.8f, 9f, 1f, 4f,
            new int[] {3, 2, 8, 0, 0, 0, 0, 0}),
    EEL(40, 130, 0.0000016, FishStyle.DARTER, Rarity.COMMON, 1.2f, 1.6f, 13f, 1f, 3f,
            new int[] {5, 3, 8, 0, 1, 0, 0, 0}),
    ASP(30, 90, 0.0000105, FishStyle.DARTER, Rarity.COMMON, 1.8f, 3.4f, 9f, 1.2f, 4f,
            new int[] {6, 1, 0, 0, 0, 0, 0, 0}),
    // coarse fish
    CARP(25, 110, 0.000026, FishStyle.BRUISER, Rarity.COMMON, 3.0f, 4.8f, 6.5f, 1f, 4f,
            new int[] {3, 12, 14, 0, 0, 0, 0, 0}),
    BARBEL(25, 90, 0.0000105, FishStyle.FIGHTER, Rarity.COMMON, 2.0f, 3.0f, 9f, 1.5f, 4f,
            new int[] {7, 0, 0, 2, 0, 0, 0, 0}),
    ROACH(10, 45, 0.000015, FishStyle.DARTER, Rarity.COMMON, 1.6f, 3.6f, 6f, 1f, 3.5f,
            new int[] {12, 14, 10, 4, 0, 0, 0, 0}),
    RUDD(10, 45, 0.000016, FishStyle.DARTER, Rarity.COMMON, 1.6f, 3.8f, 6f, 1f, 3.5f,
            new int[] {6, 10, 10, 0, 0, 0, 0, 0}),
    BREAM(25, 75, 0.000017, FishStyle.BRUISER, Rarity.COMMON, 1.4f, 5.2f, 7f, 1f, 4f,
            new int[] {6, 12, 6, 0, 0, 0, 0, 0}),
    TENCH(20, 65, 0.000016, FishStyle.BRUISER, Rarity.COMMON, 2.2f, 3.6f, 7f, 1f, 4f,
            new int[] {2, 6, 12, 0, 0, 0, 0, 0}),
    CHUB(20, 70, 0.000013, FishStyle.FIGHTER, Rarity.COMMON, 2.2f, 3.4f, 8f, 1f, 4f,
            new int[] {8, 3, 0, 2, 0, 0, 0, 0}),
    // cod family
    ATLANTIC_COD(30, 150, 0.0000105, FishStyle.FIGHTER, Rarity.COMMON, 2.4f, 4.0f, 9f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 8, 0, 12, 6}),
    SAITHE(30, 120, 0.0000095, FishStyle.FIGHTER, Rarity.COMMON, 2.2f, 3.6f, 9f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 6, 0, 8, 4}),
    ALASKA_POLLOCK(25, 90, 0.0000085, FishStyle.DARTER, Rarity.COMMON, 2.0f, 3.4f, 8.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 3, 0, 10, 3}),
    HADDOCK(25, 90, 0.0000095, FishStyle.DARTER, Rarity.COMMON, 2.2f, 3.6f, 8.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 6, 0, 8, 3}),
    HAKE(30, 130, 0.0000075, FishStyle.FIGHTER, Rarity.COMMON, 1.8f, 3.0f, 10f, 1.6f, 4f,
            new int[] {0, 0, 0, 0, 5, 2, 3, 6}),
    WHITING(20, 60, 0.0000085, FishStyle.DARTER, Rarity.COMMON, 1.8f, 3.0f, 8f, 1.2f, 3.5f,
            new int[] {0, 0, 0, 0, 7, 1, 4, 0}),
    // herring family
    HERRING(18, 40, 0.0000085, FishStyle.DARTER, Rarity.COMMON, 1.4f, 2.8f, 7f, 1f, 3.5f,
            new int[] {0, 0, 0, 0, 12, 0, 14, 0}),
    SARDINE(12, 25, 0.0000095, FishStyle.DARTER, Rarity.COMMON, 1.2f, 2.4f, 6f, 1f, 3f,
            new int[] {0, 0, 0, 0, 6, 14, 0, 0}),
    ANCHOVY(9, 20, 0.0000065, FishStyle.DARTER, Rarity.COMMON, 1.0f, 1.8f, 6f, 1f, 3f,
            new int[] {0, 0, 0, 0, 6, 14, 0, 0}),
    SPRAT(8, 18, 0.0000075, FishStyle.DARTER, Rarity.COMMON, 1.0f, 2.0f, 5f, 0.8f, 3f,
            new int[] {0, 0, 0, 0, 10, 2, 8, 0}),
    // perch-like sea fish
    SEA_BASS(25, 100, 0.000012, FishStyle.FIGHTER, Rarity.COMMON, 2.0f, 3.8f, 8.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 6, 8, 0, 0}),
    GILTHEAD_BREAM(20, 70, 0.000017, FishStyle.FIGHTER, Rarity.COMMON, 1.6f, 4.8f, 6.5f, 1f, 4f,
            new int[] {0, 0, 0, 0, 3, 10, 0, 0}),
    RED_SNAPPER(30, 100, 0.000014, FishStyle.FIGHTER, Rarity.COMMON, 2.2f, 4.6f, 7.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 0, 8, 0, 2}),
    RED_MULLET(12, 40, 0.000013, FishStyle.DARTER, Rarity.COMMON, 1.8f, 3.2f, 7f, 1.2f, 3.5f,
            new int[] {0, 0, 0, 0, 3, 8, 0, 0}),
    GROUPER(40, 200, 0.000014, FishStyle.BRUISER, Rarity.RARE, 3.4f, 5.0f, 9f, 1.6f, 4f,
            new int[] {0, 0, 0, 0, 0, 3, 0, 2}),
    BARRACUDA(50, 180, 0.0000045, FishStyle.DARTER, Rarity.RARE, 1.6f, 2.6f, 13f, 2.5f, 4f,
            new int[] {0, 0, 0, 0, 0, 3, 0, 1}),
    // mackerels and tunas
    MACKEREL(20, 55, 0.0000085, FishStyle.DARTER, Rarity.COMMON, 1.8f, 3.2f, 8f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 12, 8, 6, 0}),
    TUNA(80, 300, 0.000015, FishStyle.BRUISER, Rarity.RARE, 4.0f, 5.6f, 10f, 1.6f, 5f,
            new int[] {0, 0, 0, 0, 1, 2, 1, 3}),
    SKIPJACK(40, 100, 0.000013, FishStyle.FIGHTER, Rarity.COMMON, 2.8f, 4.2f, 8f, 1.2f, 4.5f,
            new int[] {0, 0, 0, 0, 2, 6, 0, 2}),
    SWORDFISH(100, 400, 0.0000035, FishStyle.FIGHTER, Rarity.LEGENDARY, 2.6f, 4.4f, 11f, 6f, 5f,
            new int[] {0, 0, 0, 0, 0, 1, 0, 1}),
    // flatfish
    PLAICE(20, 70, 0.0000115, FishStyle.BRUISER, Rarity.COMMON, 5.5f, 1.0f, 7f, 0.5f, 3f,
            new int[] {0, 0, 0, 0, 7, 1, 6, 0}),
    FLOUNDER(20, 55, 0.000012, FishStyle.BRUISER, Rarity.COMMON, 5.0f, 1.0f, 6.5f, 0.5f, 3f,
            new int[] {1, 0, 0, 0, 6, 1, 5, 0}),
    SOLE(20, 60, 0.0000095, FishStyle.BRUISER, Rarity.COMMON, 4.0f, 0.8f, 8f, 0.5f, 2.5f,
            new int[] {0, 0, 0, 0, 5, 4, 1, 0}),
    TURBOT(30, 100, 0.000014, FishStyle.BRUISER, Rarity.RARE, 6.5f, 1.2f, 7f, 0.5f, 3f,
            new int[] {0, 0, 0, 0, 3, 2, 2, 1}),
    HALIBUT(60, 250, 0.0000105, FishStyle.BRUISER, Rarity.RARE, 6.0f, 1.4f, 10f, 0.8f, 3.5f,
            new int[] {0, 0, 0, 0, 0, 0, 4, 3}),
    // other sea fish
    REDFISH(25, 90, 0.000014, FishStyle.FIGHTER, Rarity.COMMON, 2.4f, 4.4f, 7.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 0, 0, 6, 5}),
    MONKFISH(30, 180, 0.000014, FishStyle.BRUISER, Rarity.RARE, 6.0f, 2.4f, 7f, 0.5f, 3.5f,
            new int[] {0, 0, 0, 0, 2, 1, 2, 4}),
    GURNARD(20, 60, 0.0000095, FishStyle.FIGHTER, Rarity.COMMON, 2.2f, 3.2f, 8f, 1.6f, 3.5f,
            new int[] {0, 0, 0, 0, 4, 4, 2, 1}),
    WOLFFISH(40, 150, 0.000009, FishStyle.BRUISER, Rarity.RARE, 2.4f, 3.6f, 11f, 1.6f, 3.5f,
            new int[] {0, 0, 0, 0, 0, 0, 3, 2}),
    // the special one
    GOLDEN_KOI(15, 75, 0.000019, FishStyle.DARTER, Rarity.LEGENDARY, 2.0f, 3.5f, 6.5f, 1f, 5f,
            new int[] {1, 1, 1, 0, 0, 0, 0, 0});

    public enum Rarity { COMMON, RARE, LEGENDARY }

    private final double minLength, maxLength, weightFactor;
    private final FishStyle style;
    private final Rarity rarity;
    private final float width, height, body, nose, tail;
    private final int[] habitatWeights;

    FishSpecies(double minLength, double maxLength, double weightFactor, FishStyle style, Rarity rarity,
                float width, float height, float body, float nose, float tail, int[] habitatWeights) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.weightFactor = weightFactor;
        this.style = style;
        this.rarity = rarity;
        this.width = width;
        this.height = height;
        this.body = body;
        this.nose = nose;
        this.tail = tail;
        if (habitatWeights.length != Habitat.values().length) throw new IllegalArgumentException(name());
        this.habitatWeights = habitatWeights;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "entity.atzen." + id(); }
    public double minLength() { return minLength; }
    public double maxLength() { return maxLength; }
    public FishStyle style() { return style; }
    public Rarity rarity() { return rarity; }
    public float modelWidth() { return width; }
    public float modelHeight() { return height; }
    public float modelBodyLength() { return body; }
    public float modelNoseLength() { return nose; }
    public float modelTailLength() { return tail; }
    /** Barbels on the snout: the catfish, the barbel and the cod family's chin barbel. */
    public boolean hasBarbels() { return this == CATFISH || this == BARBEL || this == ATLANTIC_COD || this == HADDOCK; }
    /** Flatfish lie on their side: a wide, low body. */
    public boolean flat() { return width > height * 2; }
    public double modelLengthCm() { return (modelBodyLength() + 3 + modelNoseLength() + modelTailLength()) * 100.0 / 16; }
    public String rarityKey() { return "rarity.atzen." + rarity.name().toLowerCase(Locale.ROOT); }
    /** Spawn and bite weight in a habitat; 0 where the fish does not live. */
    public int weight(Habitat habitat) { return habitatWeights[habitat.ordinal()]; }
    /** Shoal fish swim in groups, the big predators and the rare ones alone. */
    public int maxGroup() {
        if (rarity != Rarity.COMMON || this == ZANDER || this == EEL || flat()) return 1;
        return switch (this) {
            case HERRING, SARDINE, ANCHOVY, SPRAT, MACKEREL -> 8;
            case ROACH, RUDD, BREAM -> 6;
            default -> 4;
        };
    }

    public FishSpecimen roll(Random random) {
        double fraction = Math.pow(random.nextDouble(), 2);
        double length = Math.round((minLength + (maxLength - minLength) * fraction) * 10) / 10.0;
        double weight = Math.max(0.01, Math.round(weightFactor * Math.pow(length, 3)
                * (0.88 + random.nextDouble() * 0.24) * 100) / 100.0);
        return new FishSpecimen(this, length, weight);
    }

    /** Which fish takes a bait in this habitat, weighted by how common each is there. */
    public static FishSpecies choose(Habitat habitat, Random random) {
        int total = 0;
        for (FishSpecies species : values()) total += species.weight(habitat);
        if (total == 0) return choose(Habitat.RIVER, random);
        int choice = random.nextInt(total);
        for (FishSpecies species : values()) {
            choice -= species.weight(habitat);
            if (choice < 0) return species;
        }
        return PERCH;
    }

    public static FishSpecies choose(boolean swamp, Random random) {
        return choose(swamp ? Habitat.SWAMP : Habitat.RIVER, random);
    }
}
