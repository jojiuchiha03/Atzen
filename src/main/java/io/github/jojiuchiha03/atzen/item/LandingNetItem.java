package io.github.jojiuchiha03.atzen.item;

import java.util.Locale;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A landing net (Anforderung 14), held in the other hand during the fight. Its size decides which fish fit and how
 * wide the opening is for the landing gauge; bosses need the big game net.
 */
public final class LandingNetItem extends Item {
    public enum NetType {
        SMALL(60, 3), CARP(120, 25), BIG_GAME(320, 400);

        private final double lengthCm, maxKg;

        NetType(double lengthCm, double maxKg) {
            this.lengthCm = lengthCm;
            this.maxKg = maxKg;
        }

        public double lengthCm() { return lengthCm; }
        public double maxKg() { return maxKg; }
        public String id() { return name().toLowerCase(Locale.ROOT) + "_landing_net"; }
        public boolean fits(double lengthCm, double weightKg) { return lengthCm <= this.lengthCm && weightKg <= maxKg; }
    }

    private final NetType type;

    public LandingNetItem(Properties properties, NetType type) {
        super(properties);
        this.type = type;
    }

    public NetType type() { return type; }

    public static NetType type(ItemStack stack) {
        return stack.getItem() instanceof LandingNetItem net ? net.type : null;
    }
}
