package io.github.jojiuchiha03.atzen.fish;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

/**
 * A fish notices a float in the water nearby, swims to it, looks at the bait and — if it likes it — bites. The
 * angler then has the vanilla bite window to strike; the fight is against exactly this fish. A fish that does not
 * care for the bait ignores it; one that is only half keen inspects it and turns away now and then. Without a
 * strike the fish loses interest and swims off.
 */
final class BiteHookGoal extends Goal {
    private static final double RANGE = 12;
    private final AtzenFishEntity fish;
    private FishingHook target;
    private int ticks;
    private int afterBite;
    private int cooldown;
    private int inspect;
    private double appeal;

    BiteHookGoal(AtzenFishEntity fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (fish.isBattleDummy() || fish.specimen().species().boss() != null || --cooldown > 0
                || fish.getRandom().nextInt(reducedTickDelay(20)) != 0) return false;
        target = fish.level().getEntitiesOfClass(FishingHook.class, fish.getBoundingBox().inflate(RANGE), this::interesting)
                .stream().min(Comparator.comparingDouble(fish::distanceToSqr)).orElse(null);
        if (target == null) return false;
        // The bait decides: a liked bait draws the fish in, a disliked one is ignored, a bare hook only sometimes.
        appeal = appeal(target.getPlayerOwner());
        if (appeal < 0.5 && fish.getRandom().nextFloat() > appeal) {
            cooldown = 100;
            return false;
        }
        return Atzen.BATTLES.claimBite(target, fish);
    }

    private boolean interesting(FishingHook hook) {
        return hook.isAlive() && hook.isInWater() && hook.getPlayerOwner() != null && hook.getHookedIn() == null
                && !Atzen.BATTLES.isBattling(hook) && !Atzen.BATTLES.hasBiter(hook);
    }

    private double appeal(Player player) {
        ItemStack rod = player.getMainHandItem().getItem() instanceof FishingRodItem ? player.getMainHandItem() : player.getOffhandItem();
        ItemStack other = rod == player.getMainHandItem() ? player.getOffhandItem() : player.getMainHandItem();
        ItemStack bait = !AtzenRodItem.bait(rod).isEmpty() ? AtzenRodItem.bait(rod) : BattleEquipment.isBait(other) ? other : ItemStack.EMPTY;
        long time = Math.floorMod(fish.level().getOverworldClockTime(), 24000L);
        return BattleEquipment.appeal(bait, rod, fish.specimen().species(), time >= 13000 && time < 23000);
    }

    @Override
    public void start() {
        ticks = 0;
        afterBite = -1;
        inspect = -1;
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
            // A half-keen fish looks at the bait for a moment and may turn away (it can come back later).
            if (inspect < 0) inspect = 20 + fish.getRandom().nextInt(30);
            if (inspect-- > 0) {
                fish.setDeltaMovement((target.getX() - fish.getX()) * 0.05, 0, (target.getZ() - fish.getZ()) * 0.05);
                return;
            }
            if (fish.getRandom().nextDouble() > Math.min(0.95, appeal)) {
                afterBite = 0;
                return;
            }
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
