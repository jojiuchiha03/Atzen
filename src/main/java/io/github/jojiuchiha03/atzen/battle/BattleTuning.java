package io.github.jojiuchiha03.atzen.battle;

/** Equipment is snapshotted by the server at the start of a battle. */
public record BattleTuning(double strain, double catchSpeed, double surgeStrain) {
    public static final BattleTuning DEFAULT = new BattleTuning(1, 1, 1);

    public BattleTuning {
        if (!Double.isFinite(strain) || !Double.isFinite(catchSpeed) || !Double.isFinite(surgeStrain)
                || strain <= 0 || catchSpeed <= 0 || surgeStrain <= 0) {
            throw new IllegalArgumentException("Battle modifiers must be finite and positive");
        }
    }
}
