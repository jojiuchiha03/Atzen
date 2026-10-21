package io.github.jojiuchiha03.atzen.battle;

import java.util.Random;

/**
 * The fight with a hooked fish, simulated by the server in fixed ticks; clients only say what the angler does.
 *
 * <p>Two keys: <b>reel</b> (R) and <b>give line</b> (F). The fish pulls with a force that depends on its size,
 * its stamina and what it is doing; the line carries that force as tension. The drag sets itself: it slips at
 * {@link #DRAG_SHARE} of what line and rod bear, so a fish pulling harder takes line. Giving line opens the drag
 * wide. The rules: reel while the fish holds or rests; stop reeling when it announces something or pulls away; give
 * line in jumps and head shakes and when the line is about to break. Reeling against a slipping drag twists and
 * wears the line until it snaps, a tight line in a jump or reeling in head shakes tears the hook out, and a slack
 * line on a resting fish lets it fall out. A fish is landed when it is
 * close and worn out.
 */
public final class FishingBattle {
    public static final int MAX_TICKS = 20 * 60 * 8;
    public static final int BOSS_MAX_TICKS = 20 * 60 * 12;
    public static final double LANDING_DISTANCE = 4;
    /** Where the automatic drag slips, as a share of what line and rod bear. */
    public static final double DRAG_SHARE = 0.75;
    /** The drag while the angler gives line. */
    public static final double GIVE_SHARE = 0.3;

    public enum Result { ACTIVE, CAUGHT, SNAPPED, SPOOLED, ESCAPED, TIMEOUT, CANCELLED }

    /** What the angler does this tick: reel in (R) or give line (F); both at once count as giving. */
    public record Input(boolean reeling, boolean giving) {
        public static final Input NONE = new Input(false, false);
    }

    private final Fighter fighter;
    private final Rig rig;
    private final Random random;
    private Result result = Result.ACTIVE;
    private FishAction action = FishAction.CALM;
    private FishAction pending;
    private int actionTicks;
    private int warning;
    private int elapsed;
    private double tension;
    private double distance;
    private double stamina = 1;
    private double hookHold;
    private double condition = 1;
    private int sideDir = 1;
    private int bossPhase;
    private boolean faking;
    private boolean fakeDone;
    private boolean explosive;
    private int adaptCooldown;
    private int giveTicks;
    private int reelTicks;
    private boolean giving;
    private boolean slack;
    private boolean twisting;
    private int landableTicks;
    private boolean landable;
    private int goodTicks;
    private int ticks;

    public FishingBattle(Fighter fighter, Rig rig, double startDistance, double hookHold, long seed) {
        this.fighter = fighter;
        this.rig = rig;
        this.random = new Random(seed);
        this.distance = Math.clamp(startDistance, 3, 40);
        this.hookHold = Math.clamp(hookHold, 20, 100);
        this.tension = fighter.power() * 0.5;
        // The strike itself: the fish bolts straight out into open water before anything else.
        start(FishAction.RUN);
        actionTicks += 15;
    }

    /** Hook hold from the strike timing: ticks between the bite and the strike against the fish's ideal moment. */
    public static double strikeHold(int ticksAfterBite, int ideal) {
        return strikeHold(ticksAfterBite, ideal, 0.4);
    }

    /** As above; a sensitive rod lets the angler feel the bite, which widens the window for a good strike. */
    public static double strikeHold(int ticksAfterBite, int ideal, double sensitivity) {
        double quality = 1 - Math.min(1, Math.abs(ticksAfterBite - ideal) / (15 + 25 * Math.clamp(sensitivity, 0, 1)));
        return 55 + 45 * quality;
    }

