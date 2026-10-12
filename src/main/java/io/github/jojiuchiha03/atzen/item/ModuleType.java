package io.github.jojiuchiha03.atzen.item;

import io.github.jojiuchiha03.atzen.battle.Rig;
import java.util.Locale;

/**
 * Rod modules (Anforderung 12): fitted in the workshop, two per rod. Each changes one part of the rig or adds a
 * small aid; together they let a rod be set up for a purpose instead of simply being "better".
 */
public enum ModuleType {
    FINE_DRAG, REINFORCED_BLANK, BITE_SENSOR, LIGHT_BEARING, CERAMIC_GUIDES, IMPROVED_BEARINGS, REINFORCED_HOOK_MOUNT,
    LEADER_SYSTEM, FLOAT, SONAR, LINE_COUNTER, BOSS_RIG, FEEDER;

    public static final int SLOTS = 2;

    public String id() { return name().toLowerCase(Locale.ROOT) + "_module"; }

    public String translationKey() { return "item.atzen." + id(); }

    /** What the module does to the rig; boss modules only work against a boss. */
    public Rig apply(Rig rig, boolean boss) {
        return switch (this) {
            case FINE_DRAG -> rig.with(rig.lineStrength(), Math.min(1, rig.stretch() + 0.12), rig.abrasion(), rig.rodLoad(),
                    rig.reelSpeed(), rig.reelForce(), rig.capacity(), rig.hookStrength(), rig.snagFactor(), rig.runDamping());
            case REINFORCED_BLANK -> rig.with(rig.lineStrength(), rig.stretch(), rig.abrasion(), rig.rodLoad() * 1.2,
                    rig.reelSpeed(), rig.reelForce(), rig.capacity(), rig.hookStrength(), rig.snagFactor(), rig.runDamping());
            case CERAMIC_GUIDES -> rig.with(rig.lineStrength(), rig.stretch(), rig.abrasion() * 0.7, rig.rodLoad(),
                    rig.reelSpeed(), rig.reelForce(), rig.capacity(), rig.hookStrength(), rig.snagFactor(), rig.runDamping());
            case IMPROVED_BEARINGS -> rig.with(rig.lineStrength(), rig.stretch(), rig.abrasion(), rig.rodLoad(),
                    rig.reelSpeed() * 1.1, rig.reelForce() * 1.05, rig.capacity(), rig.hookStrength(), rig.snagFactor(), rig.runDamping());
            case REINFORCED_HOOK_MOUNT -> rig.with(rig.lineStrength(), rig.stretch(), rig.abrasion(), rig.rodLoad(),
                    rig.reelSpeed(), rig.reelForce(), rig.capacity(), rig.hookStrength() * 1.2, rig.snagFactor(), rig.runDamping());
            case LEADER_SYSTEM -> rig.with(rig.lineStrength(), rig.stretch(), rig.abrasion(), rig.rodLoad(),
                    rig.reelSpeed(), rig.reelForce(), rig.capacity(), rig.hookStrength(), rig.snagFactor() * 0.75, rig.runDamping());
            case LINE_COUNTER -> rig.with(rig.lineStrength(), rig.stretch(), rig.abrasion(), rig.rodLoad(),
                    rig.reelSpeed(), rig.reelForce(), rig.capacity() * 1.1, rig.hookStrength(), rig.snagFactor(), rig.runDamping());
            case BOSS_RIG -> boss ? rig.with(rig.lineStrength() * 1.1, rig.stretch(), rig.abrasion(), rig.rodLoad() * 1.15,
                    rig.reelSpeed(), rig.reelForce(), rig.capacity(), rig.hookStrength() * 1.3, rig.snagFactor(), rig.runDamping() * 0.9) : rig;
            default -> rig;
        };
    }
}
