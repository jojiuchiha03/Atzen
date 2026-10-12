package io.github.jojiuchiha03.atzen.battle;

import java.util.Random;

/**
 * The fight with a hooked fish, simulated by the server in fixed ticks; clients only say what the angler does.
 *
 * <p>It is a small line simulation, not a progress bar. The fish pulls with a force that depends on its size,
 * its stamina and what it is doing. The line carries that force as tension. The reel's drag slips when the
 * tension reaches it, so a fish pulling harder than the drag takes line; the line breaks when the tension
 * exceeds what it bears, which a drag set tighter than the line, a jolt on a stiff rig or a line worn on snags
 * can cause. The angler reels (only against a fish that pulls less than the reel), pumps (lifts the rod to pull
 * the fish closer, then reels while lowering it), sets the drag and steers sideways runs away from cover. A
 * slack line, head shakes and jumps work the hook loose. A fish is landed when it is close and worn out.
 */
public final class FishingBattle {
    public static final int MAX_TICKS = 20 * 60 * 8;
    public static final int BOSS_MAX_TICKS = 20 * 60 * 12;
    public static final int DRAG_STEPS = 10;
    public static final double LANDING_DISTANCE = 4;

    public enum Result { ACTIVE, CAUGHT, SNAPPED, SPOOLED, ESCAPED, TIMEOUT, CANCELLED }

    /** What the angler does this tick. Steer is -1 (left), 0 or 1 (right), seen from the angler. */
    public record Input(boolean reeling, boolean lifting, int steer) {
        public static final Input NONE = new Input(false, false, 0);
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
    private double pumpSlack;
    private double stamina = 1;
    private double hookHold;
    private double condition = 1;
    private double danger;
    private int sideDir = 1;
    private double lift;
    private int drag = 5;
    private int backlash;
    private int bossPhase;
    private boolean faking;
    private boolean fakeDone;
    private boolean explosive;
    private int adaptCooldown;
    private int liftTicks;
    private int reelTicks;
    private boolean slack;
    private boolean twisting;
    private boolean landable;
    private int goodTicks;
    private int ticks;

    public FishingBattle(Fighter fighter, Rig rig, double startDistance, double hookHold, long seed) {
        this.fighter = fighter;
        this.rig = rig;
        this.random = new Random(seed);
        this.distance = Math.clamp(startDistance, 3, 40);
        this.hookHold = Math.clamp(hookHold + rig.hookBonus(), 20, 100);
        this.tension = fighter.power() * 0.5;
        actionTicks = 20 + random.nextInt(20);
        // The strike itself: the fish's first reaction is a run.
        announce(FishAction.RUN, 10);
    }

    /** Hook hold from the strike timing: ticks between the bite and the strike against the fish's ideal moment. */
    public static double strikeHold(int ticksAfterBite, int ideal) {
        double quality = 1 - Math.min(1, Math.abs(ticksAfterBite - ideal) / 25.0);
        return 55 + 45 * quality;
    }

