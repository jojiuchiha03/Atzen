package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

/**
 * A fish notices a float in the water nearby, swims to it and bites. The angler then has the vanilla bite window
 * to strike; the fight is against exactly this fish. Without a strike the fish loses interest and swims off.
 */
final class BiteHookGoal extends Goal {
    private static final double RANGE = 12;
    private final AtzenFishEntity fish;
    private FishingHook target;
    private int ticks;
    private int afterBite;
    private int cooldown;

    BiteHookGoal(AtzenFishEntity fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (fish.isBattleDummy() || --cooldown > 0 || fish.getRandom().nextInt(reducedTickDelay(20)) != 0) return false;
        target = fish.level().getEntitiesOfClass(FishingHook.class, fish.getBoundingBox().inflate(RANGE), this::interesting)
                .stream().min(Comparator.comparingDouble(fish::distanceToSqr)).orElse(null);
        if (target == null) return false;
        // Bait in the rod's slot (or the other hand) makes a bite a sure thing, a bare hook only sometimes.
        boolean baited = hasBait(target.getPlayerOwner());
        if (!baited && fish.getRandom().nextFloat() > 0.45f) {
            cooldown = 100;
            return false;
        }
        return Atzen.BATTLES.claimBite(target, fish);
    }

    private boolean interesting(FishingHook hook) {
        return hook.isAlive() && hook.isInWater() && hook.getPlayerOwner() != null && hook.getHookedIn() == null
                && !Atzen.BATTLES.isBattling(hook) && !Atzen.BATTLES.hasBiter(hook);
    }

    private static boolean hasBait(Player player) {
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (!AtzenRodItem.bait(stack).isEmpty() || BattleEquipment.isBait(stack)) return true;
        }
        return false;
    }

    @Override
    public void start() {
        ticks = 0;
        afterBite = -1;
        fish.getNavigation().moveTo(target.getX(), target.getY() - 0.4, target.getZ(), 1.4);
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && target.isInWater() && !fish.isBattleDummy() && ticks < 400
                && afterBite != 0 && fish.distanceToSqr(target) < (RANGE + 4) * (RANGE + 4) && !Atzen.BATTLES.isBattling(target);
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        ticks++;
        fish.getLookControl().setLookAt(target.getX(), target.getY(), target.getZ());
        double distance = fish.distanceToSqr(target.getX(), target.getY() - 0.4, target.getZ());
        if (afterBite < 0) {
            if (distance > 1.6) {
                if (ticks % 10 == 0) fish.getNavigation().moveTo(target.getX(), target.getY() - 0.4, target.getZ(), 1.4);
                return;
            }
            fish.getNavigation().stop();
            ((BiteTarget) target).atzen$bite(40);
            afterBite = 45;
        } else {
            // Hold on just under the float while the angler can strike.
            fish.setDeltaMovement((target.getX() - fish.getX()) * 0.3, (target.getY() - 0.45 - fish.getY()) * 0.15,
                    (target.getZ() - fish.getZ()) * 0.3);
            afterBite--;
        }
    }

    @Override
    public void stop() {
        if (target != null) Atzen.BATTLES.releaseBite(target, fish);
        target = null;
        cooldown = 200 + fish.getRandom().nextInt(200);
    }
}