    public void tick(Input input) {
        if (result != Result.ACTIVE) return;
        elapsed++;
        Boss.Phase phase = fighter.boss() == null ? null : fighter.boss().phase(bossPhase);
        if (adaptCooldown > 0) adaptCooldown--;
        advanceAction();

        giving = input.giving();
        boolean reeling = input.reeling() && !giving;
        if (giving) giveTicks++;
        if (reeling) reelTicks++;
        adapt();

        // the fish's pull
        double strength = fighter.power() * (0.3 + 0.7 * stamina) * (phase == null ? 1 : phase.power());
        double pull = strength * (warning > 0 ? 0.45 : action.pull());
        if (action == FishAction.SHAKE) pull *= 1 + 0.55 * Math.sin(elapsed * 1.3);
        if (action == FishAction.RUN) pull *= rig.runDamping() * (explosive ? 1.8 : 1);
        if (phase != null) pull += fighter.power() * phase.current();

        double dragKg = dragKg();
        double target = reeling ? pull * 1.15 + 0.3 : pull;
        boolean slipping = target > dragKg;

        // line out: a fish that pulls harder than the drag takes line
        boolean away = warning == 0 && (action == FishAction.RUN || action == FishAction.DIVE || action == FishAction.SIDE);
        if (away && pull > dragKg) {
            distance += fighter.speed() * (phase == null ? 1 : phase.speed()) * (explosive ? 1.4 : 1)
                    * Math.clamp((pull - dragKg) / (0.35 * pull), 0, 1);
        }
        // line in: only against a fish that pulls less than the reel
        if (reeling && !slipping) distance -= rig.reelSpeed() * Math.clamp((rig.reelForce() - pull) / rig.reelForce(), 0, 1);
        // Winding against a slipping drag twists the line and wears it down.
        twisting = reeling && slipping;
        if (twisting) condition -= 0.004;
        distance = Math.max(1.5, distance);

        // tension follows the target, cushioned by rod and line
        double response = Math.clamp(0.3 + (1 - rig.rodFlex()) * 0.35 + (1 - rig.stretch()) * 0.35, 0.3, 1);
        tension += (Math.min(target, dragKg) - tension) * response;
        if (action == FishAction.JUMP && warning == 0 && actionTicks == 4 && tension > 0.3 * fighter.power()) {
            shock(0.6 * strength); // the fish crashes back into the water on a tight line
        }
        tension = Math.max(0, tension);

        if (tension > breakingLoad() || condition <= 0.05) {
            result = Result.SNAPPED;
            return;
        }
        if (distance > rig.capacity()) {
            result = Result.SPOOLED;
            return;
        }

        // the hook: a slack line lets it fall out, reeling in head shakes and a tight line in a jump tear it out.
        // Slack only counts while the fish holds still: then the line has to be kept tight with the reel.
        boolean resting = warning == 0 && (action == FishAction.CALM || action == FishAction.TIRED);
        slack = !reeling && resting && tension < 0.12 * fighter.power();
        double loss = slack ? 0.6 : 0;
        if (action == FishAction.SHAKE && warning == 0) loss += reeling || slack ? 0.6 : 0.08;
        if (action == FishAction.JUMP && warning == 0 && actionTicks > 4) {
            if (fighter.boss() == Boss.REED_KING && giving) loss += 0.12; // the dance: hard to hold even when giving
            else if (!giving && tension > 0.45 * breakingLoad()) loss += 2.2;
            else loss += 0.05;
        }
        hookHold -= loss;
        if (hookHold <= 0) {
            result = Result.ESCAPED;
            return;
        }

        // the fish tires against the line, and from its own runs; it gets its breath back when the line is light
        double effort = Math.clamp(Math.max(tension, away ? 0.8 * pull : 0) / fighter.power(), 0, 2);
        double drain = 0.003 * effort * rig.staminaDrain() / fighter.endurance();
        if (away && pull <= dragKg) drain *= 1.4; // a run against a drag that holds wears the fish out
        stamina -= drain;
        if (effort < 0.25 && resting) stamina += 0.0005 / fighter.endurance();
        stamina = Math.clamp(stamina, 0, 1);
        // A boss's strength is split into its phases; at each threshold it changes its behaviour and finds new strength.
        if (fighter.boss() != null && bossPhase < fighter.boss().phases() - 1
                && stamina <= 1 - (bossPhase + 1.0) / fighter.boss().phases()) nextBossPhase();

        ticks++;
        if (!slack && !twisting && tension >= 0.25 * dragKg && tension <= 0.95 * dragKg) goodTicks++;
        boolean lastPhase = fighter.boss() == null || bossPhase == fighter.boss().phases() - 1;
        boolean wasLandable = landable;
        landable = lastPhase && distance <= LANDING_DISTANCE && stamina <= landingStamina() && !faking && warning == 0
                && action != FishAction.RUN && action != FishAction.JUMP;
        landableTicks = landable ? (wasLandable ? landableTicks + 1 : 0) : 0;
        if (elapsed >= (fighter.boss() != null ? BOSS_MAX_TICKS : MAX_TICKS)) result = Result.TIMEOUT;
    }

