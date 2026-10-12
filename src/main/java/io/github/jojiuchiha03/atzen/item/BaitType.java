package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.battle.Boss;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;

/**
 * Every bait and lure. Natural baits stack and one is used per fight; lures do not stack, wear down with every
 * fight (a worn lure runs worse) and are lost when the line breaks. The appeal says how much each diet likes it:
 * a fish ignores a bait it does not like, a big bait selects bigger fish.
 */
public enum BaitType {
    // natural baits; appeal for: coarse, predator, salmonid, sea predator, sea bottom, shoal
    WORM("worm_bait", 0, new double[] {1.2, 0.6, 1.0, 0.3, 0.8, 0.2}, 0, 1, 1, 1),
    MAGGOT("maggot_bait", 0, new double[] {1.4, 0.3, 0.8, 0, 0.3, 0.5}, 0, 1, 1, 1),
    CORN("corn_bait", 0, new double[] {1.5, 0, 0, 0, 0, 0}, 0.2, 1, 1, 1),
    BREAD("bread_bait", 0, new double[] {1.2, 0, 0.2, 0, 0, 0.4}, 0, 1, 1, 1),
    BOILIE("boilie_bait", 0, new double[] {1.0, 0, 0, 0, 0, 0}, 0.7, 1, 1, 1),
    BAITFISH("baitfish_bait", 0, new double[] {0, 1.6, 0.6, 1.3, 0.6, 0}, 0.5, 1, 1, 1),
    FISH_STRIP("fish_strip_bait", 0, new double[] {0.2, 1.0, 0.4, 1.2, 1.3, 0.2}, 0.2, 1, 1, 1),
    LARVA("larva_bait", 0, new double[] {0.8, 0.3, 1.4, 0, 0, 0.3}, 0, 1, 1, 1),
    SHRIMP("shrimp_bait", 0, new double[] {0, 0.2, 0.2, 1.0, 1.4, 0.8}, 0.1, 1, 1, 1),
    GLOW("glow_bait", 0, new double[] {0.8, 0.8, 0.8, 0.8, 0.8, 0.8}, 0, 1, 0.8, 1),
    HEARTY("hearty_bait", 0, new double[] {1.0, 1.0, 1.0, 1.0, 1.0, 0.5}, 0, 1.25, 1, 1.12),
    // lures
    SPINNER("spinner", 40, new double[] {0.2, 1.2, 1.2, 0.6, 0.2, 0.3}, 0.1, 1, 1, 1),
    SPOON("spoon", 50, new double[] {0, 1.2, 0.8, 1.2, 0.3, 0.3}, 0.2, 1, 1, 1),
    WOBBLER("wobbler", 40, new double[] {0, 1.5, 0.6, 1.0, 0, 0}, 0.5, 1, 1, 1),
    SOFT_LURE("soft_lure", 20, new double[] {0, 1.3, 0.3, 1.0, 0.6, 0}, 0.3, 1, 1, 1),
    POPPER("popper", 40, new double[] {0, 0.6, 0.2, 1.4, 0, 0}, 0.3, 1, 1, 1),
    JIG("jig", 45, new double[] {0, 0.8, 0.3, 1.0, 1.2, 0}, 0.2, 1, 1, 1),
    STREAMER("streamer", 30, new double[] {0, 0.7, 1.3, 0.6, 0, 0}, 0.2, 1, 1, 1),
    DRY_FLY("dry_fly", 25, new double[] {0.3, 0, 1.6, 0, 0, 0}, 0, 1, 1, 1),
    NYMPH("nymph", 25, new double[] {0.5, 0, 1.4, 0, 0.2, 0}, 0, 1, 1, 1),
    // boss baits: on their own a strong bait, in the right place the call of a boss
    ANCIENT("ancient_bait", 0, new double[] {0, 1.4, 0, 0, 0, 0}, 0.8, 1, 1, 1),
    REED_KING("reed_king_lure", 60, new double[] {0, 1.6, 0.4, 0, 0, 0}, 0.8, 1, 1, 1),
    CURRENT("current_bait", 0, new double[] {1.2, 0, 0, 0, 1.0, 0}, 0.8, 1, 1, 1),
    DEEP_LIGHT("deep_light_bait", 0, new double[] {0.8, 0.8, 0.8, 0.8, 0.8, 0.8}, 0.6, 1, 1, 1);

