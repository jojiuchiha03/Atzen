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
    /** For the peaceful fish: coarse fish, trout, bottom fish, shoals. */
    WORM("worm_bait", 0, new double[] {1.3, 0.4, 1.2, 0.3, 1.1, 1.0}, 0, 1, 1, 1),
    /** For the predators, fresh water and sea. */
    BAITFISH("baitfish_bait", 0, new double[] {0, 1.5, 0.6, 1.4, 0.6, 0}, 0.5, 1, 1, 1),
    /** Draws every fish, at night most of all; runs pull weaker. */
    GLOW("glow_bait", 0, new double[] {0.8, 0.8, 0.8, 0.8, 0.8, 0.8}, 0, 1, 0.8, 1),
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

    /** How keen this species is on this bait; below 0.5 it ignores the bait. */
    public double appeal(FishSpecies species) { return appeal[species.diet().ordinal()]; }

    /** Without bait the hook only now and then catches something. */
    public static final double BARE_HOOK = 0.4;

    public String translationKey() { return "item.atzen." + id; }

}