    private void advanceAction() {
        if (warning > 0) {
            if (--warning == 0) start(pending);
            return;
        }
        // The Old One plays dead once, close to the bank, then explodes.
        if (fighter.boss() == Boss.ANCIENT_CATFISH && bossPhase == fighter.boss().phases() - 1 && !fakeDone
                && stamina < 0.12 && distance < 7) {
            fakeDone = true;
            faking = true;
            action = FishAction.TIRED;
            actionTicks = 50;
            return;
        }
        if (--actionTicks > 0) return;
        if (faking) {
            faking = false;
            stamina = Math.min(1, stamina + 0.2);
            explosive = true;
            announce(FishAction.RUN, 6);
            return;
        }
        explosive = false;
        if (action == FishAction.JUMP && fighter.boss() == Boss.REED_KING && bossPhase == 1 && random.nextInt(10) < 6) {
            announce(FishAction.JUMP, 8); // the dance: leap after leap
            return;
        }
        if (action.announced() || action == FishAction.TIRED) {
            start(stamina < 0.3 && random.nextInt(3) == 0 ? FishAction.TIRED : FishAction.CALM);
            return;
        }
        FishAction next = choose();
        if (next.announced()) announce(next, warningTicks());
        else start(next);
    }

    private FishAction choose() {
        int[] weights = fighter.boss() == null ? fighter.weights() : fighter.boss().phase(bossPhase).weights();
        int tired = stamina < 0.35 ? (int) Math.round(12 * (0.35 - stamina) / 0.35) : 0;
        int total = tired;
        for (int weight : weights) total += weight;
        int choice = random.nextInt(Math.max(1, total));
        for (FishAction candidate : FishAction.values()) {
            int weight = weights[candidate.ordinal()] + (candidate == FishAction.TIRED ? tired : 0);
            choice -= weight;
            if (choice < 0) return candidate;
        }
        return FishAction.CALM;
    }

    private int warningTicks() {
        return fighter.boss() == null ? 20 : fighter.boss().phase(bossPhase).warning();
    }

    private void announce(FishAction next, int ticks) {
        pending = next;
        warning = Math.max(1, ticks);
    }

    private void start(FishAction next) {
        action = next;
        pending = null;
        double calm = fighter.boss() == null ? 1 : fighter.boss().phase(bossPhase).calm();
        actionTicks = switch (next) {
            case CALM -> (int) ((20 + random.nextInt(25) + 50 * (1 - stamina)) * calm);
            case RUN -> 30 + random.nextInt(35);
            case DIVE -> 30 + random.nextInt(25);
            case SIDE -> 40 + random.nextInt(25);
            case SHAKE -> 20 + random.nextInt(16);
            case JUMP -> 22;
            case TIRED -> 50 + random.nextInt(40);
        };
        if (next == FishAction.SIDE) sideDir = random.nextBoolean() ? 1 : -1;
        if (next == FishAction.RUN) shock(0.35 * fighter.power() * (0.3 + 0.7 * stamina));
    }