    public void tick(Input input) {
        if (result != Result.ACTIVE) return;
        elapsed++;
        Boss.Phase phase = fighter.boss() == null ? null : fighter.boss().phase(bossPhase);
        if (adaptCooldown > 0) adaptCooldown--;
        if (backlash > 0) backlash--;
        advanceAction(input);

        // the fish's pull
        double strength = fighter.power() * (0.3 + 0.7 * stamina) * (phase == null ? 1 : phase.power());
        double pull = strength * (warning > 0 ? 0.45 : action.pull());
        if (action == FishAction.SHAKE) pull *= 1 + 0.55 * Math.sin(elapsed * 1.3);
        if (action == FishAction.RUN) pull *= rig.runDamping() * (explosive ? 1.8 : 1);
        if (rig.ability() == ReelAbility.FLY && fighter.weight() < 3) pull *= 0.8;
        if (phase != null) pull += fighter.power() * phase.current();

        // the rod: lifting raises it over a second, letting go lowers it faster
        double previousLift = lift;
        lift = input.lifting() ? Math.min(1, lift + 0.06) : Math.max(0, lift - 0.1);
        boolean rising = lift > previousLift, lowering = lift < previousLift;
        boolean reeling = input.reeling() && backlash == 0;
        if (input.lifting()) liftTicks++;
        if (input.reeling()) reelTicks++;
        adapt(input);

        double dragKg = dragKg();
        double flyPump = rig.ability() == ReelAbility.FLY ? 1.6 : 1;
        double target = pull;
        if (reeling) target = pull * 1.15 + 0.3;
        if (rising) target += 0.45 * rig.rodPower() * flyPump;
        if (lowering && !reeling) target *= 0.3;
        if (action == FishAction.SIDE && warning == 0 && input.steer() == -sideDir) target += 0.2 * strength;
        boolean slipping = target > dragKg;

        // line out: a fish that pulls harder than the drag takes line
        boolean away = warning == 0 && (action == FishAction.RUN || action == FishAction.DIVE || action == FishAction.SIDE);
        if (away && pull > dragKg) {
            double out = fighter.speed() * (phase == null ? 1 : phase.speed()) * (explosive ? 1.4 : 1)
                    * Math.clamp((pull - dragKg) / (0.35 * pull), 0, 1);
            if (rig.ability() == ReelAbility.ANCHOR) out *= 0.65;
            distance += out;
            if (rig.ability() == ReelAbility.BAITCASTER && drag <= 3 && backlash == 0 && random.nextInt(60) == 0) {
                backlash = 40;
                shock(0.4 * strength);
            }
        }
        // line in: reeling, pumping, or the current reel on its own
        if (!slipping) {
            if (reeling) {
                double gain = rig.reelSpeed() * Math.clamp((rig.reelForce() - pull) / rig.reelForce(), 0, 1) * (lowering ? 2 : 1);
                double fromSlack = Math.min(pumpSlack, gain * 1.5);
                pumpSlack -= fromSlack;
                distance -= Math.max(0, gain - fromSlack / 1.5);
            }
            if (rising) {
                double power = rig.rodPower() * flyPump;
                double gain = 0.08 * Math.clamp((power + 0.3 * rig.reelForce() - pull) / power, 0, 1);
                distance -= gain;
                pumpSlack += gain;
            }
            if (rig.ability() == ReelAbility.CURRENT && !reeling && (action == FishAction.CALM || action == FishAction.TIRED)) {
                distance -= rig.reelSpeed() * 0.4;
            }
        }
        // Winding against a slipping drag twists the line and wears it down.
        twisting = reeling && slipping;
        if (twisting) condition -= 0.0012 / rig.abrasion();
        distance = Math.max(1.5, distance);
        if (!lowering && !rising && pumpSlack > 0 && !reeling) pumpSlack = Math.max(0, pumpSlack - 0.01);

        // tension follows the target, cushioned by rod and line
        double response = Math.clamp(0.3 + (1 - rig.rodFlex()) * 0.35 + (1 - rig.stretch()) * 0.35, 0.3, 1);
        tension += (Math.min(target, dragKg) - tension) * response;
        if (action == FishAction.JUMP && warning == 0 && actionTicks == 4 && tension > 0.3 * fighter.power()) {
            shock(0.6 * strength); // the fish crashes back into the water on a tight line
        }
        tension = Math.max(0, tension);

        // snags: a sideways run reaches cover unless the angler steers against it
        if (action == FishAction.SIDE && warning == 0) {
            if (input.steer() == -sideDir) danger = Math.max(0, danger - 2.5);
            else danger += 1.4 * (phase == null ? 1 : phase.snags());
            if (danger >= 100) {
                condition -= 0.22 / rig.abrasion();
                danger = 55;
                shock(0.3 * strength);
            }
        } else {
            danger = Math.max(0, danger - 0.5);
        }

        if (tension > breakingLoad() || condition <= 0.05) {
            result = Result.SNAPPED;
            return;
        }
        if (distance + pumpSlack > rig.capacity()) {
            result = Result.SPOOLED;
            return;
        }

        // the hook
        // a rod held up keeps the line tight; a rod dropped without reeling leaves it hanging
        slack = !reeling && !input.lifting() && (tension < 0.12 * fighter.power() || pumpSlack > 0.4);
        double loss = 0;
        if (slack) loss += rig.ability() == ReelAbility.FORGIVING ? 0.4 : 0.7;
        if (action == FishAction.SHAKE && warning == 0) loss += tension > 0.7 * breakingLoad() || slack ? 0.6 : 0.08;
        if (action == FishAction.JUMP && warning == 0 && actionTicks > 4) {
            if (rig.ability() == ReelAbility.JUMP_MASTER && lift < 0.2) loss += 0;
            else if (lift > 0.2 || tension > 0.55 * breakingLoad()) loss += 2.2;
            else if (slack) loss += 1;
            else loss += 0.05;
        }
        hookHold -= loss;
        if (hookHold <= 0) {
            result = Result.ESCAPED;
            return;
        }

        // the fish tires against the line and gets its breath back when the line is light
        double effort = Math.clamp(tension / fighter.power(), 0, 2);
        boolean tide = rig.ability() == ReelAbility.TIDE && fighter.salt();
        double drain = 0.0016 * effort * rig.staminaDrain() / fighter.endurance();
        if (tide) drain *= 1.15;
        if (rig.ability() == ReelAbility.ADAPT) drain *= 1.25;
        if (away && pull <= dragKg) drain *= 1.4; // a run against a drag that holds wears the fish out
        stamina -= drain;
        if (effort < 0.25 && !tide) stamina += 0.0005 / fighter.endurance();
        stamina = Math.clamp(stamina, 0, 1);
        // A boss's strength is split into its phases; at each threshold it changes its behaviour and finds new strength.
        if (fighter.boss() != null && bossPhase < fighter.boss().phases() - 1
                && stamina <= 1 - (bossPhase + 1.0) / fighter.boss().phases()) nextBossPhase();

        ticks++;
        if (!slack && tension >= 0.25 * dragKg && tension <= 0.95 * dragKg) goodTicks++;
        boolean lastPhase = fighter.boss() == null || bossPhase == fighter.boss().phases() - 1;
        landable = lastPhase && distance <= LANDING_DISTANCE && stamina <= landingStamina() && !faking && warning == 0
                && action != FishAction.RUN && action != FishAction.JUMP;
        if (elapsed >= (fighter.boss() != null ? BOSS_MAX_TICKS : MAX_TICKS)) result = Result.TIMEOUT;
    }

