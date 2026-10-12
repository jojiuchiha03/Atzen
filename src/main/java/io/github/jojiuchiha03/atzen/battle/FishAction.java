package io.github.jojiuchiha03.atzen.battle;

import java.util.Locale;

/**
 * What the hooked fish is doing. Every action except CALM and TIRED is announced by a short warning first
 * (the fish thrashes), so a good angler can react: ease the drag before a run, lower the rod before a jump,
 * steer against a run to the side.
 */
public enum FishAction {
    /** Holds against the line; the time to reel and pump. */
    CALM(0.35, false),
    /** Runs away from the angler: takes line from the reel when it pulls harder than the drag. */
    RUN(1.0, true),
    /** Heads for the bottom. */
    DIVE(0.85, true),
    /** Runs sideways towards cover; steer against it or the line rubs on snags. */
    SIDE(0.75, true),
    /** Head shakes: jerks on the line that work the hook loose when the line is too tight or too loose. */
    SHAKE(0.6, true),
    /** Leaps out of the water to throw the hook. */
    JUMP(0.15, true),
    /** Worn out for a moment. */
    TIRED(0.15, false);

    private final double pull;
    private final boolean announced;

    FishAction(double pull, boolean announced) {
        this.pull = pull;
        this.announced = announced;
    }

    /** Share of the fish's full strength it puts on the line. */
    public double pull() { return pull; }
    public boolean announced() { return announced; }
    public String translationKey() { return "action.atzen." + name().toLowerCase(Locale.ROOT); }
}