    private final String id;
    private final int durability;
    private final double[] appeal;
    private final double sizeBias, drain, run, strain;

    BaitType(String id, int durability, double[] appeal, double sizeBias, double drain, double run, double strain) {
        this.id = id;
        this.durability = durability;
        this.appeal = appeal;
        this.sizeBias = sizeBias;
        this.drain = drain;
        this.run = run;
        this.strain = strain;
    }

    public String id() { return id; }
    public boolean lure() { return durability > 0; }
    public int durability() { return durability; }
    public double sizeBias() { return sizeBias; }
    /** Multiplier on how fast the fish tires. */
    public double drain() { return drain; }
    /** Multiplier on how hard the fish pulls in runs. */
    public double run() { return run; }
    /** Multiplier on line strain (more than 1 is worse). */
    public double strain() { return strain; }

    public Boss boss() {
        return switch (this) {
            case ANCIENT -> Boss.ANCIENT_CATFISH;
            case REED_KING -> Boss.REED_KING;
            case CURRENT -> Boss.BLACK_STURGEON;
            case DEEP_LIGHT -> Boss.DEEP_WARDEN;
            default -> null;
        };
    }

    /** How keen this species is on this bait; below 0.5 it ignores the bait. Favourites get a bonus. */
    public double appeal(FishSpecies species) {
        double value = appeal[species.diet().ordinal()];
        boolean favourite = switch (this) {
            case SOFT_LURE -> species == FishSpecies.ZANDER;
            case BOILIE, CORN -> species == FishSpecies.CARP || species == FishSpecies.CRUCIAN_CARP;
            case BAITFISH, FISH_STRIP -> species == FishSpecies.CATFISH || species == FishSpecies.PIKE;
            case SPINNER -> species == FishSpecies.PERCH || species == FishSpecies.ASP;
            case BREAD -> species == FishSpecies.CHUB || species == FishSpecies.RUDD;
            case WORM -> species == FishSpecies.EEL || species == FishSpecies.TENCH;
            case DRY_FLY, NYMPH -> species == FishSpecies.GRAYLING || species == FishSpecies.RIVER_TROUT;
            case POPPER -> species == FishSpecies.SEA_BASS || species == FishSpecies.BARRACUDA;
            case JIG -> species == FishSpecies.ATLANTIC_COD || species == FishSpecies.SAITHE;
            default -> false;
        };
        return favourite ? value * 1.4 : value;
    }

    /** Without bait the hook only now and then catches something. */
    public static final double BARE_HOOK = 0.4;

    public String translationKey() { return "item.atzen." + id; }

    /** Weight in grams: what the rod has to cast. */
    public double grams() {
        return switch (this) {
            case WORM -> 3;
            case MAGGOT -> 1;
            case CORN -> 2;
            case BREAD -> 2;
            case BOILIE -> 15;
            case BAITFISH -> 40;
            case FISH_STRIP -> 20;
            case LARVA -> 1;
            case SHRIMP -> 8;
            case GLOW -> 5;
            case HEARTY -> 10;
            case SPINNER -> 8;
            case SPOON -> 15;
            case WOBBLER -> 18;
            case SOFT_LURE -> 20;
            case POPPER -> 25;
            case JIG -> 30;
            case STREAMER -> 4;
            case DRY_FLY -> 1;
            case NYMPH -> 3;
            case ANCIENT -> 60;
            case REED_KING -> 25;
            case CURRENT -> 50;
            case DEEP_LIGHT -> 30;
        };
    }

    /** Flies are cast with the fly line, not by their own weight. */
    public boolean fly() { return this == STREAMER || this == DRY_FLY || this == NYMPH; }
}