    /** The Deep Warden answers the angler's habit: always reeling draws runs, always giving draws head shakes. */
    private void adapt() {
        if (fighter.boss() != Boss.DEEP_WARDEN || adaptCooldown > 0 || warning > 0
                || (action != FishAction.CALM && action != FishAction.TIRED)) return;
        int total = giveTicks + reelTicks;
        if (total < 60) return;
        if (giveTicks > total * 0.5) {
            announce(FishAction.SHAKE, 6);
            adaptCooldown = 100;
        } else if (reelTicks > total * 0.85) {
            announce(FishAction.RUN, 6);
            adaptCooldown = 100;
        }
    }

    private void nextBossPhase() {
        bossPhase++;
        stamina = Math.min(1, stamina + 0.12);
        giveTicks = 0;
        reelTicks = 0;
        announce(FishAction.RUN, 10);
    }

    private void shock(double kg) {
        double cushion = 1 - 0.5 * rig.rodFlex() - 0.6 * rig.stretch();
        tension += kg * Math.max(0.15, cushion);
        double dragKg = dragKg();
        if (tension > dragKg) tension = dragKg + (tension - dragKg) * 0.5;
    }

    private double landingStamina() { return fighter.boss() != null ? 0.1 : 0.2; }

    /** Landing by hand (small fish): a close, worn-out fish; one with fight left in it can kick free. */
    public boolean land() {
        double chance = Math.clamp(0.95 - fighter.weight() * 0.05 - stamina * 1.5, 0.3, 0.95);
        if (fighter.boss() != null) chance = Math.min(chance, 0.6);
        return land(chance);
    }

    /** Landing with a given chance (the net and how well it was aimed, see {@link Landing}). */
    public boolean land(double chance) {
        if (result != Result.ACTIVE || !landable) return false;
        if (random.nextDouble() < chance) {
            result = Result.CAUGHT;
            return true;
        }
        stamina = Math.min(1, stamina + 0.12);
        start(FishAction.RUN);
        landable = false;
        return false;
    }

    public void cancel() { if (result == Result.ACTIVE) result = Result.CANCELLED; }

    /** The drag sets itself below what line and rod bear; giving line opens it. */
    public double dragKg() {
        return Math.min(rig.lineStrength(), rig.rodLoad()) * (giving ? GIVE_SHARE : DRAG_SHARE);
    }

    public double breakingLoad() { return rig.lineStrength() * condition; }
    public Fighter fighter() { return fighter; }
    public Rig rig() { return rig; }
    public Result result() { return result; }
    public FishAction action() { return action; }
    /** The action the fish is about to start, while it is still announcing it; otherwise null. */
    public FishAction pending() { return warning > 0 ? pending : null; }
    public int elapsed() { return elapsed; }
    public double tension() { return tension; }
    public double tensionShare() { return tension / rig.lineStrength(); }
    /** Ticks since the fish became landable, for the net's aim (see {@link Landing}). */
    public int landableTicks() { return landableTicks; }
    public double distance() { return distance; }
    public double stamina() { return stamina; }
    public double hookHold() { return hookHold; }
    public double condition() { return condition; }
    public int sideDir() { return sideDir; }
    public boolean giving() { return giving; }
    public boolean slack() { return slack; }
    public boolean landable() { return landable; }
    /** Reeling while the drag slips: the line twists. */
    public boolean twisting() { return twisting; }
    public boolean faking() { return faking; }
    public int bossPhase() { return bossPhase; }
    public int maxTicks() { return fighter.boss() != null ? BOSS_MAX_TICKS : MAX_TICKS; }
    /** Share of the fight with a tight, well-controlled line, 0..100. */
    public int quality() { return ticks == 0 ? 0 : (int) (100L * goodTicks / ticks); }
}
