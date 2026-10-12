package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.battle.Boss;
import io.github.jojiuchiha03.atzen.battle.Fighter;
import io.github.jojiuchiha03.atzen.battle.FishStyle;
import java.util.Locale;
import java.util.Random;
import java.util.function.ToDoubleFunction;

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
    RAINBOW_TROUT(20, 80, 0.0000115, FishStyle.DARTER, Rarity.UNCOMMON, 2.0f, 3.5f, 8f, 1f, 4f,
            new int[] {7, 4, 0, 6, 0, 0, 0, 0}),
    GRAYLING(20, 55, 0.00001, FishStyle.DARTER, Rarity.UNCOMMON, 1.8f, 3.2f, 8f, 1f, 4f,
            new int[] {4, 0, 0, 8, 0, 0, 0, 0}),
    SALMON(50, 150, 0.0000105, FishStyle.FIGHTER, Rarity.RARE, 2.4f, 4.0f, 10f, 1.4f, 4.5f,
            new int[] {4, 0, 0, 3, 2, 0, 4, 0}),
    SEA_TROUT(40, 110, 0.0000105, FishStyle.FIGHTER, Rarity.RARE, 2.2f, 3.8f, 9.5f, 1.2f, 4.5f,
            new int[] {4, 0, 0, 2, 3, 0, 3, 0}),
    // freshwater predators
    PIKE(35, 150, 0.000008, FishStyle.FIGHTER, Rarity.UNCOMMON, 1.6f, 2.8f, 10f, 2.5f, 4f,
            new int[] {5, 8, 8, 3, 0, 0, 0, 0}),
    ZANDER(30, 100, 0.0000085, FishStyle.FIGHTER, Rarity.RARE, 1.8f, 3.2f, 9f, 1.5f, 4f,
            new int[] {6, 6, 2, 0, 0, 0, 0, 0}),
    PERCH(12, 55, 0.000018, FishStyle.FIGHTER, Rarity.COMMON, 2.0f, 4.8f, 6.5f, 1f, 4f,
            new int[] {10, 14, 10, 6, 0, 0, 0, 0}),
    CATFISH(40, 220, 0.000009, FishStyle.BRUISER, Rarity.RARE, 3.0f, 2.8f, 9f, 1f, 4f,
            new int[] {3, 2, 8, 0, 0, 0, 0, 0}),
    EEL(40, 130, 0.0000016, FishStyle.DARTER, Rarity.RARE, 1.2f, 1.6f, 13f, 1f, 3f,
            new int[] {5, 3, 8, 0, 1, 0, 0, 0}),
    ASP(30, 90, 0.0000105, FishStyle.DARTER, Rarity.UNCOMMON, 1.8f, 3.4f, 9f, 1.2f, 4f,
            new int[] {6, 1, 0, 0, 0, 0, 0, 0}),
    // coarse fish
    CARP(25, 110, 0.000026, FishStyle.BRUISER, Rarity.UNCOMMON, 3.0f, 4.8f, 6.5f, 1f, 4f,
            new int[] {3, 12, 14, 0, 0, 0, 0, 0}),
    BARBEL(25, 90, 0.0000105, FishStyle.FIGHTER, Rarity.UNCOMMON, 2.0f, 3.0f, 9f, 1.5f, 4f,
            new int[] {7, 0, 0, 2, 0, 0, 0, 0}),
    ROACH(10, 45, 0.000015, FishStyle.DARTER, Rarity.COMMON, 1.6f, 3.6f, 6f, 1f, 3.5f,
            new int[] {12, 14, 10, 4, 0, 0, 0, 0}),
    RUDD(10, 45, 0.000016, FishStyle.DARTER, Rarity.COMMON, 1.6f, 3.8f, 6f, 1f, 3.5f,
            new int[] {6, 10, 10, 0, 0, 0, 0, 0}),
    BREAM(25, 75, 0.000017, FishStyle.BRUISER, Rarity.COMMON, 1.4f, 5.2f, 7f, 1f, 4f,
            new int[] {6, 12, 6, 0, 0, 0, 0, 0}),
    TENCH(20, 65, 0.000016, FishStyle.BRUISER, Rarity.UNCOMMON, 2.2f, 3.6f, 7f, 1f, 4f,
            new int[] {2, 6, 12, 0, 0, 0, 0, 0}),
    CRUCIAN_CARP(10, 45, 0.00003, FishStyle.BRUISER, Rarity.COMMON, 2.6f, 4.6f, 6f, 1f, 3.5f,
            new int[] {1, 10, 12, 0, 0, 0, 0, 0}),
    STURGEON(80, 250, 0.0000045, FishStyle.FIGHTER, Rarity.EPIC, 2.4f, 2.4f, 13f, 3f, 5f,
            new int[] {2, 0, 0, 1, 0, 0, 0, 0}),
    CHUB(20, 70, 0.000013, FishStyle.FIGHTER, Rarity.COMMON, 2.2f, 3.4f, 8f, 1f, 4f,
            new int[] {8, 3, 0, 2, 0, 0, 0, 0}),
    // cod family
    ATLANTIC_COD(30, 150, 0.0000105, FishStyle.FIGHTER, Rarity.COMMON, 2.4f, 4.0f, 9f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 8, 0, 12, 6}),
    SAITHE(30, 120, 0.0000095, FishStyle.FIGHTER, Rarity.UNCOMMON, 2.2f, 3.6f, 9f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 6, 0, 8, 4}),
    ALASKA_POLLOCK(25, 90, 0.0000085, FishStyle.DARTER, Rarity.COMMON, 2.0f, 3.4f, 8.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 3, 0, 10, 3}),
    HADDOCK(25, 90, 0.0000095, FishStyle.DARTER, Rarity.UNCOMMON, 2.2f, 3.6f, 8.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 6, 0, 8, 3}),
    HAKE(30, 130, 0.0000075, FishStyle.FIGHTER, Rarity.UNCOMMON, 1.8f, 3.0f, 10f, 1.6f, 4f,
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
    SEA_BASS(25, 100, 0.000012, FishStyle.FIGHTER, Rarity.UNCOMMON, 2.0f, 3.8f, 8.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 6, 8, 0, 0}),
    GILTHEAD_BREAM(20, 70, 0.000017, FishStyle.FIGHTER, Rarity.UNCOMMON, 1.6f, 4.8f, 6.5f, 1f, 4f,
            new int[] {0, 0, 0, 0, 3, 10, 0, 0}),
    RED_SNAPPER(30, 100, 0.000014, FishStyle.FIGHTER, Rarity.RARE, 2.2f, 4.6f, 7.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 0, 8, 0, 2}),
    RED_MULLET(12, 40, 0.000013, FishStyle.DARTER, Rarity.UNCOMMON, 1.8f, 3.2f, 7f, 1.2f, 3.5f,
            new int[] {0, 0, 0, 0, 3, 8, 0, 0}),
    GROUPER(40, 200, 0.000014, FishStyle.BRUISER, Rarity.EPIC, 3.4f, 5.0f, 9f, 1.6f, 4f,
            new int[] {0, 0, 0, 0, 0, 3, 0, 2}),
    BARRACUDA(50, 180, 0.0000045, FishStyle.DARTER, Rarity.RARE, 1.6f, 2.6f, 13f, 2.5f, 4f,
            new int[] {0, 0, 0, 0, 0, 3, 0, 1}),
    // mackerels and tunas
    MACKEREL(20, 55, 0.0000085, FishStyle.DARTER, Rarity.COMMON, 1.8f, 3.2f, 8f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 12, 8, 6, 0}),
    TUNA(80, 300, 0.000015, FishStyle.BRUISER, Rarity.EPIC, 4.0f, 5.6f, 10f, 1.6f, 5f,
            new int[] {0, 0, 0, 0, 1, 2, 1, 3}),
    SKIPJACK(40, 100, 0.000013, FishStyle.FIGHTER, Rarity.UNCOMMON, 2.8f, 4.2f, 8f, 1.2f, 4.5f,
            new int[] {0, 0, 0, 0, 2, 6, 0, 2}),
    SWORDFISH(100, 400, 0.0000035, FishStyle.FIGHTER, Rarity.EPIC, 2.6f, 4.4f, 11f, 6f, 5f,
            new int[] {0, 0, 0, 0, 0, 1, 0, 1}),
    // flatfish
    PLAICE(20, 70, 0.0000115, FishStyle.BRUISER, Rarity.COMMON, 5.5f, 1.0f, 7f, 0.5f, 3f,
            new int[] {0, 0, 0, 0, 7, 1, 6, 0}),
    FLOUNDER(20, 55, 0.000012, FishStyle.BRUISER, Rarity.COMMON, 5.0f, 1.0f, 6.5f, 0.5f, 3f,
            new int[] {1, 0, 0, 0, 6, 1, 5, 0}),
    SOLE(20, 60, 0.0000095, FishStyle.BRUISER, Rarity.UNCOMMON, 4.0f, 0.8f, 8f, 0.5f, 2.5f,
            new int[] {0, 0, 0, 0, 5, 4, 1, 0}),
    TURBOT(30, 100, 0.000014, FishStyle.BRUISER, Rarity.RARE, 6.5f, 1.2f, 7f, 0.5f, 3f,
            new int[] {0, 0, 0, 0, 3, 2, 2, 1}),
    HALIBUT(60, 250, 0.0000105, FishStyle.BRUISER, Rarity.EPIC, 6.0f, 1.4f, 10f, 0.8f, 3.5f,
            new int[] {0, 0, 0, 0, 0, 0, 4, 3}),
    // other sea fish
    REDFISH(25, 90, 0.000014, FishStyle.FIGHTER, Rarity.UNCOMMON, 2.4f, 4.4f, 7.5f, 1.2f, 4f,
            new int[] {0, 0, 0, 0, 0, 0, 6, 5}),
    MONKFISH(30, 180, 0.000014, FishStyle.BRUISER, Rarity.EPIC, 6.0f, 2.4f, 7f, 0.5f, 3.5f,
            new int[] {0, 0, 0, 0, 2, 1, 2, 4}),
    GURNARD(20, 60, 0.0000095, FishStyle.FIGHTER, Rarity.UNCOMMON, 2.2f, 3.2f, 8f, 1.6f, 3.5f,
            new int[] {0, 0, 0, 0, 4, 4, 2, 1}),
    WOLFFISH(40, 150, 0.000009, FishStyle.BRUISER, Rarity.RARE, 2.4f, 3.6f, 11f, 1.6f, 3.5f,
            new int[] {0, 0, 0, 0, 0, 0, 3, 2}),
    // the special one
    GOLDEN_KOI(15, 75, 0.000019, FishStyle.DARTER, Rarity.LEGENDARY, 2.0f, 3.5f, 6.5f, 1f, 5f,
            new int[] {1, 1, 1, 0, 0, 0, 0, 0}),
    // the bosses: they never spawn on their own, they only take their own bait (see BossEncounters)
    ANCIENT_CATFISH(240, 320, 0.0000075, FishStyle.BRUISER, Rarity.LEGENDARY, 3.4f, 3.0f, 10f, 1f, 4.5f,
            new int[] {0, 0, 0, 0, 0, 0, 0, 0}),
    REED_KING(130, 170, 0.0000085, FishStyle.FIGHTER, Rarity.LEGENDARY, 1.8f, 3.0f, 11f, 2.8f, 4.5f,
            new int[] {0, 0, 0, 0, 0, 0, 0, 0}),
    BLACK_STURGEON(250, 350, 0.0000045, FishStyle.FIGHTER, Rarity.LEGENDARY, 2.8f, 2.8f, 14f, 3.4f, 5.5f,
            new int[] {0, 0, 0, 0, 0, 0, 0, 0}),
    DEEP_WARDEN(150, 220, 0.000012, FishStyle.BRUISER, Rarity.MYTHIC, 4.0f, 4.6f, 9f, 2.0f, 5f,
            new int[] {0, 0, 0, 0, 0, 0, 0, 0});

    /** Gewöhnlich, Ungewöhnlich, Selten, Episch, Legendär, Mythisch. */
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC }

    /** What a fish eats; decides which baits it takes. */
    public enum Diet {
        /** Coarse fish: worms, maggots, corn, bread, boilies. */
        COARSE(14),
        /** Freshwater predators: baitfish and lures. */
        PREDATOR(18),
        /** Trout and their kin: insects, flies, small spinners. */
        SALMONID(6),
        /** Sea predators: baitfish, strips, lures. */
        SEA_PREDATOR(10),
        /** Fish of the sea floor: shrimp, strips, worms. */
        SEA_BOTTOM(14),
        /** Shoal fish of the open sea: tiny baits. */
        SHOAL(6);

        private final int strikeIdeal;

        Diet(int strikeIdeal) { this.strikeIdeal = strikeIdeal; }

        /** Ticks after the bite at which the strike sets the hook best: predators turn the bait first, trout spit fast. */
        public int strikeIdeal() { return strikeIdeal; }

        /** Hunts other fish. */
        public boolean predatory() { return this == PREDATOR || this == SEA_PREDATOR; }
    }

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
    public boolean hasBarbels() {
        return switch (this) {
            case CATFISH, BARBEL, ATLANTIC_COD, HADDOCK, STURGEON, ANCIENT_CATFISH, BLACK_STURGEON -> true;
            default -> false;
        };
    }
    /** Flatfish lie on their side: a wide, low body. */
    public boolean flat() { return width > height * 2; }
    public double modelLengthCm() { return (modelBodyLength() + 3 + modelNoseLength() + modelTailLength()) * 100.0 / 16; }
    public String rarityKey() { return "rarity.atzen." + rarity.name().toLowerCase(Locale.ROOT); }
    /** Spawn and bite weight in a habitat; 0 where the fish does not live. */
    public int weight(Habitat habitat) { return habitatWeights[habitat.ordinal()]; }
    /** Shoal fish swim in groups, the big predators and the rare ones alone. */
    public int maxGroup() {
        if (rarity.ordinal() >= Rarity.RARE.ordinal() || this == ZANDER || this == EEL || flat()) return 1;
        return switch (this) {
            case HERRING, SARDINE, ANCHOVY, SPRAT, MACKEREL -> 8;
            case ROACH, RUDD, BREAM -> 6;
            default -> 4;
        };
    }

    public FishSpecimen roll(Random random) { return roll(random, 0); }

    /** A random specimen; {@code sizeBias} 0..1 favours bigger fish (big baits catch big fish). */
    public FishSpecimen roll(Random random, double sizeBias) {
        double fraction = Math.pow(random.nextDouble(), 2 - Math.clamp(sizeBias, 0, 1));
        double length = Math.round((minLength + (maxLength - minLength) * fraction) * 10) / 10.0;
        double weight = Math.max(0.01, Math.round(weightFactor * Math.pow(length, 3)
                * (0.88 + random.nextDouble() * 0.24) * 100) / 100.0);
        return new FishSpecimen(this, length, weight);
    }

    public Diet diet() {
        return switch (this) {
            case CARP, CRUCIAN_CARP, BARBEL, ROACH, RUDD, BREAM, TENCH, CHUB, GOLDEN_KOI, STURGEON, BLACK_STURGEON -> Diet.COARSE;
            case PIKE, ZANDER, PERCH, CATFISH, EEL, ASP, ANCIENT_CATFISH, REED_KING -> Diet.PREDATOR;
            case RIVER_TROUT, RAINBOW_TROUT, GRAYLING, SALMON, SEA_TROUT -> Diet.SALMONID;
            case HADDOCK, GILTHEAD_BREAM, RED_MULLET, PLAICE, FLOUNDER, SOLE, TURBOT, HALIBUT, MONKFISH, GURNARD -> Diet.SEA_BOTTOM;
            case HERRING, SARDINE, ANCHOVY, SPRAT -> Diet.SHOAL;
            default -> Diet.SEA_PREDATOR;
        };
    }

    /** The boss behind this species, or null. */
    public Boss boss() {
        return switch (this) {
            case ANCIENT_CATFISH -> Boss.ANCIENT_CATFISH;
            case REED_KING -> Boss.REED_KING;
            case BLACK_STURGEON -> Boss.BLACK_STURGEON;
            case DEEP_WARDEN -> Boss.DEEP_WARDEN;
            default -> null;
        };
    }

    public static FishSpecies of(Boss boss) {
        for (FishSpecies species : values()) if (species.boss() == boss) return species;
        throw new IllegalArgumentException(boss.name());
    }

    /** Lives only in the sea. */
    public boolean salt() {
        for (int i = 0; i < Habitat.OCEAN.ordinal(); i++) if (habitatWeights[i] > 0) return false;
        return boss() == null;
    }

    /** Where in the water column the fish likes to be: -1 at the bottom, 0 in the middle, 1 just under the surface. */
    public int preferredDepth() {
        return switch (this) {
            case HERRING, SARDINE, ANCHOVY, SPRAT, ASP, MACKEREL, RUDD, GOLDEN_KOI, SKIPJACK -> 1;
            case CARP, CRUCIAN_CARP, TENCH, BARBEL, CATFISH, EEL, BREAM, STURGEON, ATLANTIC_COD, HADDOCK, PLAICE, FLOUNDER,
                 SOLE, TURBOT, HALIBUT, MONKFISH, GURNARD, WOLFFISH, RED_MULLET, ZANDER -> -1;
            default -> 0;
        };
    }

    /** Sharp teeth: without a steel leader they can bite through the line. */
    public boolean teeth() {
        return switch (this) {
            case PIKE, ZANDER, BARRACUDA, WOLFFISH, MONKFISH, REED_KING, DEEP_WARDEN -> true;
            default -> false;
        };
    }

    /** Shy fish notice a visible leader and a big hook. */
    public boolean shy() {
        return diet() == Diet.SALMONID || this == CARP || this == TENCH || this == GRAYLING || this == SEA_BASS;
    }

    private boolean jumper() {
        return switch (this) {
            case RIVER_TROUT, RAINBOW_TROUT, GRAYLING, SALMON, SEA_TROUT, PIKE, ASP, BARRACUDA, SWORDFISH, SEA_BASS,
                 MACKEREL, SKIPJACK -> true;
            default -> false;
        };
    }

    private boolean diver() {
        return switch (this) {
            case CARP, CRUCIAN_CARP, TENCH, CATFISH, BARBEL, BREAM, ATLANTIC_COD, SAITHE, HADDOCK, GROUPER, HALIBUT,
                 PLAICE, FLOUNDER, SOLE, TURBOT, MONKFISH, WOLFFISH, REDFISH, STURGEON, TUNA -> true;
            default -> false;
        };
    }

    private boolean snagger() {
        return switch (this) {
            case PIKE, ZANDER, EEL, TENCH, CARP, CATFISH, GROUPER, WOLFFISH, CHUB, PERCH, BARBEL -> true;
            default -> false;
        };
    }

    private boolean shaker() {
        return switch (this) {
            case PIKE, ZANDER, CATFISH, BARRACUDA, WOLFFISH, EEL, PERCH, SEA_BASS, SALMON, HAKE, MONKFISH -> true;
            default -> false;
        };
    }

    /** How often this fish runs, dives, heads for cover, shakes its head or jumps when hooked. */
    public int[] actionWeights() {
        return Fighter.weights(3, style == FishStyle.BRUISER ? 3 : 4, diver() ? 4 : 1, snagger() ? 4 : 1,
                shaker() ? 4 : 1, jumper() ? 3 : 0);
    }

    /**
     * How keen this fish is to feed at this time of day: predators hunt at dawn and dusk, wels, eel and zander
     * at night, coarse fish by day, trout in the twilight. Rain livens up fresh water.
     */
    public double activity(long dayTime, boolean raining) {
        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= 13000 && time < 23000;
        boolean twilight = time >= 22500 || time < 1500 || (time >= 11500 && time < 13500);
        double activity = switch (diet()) {
            case PREDATOR -> twilight ? 1.5 : night ? 1.0 : 0.8;
            case COARSE -> night ? 0.7 : 1.15;
            case SALMONID -> twilight ? 1.4 : night ? 0.5 : 1.0;
            default -> 1.0;
        };
        if (night && (this == CATFISH || this == EEL || this == ZANDER)) activity = 1.8;
        if (raining && !salt()) activity *= 1.2;
        return activity;
    }

    /** Which fish takes a bait in this habitat, weighted by how common each is there. */
    public static FishSpecies choose(Habitat habitat, Random random) {
        return choose(habitat, random, species -> 1);
    }

    /** As {@link #choose(Habitat, Random)}, each weight multiplied by a factor (bait, time of day, weather). */
    public static FishSpecies choose(Habitat habitat, Random random, ToDoubleFunction<FishSpecies> factor) {
        double total = 0;
        for (FishSpecies species : values()) total += species.weight(habitat) * Math.max(0, factor.applyAsDouble(species));
        if (total <= 0) return habitat == Habitat.RIVER ? PERCH : choose(Habitat.RIVER, random, factor);
        double choice = random.nextDouble() * total;
        for (FishSpecies species : values()) {
            choice -= species.weight(habitat) * Math.max(0, factor.applyAsDouble(species));
            if (choice < 0) return species;
        }
        return PERCH;
    }

    public static FishSpecies choose(boolean swamp, Random random) {
        return choose(swamp ? Habitat.SWAMP : Habitat.RIVER, random);
    }
}