    private void advanceAction(Input input) {
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
        int base = fighter.boss() == null ? 18 : fighter.boss().phase(bossPhase).warning();
        if (rig.ability() == ReelAbility.ECHO) base += 20;
        if (rig.ability() == ReelAbility.ADAPT) base += 10;
        return base;
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

    /** The Deep Warden answers the technique the angler leans on: pumping draws head shakes, winching draws runs. */
    private void adapt(Input input) {
        if (fighter.boss() != Boss.DEEP_WARDEN || adaptCooldown > 0 || warning > 0
                || (action != FishAction.CALM && action != FishAction.TIRED)) return;
        int total = liftTicks + reelTicks;
        if (total < 60) return;
        if (input.lifting() && liftTicks > total * 0.5) {
            announce(FishAction.SHAKE, 4);
            adaptCooldown = 100;
        } else if (input.reeling() && reelTicks > total * 0.75) {
            announce(FishAction.RUN, 4);
            adaptCooldown = 100;
        }
    }

    private void nextBossPhase() {
        bossPhase++;
        stamina = Math.min(1, stamina + 0.12);
        liftTicks = 0;
        reelTicks = 0;
        announce(FishAction.RUN, 8);
    }

    private void shock(double kg) {
        double cushion = 1 - 0.5 * rig.rodFlex() - 0.6 * rig.stretch();
        if (rig.ability() == ReelAbility.FINE_DRAG) cushion *= 0.55;
        tension += kg * Math.max(0.15, cushion);
        double dragKg = dragKg();
        if (tension > dragKg) tension = dragKg + (tension - dragKg) * 0.5;
    }

    private double landingStamina() { return fighter.boss() != null ? 0.08 : 0.15; }

    /**
     * Landing by hand, the only way until the landing net arrives: works on a close, worn-out fish; a heavy fish
     * or one with fight left in it can kick free and run once more.
     */
    public boolean land() {
        if (result != Result.ACTIVE || !landable) return false;
        double chance = Math.clamp(0.95 - fighter.weight() * 0.003 - stamina * 1.5, 0.3, 0.95);
        if (fighter.boss() != null) chance = Math.min(chance, 0.6);
        if (random.nextDouble() < chance) {
            result = Result.CAUGHT;
            return true;
        }
        stamina = Math.min(1, stamina + 0.12);
        start(FishAction.RUN);
        landable = false;
        return false;
    }

    public void adjustDrag(int delta) { drag = Math.clamp(drag + Integer.signum(delta), 1, DRAG_STEPS); }

    public void cancel() { if (result == Result.ACTIVE) result = Result.CANCELLED; }

    public double dragKg() {
        double kg = drag / (double) DRAG_STEPS * rig.lineStrength() * rig.dragLimit();
        return rig.ability() == ReelAbility.AUTO_DRAG ? Math.min(kg, 0.9 * breakingLoad()) : kg;
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
    public double distance() { return distance; }
    public double lineOut() { return distance + pumpSlack; }
    public double stamina() { return stamina; }
    public double hookHold() { return hookHold; }
    public double condition() { return condition; }
    public double danger() { return danger; }
    public int sideDir() { return sideDir; }
    public double lift() { return lift; }
    public int drag() { return drag; }
    public boolean slack() { return slack; }
    public boolean landable() { return landable; }
    public boolean tangled() { return backlash > 0; }
    /** Reeling while the drag slips: the line twists. */
    public boolean twisting() { return twisting; }
    public boolean faking() { return faking; }
    public int bossPhase() { return bossPhase; }
    public int maxTicks() { return fighter.boss() != null ? BOSS_MAX_TICKS : MAX_TICKS; }
    /** Share of the fight with a tight, well-controlled line, 0..100. */
    public int quality() { return ticks == 0 ? 0 : (int) (100L * goodTicks / ticks); }
}
