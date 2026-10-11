package io.github.jojiuchiha03.atzen.battle;

import java.util.Random;

/** Fixed-step, server-owned simulation. Clients only provide whether they reel. */
public final class FishingBattle {
    public static final int MAX_TICKS = 20 * 60;
    public static final double SWEET_MIN = 30;
    public static final double SWEET_MAX = 65;

    public enum Phase { STEADY, WARNING, SURGE, RECOVERY }
    public enum Result { ACTIVE, CAUGHT, SNAPPED, ESCAPED, TIMEOUT, CANCELLED }

    private final FishStyle style;
    private final Random random;
    private final BattleTuning tuning;
    private Phase phase = Phase.STEADY;
    private Result result = Result.ACTIVE;
    private double tension = 25;
    private double progress;
    private int phaseTicks;
    private int elapsed;
    private int slackTicks;
    private int goodTicks;
    private int reelTicks;

    public FishingBattle(FishStyle style, long seed) {
        this(style, seed, BattleTuning.DEFAULT);
    }

    public FishingBattle(FishStyle style, long seed, BattleTuning tuning) {
        this.style = style;
        this.random = new Random(seed);
        this.tuning = tuning;
        phaseTicks = 60 + random.nextInt(30);
    }

    public void tick(boolean reeling) {
        if (result != Result.ACTIVE) return;
        elapsed++;
        if (--phaseTicks <= 0) nextPhase();

        if (reeling) {
            reelTicks++;
            double strain = switch (phase) {
                case STEADY -> 0.38;
                case WARNING -> 0.55;
                case SURGE -> 1.65;
                case RECOVERY -> 0.12;
            };
            tension += strain * style.pull() * tuning.strain()
                    * (phase == Phase.SURGE ? tuning.surgeStrain() : 1);
            boolean sweet = tension >= SWEET_MIN && tension <= SWEET_MAX;
            if (sweet) goodTicks++;
            double gain = switch (phase) {
                case STEADY, WARNING -> 0.27;
                case SURGE -> 0.08;
                case RECOVERY -> 0.38;
            };
            progress += gain * style.catchSpeed() * tuning.catchSpeed() * (sweet ? 1.35 : 1.0);
        } else {
            tension -= phase == Phase.SURGE ? 0.42 : 0.72;
            // Resting prevents snapping, but the fish slowly regains ground.
            progress -= phase == Phase.SURGE ? 0.045 : 0.015;
        }

        tension = Math.clamp(tension, 0, 100);
        progress = Math.clamp(progress, 0, 100);
        slackTicks = tension < 8 ? slackTicks + 1 : 0;

        if (tension >= 100) result = Result.SNAPPED;
        else if (slackTicks >= 60) result = Result.ESCAPED;
        else if (progress >= 100) result = Result.CAUGHT;
        else if (elapsed >= MAX_TICKS) result = Result.TIMEOUT;
    }

    private void nextPhase() {
        phase = switch (phase) {
            case STEADY -> Phase.WARNING;
            case WARNING -> Phase.SURGE;
            case SURGE -> Phase.RECOVERY;
            case RECOVERY -> Phase.STEADY;
        };
        phaseTicks = switch (phase) {
            case STEADY -> 45 + random.nextInt(35);
            case WARNING -> 24;
            case SURGE -> 28 + random.nextInt(18);
            case RECOVERY -> 40 + random.nextInt(20);
        };
    }

    public void cancel() { if (result == Result.ACTIVE) result = Result.CANCELLED; }
    public FishStyle style() { return style; }
    public Phase phase() { return phase; }
    public Result result() { return result; }
    public double tension() { return tension; }
    public double progress() { return progress; }
    public int elapsed() { return elapsed; }
    public int slackTicks() { return slackTicks; }
    public int quality() { return reelTicks == 0 ? 0 : (int) (100L * goodTicks / reelTicks); }
}
