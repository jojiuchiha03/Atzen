package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.battle.BattleTuning;

public record FishSpecimen(FishSpecies species, double lengthCm, double weightKg) {
    public FishSpecimen {
        if (species == null || !Double.isFinite(lengthCm) || !Double.isFinite(weightKg)
                || lengthCm < species.minLength() || lengthCm > species.maxLength() || weightKg <= 0) {
            throw new IllegalArgumentException("Invalid fish measurements");
        }
    }

    public double sizeFraction() { return (lengthCm - species.minLength()) / (species.maxLength() - species.minLength()); }
    public String sizeKey() {
        return "size.atzen." + (sizeFraction() >= 0.85 ? "trophy" : sizeFraction() >= 0.60 ? "large"
                : sizeFraction() >= 0.25 ? "normal" : "small");
    }
    public BattleTuning tune(BattleTuning gear) {
        return new BattleTuning(gear.strain() * (1 + sizeFraction() * 0.35),
                gear.catchSpeed() * (1 - sizeFraction() * 0.25), gear.surgeStrain());
    }
}
