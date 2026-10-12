package io.github.jojiuchiha03.atzen.battle;

import com.mojang.brigadier.CommandDispatcher;
import io.github.jojiuchiha03.atzen.AtzenSounds;
import io.github.jojiuchiha03.atzen.Performance;
import io.github.jojiuchiha03.atzen.ecology.Ecology;
import io.github.jojiuchiha03.atzen.item.FishingTools;
import io.github.jojiuchiha03.atzen.item.LandingNetItem;
import io.github.jojiuchiha03.atzen.item.ModuleType;
import io.github.jojiuchiha03.atzen.item.RodMods;
import io.github.jojiuchiha03.atzen.progress.Area;
import io.github.jojiuchiha03.atzen.progress.Progression;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import io.github.jojiuchiha03.atzen.fish.BiteTarget;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.BaitType;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import io.github.jojiuchiha03.atzen.item.GearSlot;
import io.github.jojiuchiha03.atzen.item.HookType;
import io.github.jojiuchiha03.atzen.item.RodType;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import io.github.jojiuchiha03.atzen.network.ReelPayload;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Runs the fights on the server: starts one when an angler strikes a bite, feeds it the angler's input every
 * tick, moves the fish everyone sees on the line, and hands out the catch.
 */
public final class BattleManager {
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Session> completing = new HashMap<>();
    /** Fish swimming to or biting a float, by float UUID (see BiteHookGoal). */
    private final Map<UUID, AtzenFishEntity> biters = new HashMap<>();
    private final BossEncounters bosses = new BossEncounters();
    private int nextId;

    public boolean interceptRetrieve(FishingHook hook, ItemStack rod, int nibble) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player) || completing.containsKey(hook.getUUID())) return false;
        Session active = sessions.get(player.getUUID());
        if (active != null) {
            // Right-click during a fight: land the fish if it is close and worn out.
            if (active.hook == hook && active.battle.landable()) land(active);
            return active.hook == hook;
        }
        AtzenFishEntity liveFish = hook.getHookedIn() instanceof AtzenFishEntity fish ? fish : null;
        if (liveFish == null && nibble > 0) {
            // A fish from the world bit this float: the fight is against that very fish.
            AtzenFishEntity biter = biters.get(hook.getUUID());
            if (biter != null && biter.isAlive() && !biter.isBattleDummy() && biter.distanceToSqr(hook) < 9) liveFish = biter;
        }
        if ((liveFish == null && (nibble <= 0 || hook.getHookedIn() != null))
                || (liveFish != null && !liveFish.isAlive()) || !ServerPlayNetworking.canSend(player, BattlePayload.TYPE)) return false;
        InteractionHand hand = player.getMainHandItem() == rod ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (player.getItemInHand(hand) != rod) return false;
        // Bait loaded into the rod's slot comes first; bait held in the other hand still works.
        ItemStack loaded = AtzenRodItem.bait(rod);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ItemStack bait = !loaded.isEmpty() ? loaded : BattleEquipment.isBait(other) ? other : ItemStack.EMPTY;
        BaitType baitType = AtzenItems.bait(bait);
        ServerLevel level = (ServerLevel) player.level();
        Random random = new Random(player.getRandom().nextLong());

        Boss boss = liveFish == null ? bosses.check(level, hook.blockPosition(), baitType, player.getUUID(), random) : null;
        FishSpecimen specimen = liveFish != null ? liveFish.specimen()
                : boss != null ? FishSpecies.of(boss).roll(random) : choose(player, level, hook.blockPosition(), rod, bait, random);
        Fighter fighter = specimen.fighter();
        int sinceBite = liveFish != null && nibble <= 0 ? fighter.strikeIdeal() : ((BiteTarget) hook).atzen$ticksSinceBite();
        double hold = FishingBattle.strikeHold(sinceBite, fighter.strikeIdeal(), BattleEquipment.sensitivity(player, rod));
        if (!loaded.isEmpty()) consumeBait(player, rod);
        else if (!bait.isEmpty() && !player.hasInfiniteMaterials()) {
            ItemStack used = AtzenRodItem.use(bait);
            player.setItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, used);
        }
        Session session = begin(player, hook, rod, hand, bait, baitType, specimen, liveFish, hold, hook.position(), random);
        biters.remove(hook.getUUID());
        if (liveFish == null) spawnDummy(session);
        else {
            liveFish.getNavigation().stop();
            liveFish.setNoAi(true);
            session.dummy = liveFish;
        }
        if (boss != null) {
            player.sendSystemMessage(Component.translatable("message.atzen.boss_hooked",
                    Component.translatable(boss.translationKey())).withStyle(ChatFormatting.GOLD));
        }
        send(session);
        return true;
    }

    /**
     * Which fish bites: habitat, bait appeal and the time of day; a fished-out spot gives fewer and smaller fish,
     * a ground not yet unlocked only common ones, Luck of the Sea and groundbait help.
     */
    private FishSpecimen choose(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack rod, ItemStack bait, Random random) {
        boolean night = night(level);
        boolean raining = level.isRainingAt(pos);
        long time = level.getOverworldClockTime();
        Habitat habitat = Habitat.at(level, pos);
        boolean open = Area.of(habitat).open(Progression.level(player));
        int luck = BattleEquipment.luck(player, rod);
        boolean chum = Ecology.nearChum(level, pos) || RodMods.has(rod, ModuleType.FEEDER);
        double pressure = Ecology.factor(level, pos);
        FishSpecies species = FishSpecies.choose(habitat, random, candidate -> {
            double appeal = BattleEquipment.appeal(bait, rod, candidate, night);
            if (candidate.boss() != null || appeal < 0.5) return 0;
            int rarity = candidate.rarity().ordinal();
            if (!open && rarity >= FishSpecies.Rarity.RARE.ordinal()) return 0;
            double weight = appeal * candidate.activity(time, raining);
            if (rarity >= FishSpecies.Rarity.RARE.ordinal()) weight *= (1 + 0.25 * luck) * pressure;
            if (chum && candidate.diet() == FishSpecies.Diet.COARSE) weight *= 1.5;
            return weight;
        });
        BaitType baitType = AtzenItems.bait(bait);
        double bias = (baitType == null ? 0 : baitType.sizeBias()) - (1 - pressure) * 0.6;
        return species.roll(random, bias);
    }

    /** Uses one bait from the rod's slot; an empty slot is refilled from a bait box. */
    private static void consumeBait(ServerPlayer player, ItemStack rod) {
        BaitType before = AtzenItems.bait(AtzenRodItem.bait(rod));
        AtzenRodItem.consumeBait(rod, player);
        FishingTools.BaitBox.refill(player, rod, before);
    }

    /** Builds the fight and the session (shared by the float and the rod holder). */
    private Session begin(ServerPlayer player, FishingHook hook, ItemStack rod, InteractionHand hand, ItemStack bait, BaitType baitType,
                          FishSpecimen specimen, AtzenFishEntity liveFish, double hold, Vec3 anchor, Random random) {
        Fighter fighter = specimen.fighter();
        if (baitType == BaitType.POPPER && fighter.boss() == null) {
            // A popper on the surface: hooked fish come up and jump more.
            int[] weights = fighter.weights().clone();
            weights[FishAction.JUMP.ordinal()] += 3;
            fighter = new Fighter(fighter.power(), fighter.endurance(), fighter.speed(), fighter.weight(), weights,
                    fighter.strikeIdeal(), fighter.salt(), fighter.teeth(), null);
        }
        RodType rodType = BattleEquipment.rodType(rod);
        // A circle hook sets itself; a fish far too small for the rod is hardly felt.
        if (AtzenItems.hook(AtzenRodItem.get(rod, GearSlot.HOOK)) == HookType.CIRCLE) hold = Math.max(hold, 85);
        if (fighter.weight() < rodType.fishMin()) hold -= 15;
        Rig rig = BattleEquipment.rig(player, rod, bait, fighter.boss() != null);
        FishingBattle battle = new FishingBattle(fighter, rig, Math.sqrt(anchor.distanceToSqr(player.position())), hold, random.nextLong());
        Session session = new Session(++nextId, player, hook, rod, hand, battle, specimen, liveFish, anchor, false);
        session.strike = (int) Math.round(hold);
        sessions.put(player.getUUID(), session);
        return session;
    }

    /**
     * A bite at a rod holder taken up: the rod is in the angler's hand now, the hook sets itself (more or less
     * well), and the fight runs without a float. Returns false if a fight could not start.
     */
    public boolean startFromHolder(ServerPlayer player, ItemStack rod, Vec3 target, boolean sensor) {
        if (sessions.containsKey(player.getUUID()) || player.fishing != null || !ServerPlayNetworking.canSend(player, BattlePayload.TYPE)) return false;
        ServerLevel level = (ServerLevel) player.level();
        Random random = new Random(player.getRandom().nextLong());
        ItemStack bait = AtzenRodItem.bait(rod);
        BaitType baitType = AtzenItems.bait(bait);
        Boss boss = bosses.check(level, BlockPos.containing(target), baitType, player.getUUID(), random);
        FishSpecimen specimen = boss != null ? FishSpecies.of(boss).roll(random) : choose(player, level, BlockPos.containing(target), rod, bait, random);
        double hold = (sensor ? 80 : 65) + random.nextInt(15);
        if (!bait.isEmpty()) consumeBait(player, rod);
        Session session = begin(player, null, rod, InteractionHand.MAIN_HAND, bait, baitType, specimen, null, hold, target, random);
        spawnDummy(session);
        if (boss != null) {
            player.sendSystemMessage(Component.translatable("message.atzen.boss_hooked",
                    Component.translatable(boss.translationKey())).withStyle(ChatFormatting.GOLD));
        }
        send(session);
        return true;
    }

    /** A click with the rod during a fight without a float (rod holder): land the fish. True if it was used. */
    public boolean click(ServerPlayer player) {
        Session session = sessions.get(player.getUUID());
        if (session == null || session.hook != null || session.practice) return false;
        if (session.battle.landable()) land(session);
        return true;
    }

    private static boolean night(ServerLevel level) {
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return time >= 13000 && time < 23000;
    }

    /**
     * Landing (Anforderung 14): with a net in the other hand the gauge decides – a scoop with the fish inside the
     * opening almost always works. Without a net only small fish (up to 2 kg) can be taken by hand; bosses need the
     * big game net.
     */
    private void land(Session session) {
        ServerPlayer player = session.player;
        FishSpecimen fish = session.specimen;
        boolean boss = fish.species().boss() != null;
        ItemStack net = netOf(session);
        LandingNetItem.NetType type = LandingNetItem.type(net);
        boolean landed;
        if (type == null) {
            if (fish.weightKg() > HAND_LIMIT_KG || boss) {
                player.sendSystemMessage(Component.translatable("message.atzen.need_net").withStyle(ChatFormatting.GOLD), true);
                return;
            }
            landed = session.battle.land();
            session.handLanded = true;
        } else {
            if (!type.fits(fish.lengthCm(), fish.weightKg()) || (boss && type != LandingNetItem.NetType.BIG_GAME)) {
                player.sendSystemMessage(Component.translatable("message.atzen.net_too_small").withStyle(ChatFormatting.GOLD), true);
                return;
            }
            boolean aligned = Landing.aligned(session.battle.landableTicks(), Landing.window(type.lengthCm(), fish.lengthCm()));
            landed = session.battle.land(Landing.chance(aligned, session.battle.stamina(), boss));
            InteractionHand netHand = session.hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            net.hurtAndBreak(1, player, netHand);
            player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.7f, 0.8f);
        }
        if (landed) return;
        // It kicked free and runs again.
        if (player.level() instanceof ServerLevel level && session.dummy != null) {
            level.sendParticles(ParticleTypes.SPLASH, session.dummy.getX(), session.surface + 0.1, session.dummy.getZ(),
                    30, 0.4, 0.2, 0.4, 0.2);
            level.playSound(null, session.dummy.blockPosition(), AtzenSounds.LEAP, SoundSource.NEUTRAL, 1.0f, 0.9f);
        }
        player.sendSystemMessage(Component.translatable("message.atzen.landing_failed").withStyle(ChatFormatting.RED), true);
    }

    /** Fish up to this weight can be lifted out by hand. */
    public static final double HAND_LIMIT_KG = 2;

    private static ItemStack netOf(Session session) {
        return session.player.getItemInHand(session.hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /** Half-width of the net's opening for the gauge, -1 without a fitting net. */
    private static float netWindow(Session session) {
        LandingNetItem.NetType type = LandingNetItem.type(netOf(session));
        if (type == null || !type.fits(session.specimen.lengthCm(), session.specimen.weightKg())) return -1;
        return (float) Landing.window(type.lengthCm(), session.specimen.lengthCm());
    }

    /** The fish everyone sees on the line: a non-saved, invulnerable copy of the specimen that is being fought. */
    private void spawnDummy(Session session) {
        if (!(session.player.level() instanceof ServerLevel level)) return;
        AtzenFishEntity dummy = AtzenFishRegistry.entity(session.specimen.species()).create(level, EntitySpawnReason.TRIGGERED);
        if (dummy == null) return;
        dummy.becomeBattleDummy(session.specimen);
        dummy.snapTo(session.anchor.x, session.surface - 0.5, session.anchor.z, session.player.getYRot(), 0);
        session.dummy = dummy;
        session.fishPos = dummy.position();
        level.addFreshEntity(dummy);
        level.sendParticles(ParticleTypes.SPLASH, session.anchor.x, session.surface + 0.1, session.anchor.z, 14, 0.3, 0.1, 0.3, 0.12);
    }

    public boolean ownsDummy(AtzenFishEntity fish) {
        for (Session session : sessions.values()) if (session.dummy == fish && session.liveFish == null) return true;
        return false;
    }

    public boolean claimBite(FishingHook hook, AtzenFishEntity fish) {
        AtzenFishEntity current = biters.get(hook.getUUID());
        if (current != null && current != fish && current.isAlive()) return false;
        biters.put(hook.getUUID(), fish);
        return true;
    }

    public void releaseBite(FishingHook hook, AtzenFishEntity fish) {
        biters.remove(hook.getUUID(), fish);
    }

    public boolean hasBiter(FishingHook hook) {
        AtzenFishEntity fish = biters.get(hook.getUUID());
        if (fish != null && !fish.isAlive()) biters.remove(hook.getUUID());
        return fish != null && fish.isAlive();
    }

    /**
     * Server-side choreography, visible to every player: the fish sits where the line says it is (the line out,
     * off to the side in a sideways run, deep in a dive), thrashes before an action, leaps in a jump, lies flat
     * at the surface when worn out. The float follows the fish so the line moves with it.
     */
    private void animate(Session session) {
        if (session.practice || !(session.player.level() instanceof ServerLevel level)) return;
        FishingBattle battle = session.battle;
        int t = battle.elapsed();
        FishingBattle.Input input = session.input();
        if (input.reeling() && t % 7 == 0) session.player.swing(session.hand, true);
        if (session.dummy == null || session.dummy.isRemoved()) return;

        Vec3 eye = session.player.position();
        Vec3 dir = session.direction;
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        FishAction action = battle.action();
        boolean warning = battle.pending() != null;
        double lateralTarget = action == FishAction.SIDE && !warning ? battle.sideDir() * (1 + battle.danger() / 100 * 4) : 0;
        session.lateral += (lateralTarget - session.lateral) * 0.08;
        double reach = Math.max(1.2, Math.sqrt(Math.max(0, battle.distance() * battle.distance() - 2)));
        Vec3 flat = eye.add(dir.scale(reach)).add(side.scale(session.lateral));
        double depth = switch (action) {
            case DIVE -> warning ? 0.6 : Math.min(3, 0.6 + (t % 200) / 40.0);
            case TIRED -> 0.15;
            case CALM -> 0.6;
            default -> 0.5;
        };
        double y = session.surface - depth;
        float pitch = 0;
        if (action == FishAction.JUMP && !warning) {
            int left = 22 - (t - session.actionStart);
            double arc = Math.sin(Math.clamp((22 - left) / 18.0, 0, 1) * Math.PI);
            y = session.surface - 0.3 + arc * 1.6;
            pitch = (float) ((left - 11) * -8.0);
            if (left == 21 || left == 4) {
                level.sendParticles(ParticleTypes.SPLASH, flat.x, session.surface + 0.1, flat.z, 24, 0.4, 0.1, 0.4, 0.2);
                if (left == 21) level.playSound(null, flat.x, session.surface, flat.z, AtzenSounds.LEAP, SoundSource.NEUTRAL, 0.9f,
                        0.9f + session.player.getRandom().nextFloat() * 0.2f);
            }
        }
        if (warning) {
            flat = flat.add((session.player.getRandom().nextDouble() - 0.5) * 0.3, 0, (session.player.getRandom().nextDouble() - 0.5) * 0.3);
            if (t % 3 == 0) level.sendParticles(ParticleTypes.SPLASH, flat.x, session.surface + 0.1, flat.z, 4, 0.2, 0.05, 0.2, 0.08);
        } else if (action == FishAction.RUN && t % 4 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, flat.x, y + 0.2, flat.z, 3, 0.2, 0.05, 0.2, 0.02);
        } else if (action == FishAction.SHAKE && t % 3 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, flat.x, session.surface + 0.1, flat.z, 3, 0.15, 0.05, 0.15, 0.05);
        }
        // Only over water: at the bank the fish stays where it last could swim.
        if (level.getFluidState(BlockPos.containing(flat.x, session.surface - 0.3, flat.z)).is(FluidTags.WATER)) {
            session.fishPos = session.fishPos.lerp(new Vec3(flat.x, y, flat.z), 0.3);
        } else {
            session.fishPos = new Vec3(session.fishPos.x, Math.max(y, session.fishPos.y), session.fishPos.z);
        }
        Vec3 heading = session.fishPos.subtract(session.dummy.position());
        float yaw = heading.horizontalDistanceSqr() > 1e-4 ? (float) Math.toDegrees(Math.atan2(-heading.x, heading.z)) : session.dummy.getYRot();
        if (action == FishAction.SHAKE && !warning) yaw += (float) Math.sin(t * 1.3) * 35;
        session.dummy.snapTo(session.fishPos.x, session.fishPos.y, session.fishPos.z, yaw, pitch);
        session.dummy.setYHeadRot(yaw);
        session.dummy.setYBodyRot(yaw);
        session.dummy.setDeltaMovement(Vec3.ZERO);
        if (session.hook == null) {
            // No float from a rod holder: a thin trail of bubbles shows the line.
            if (t % 5 == 0) level.sendParticles(ParticleTypes.BUBBLE, session.fishPos.x, session.surface, session.fishPos.z, 1, 0, 0, 0, 0);
            return;
        }
        session.hook.setPos(session.fishPos.x, Math.max(session.surface - 0.1, session.fishPos.y + 0.3), session.fishPos.z);
        session.hook.setDeltaMovement(Vec3.ZERO);
    }

    private static void removeDummy(Session session) {
        if (session.dummy == null || session.dummy.isRemoved()) return;
        // A real fish from the world swims on (unless it was caught, then finish() removes it); a dummy vanishes.
        if (session.dummy == session.liveFish) session.dummy.setNoAi(false);
        else session.dummy.discard();
    }

    public boolean isBattling(FishingHook hook) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player)) return false;
        Session session = sessions.get(player.getUUID());
        return session != null && session.hook == hook;
    }

    public void input(ServerPlayer player, ReelPayload payload) {
        Session session = sessions.get(player.getUUID());
        if (session == null || session.player != player || session.id != payload.battleId()) return;
        if (payload.cancel()) session.battle.cancel();
        // Updating intent never advances the simulation; packet spam cannot speed up a catch.
        session.reeling = payload.reeling();
        session.lifting = payload.lifting();
        session.steer = Math.clamp(payload.steer(), -1, 1);
        if (payload.dragDelta() != 0) session.battle.adjustDrag(payload.dragDelta());
        session.inputAge = 0;
    }

    public void tick(MinecraftServer server) {
        for (Session session : List.copyOf(sessions.values())) {
            ServerPlayer player = session.player;
            if (player.hasDisconnected() || server.getPlayerList().getPlayer(player.getUUID()) != player) {
                disconnect(player);
                continue;
            }
            if (!player.isAlive() || player.isSpectator()
                    || (!session.practice && session.hook == null && (player.getItemInHand(session.hand) != session.rod
                            || player.distanceToSqr(session.anchor) > 4096 || player.fishing != null))
                    || (session.hook != null &&
                    (session.hook.isRemoved() || player.fishing != session.hook
                            || session.hook.level() != player.level() || session.hook.distanceToSqr(player) > 4096
                            || player.getItemInHand(session.hand) != session.rod || session.rod.isEmpty()
                            || (session.liveFish != null && (!session.liveFish.isAlive() || session.liveFish.isRemoved()))))
                    || (session.practice && player.fishing != null)) {
                session.battle.cancel();
            }
            FishAction previous = session.battle.action();
            FishAction previousPending = session.battle.pending();
            session.inputAge++;
            session.battle.tick(session.input());
            // Training has no float to right-click: a worn-out fish close in is landed by hand at once.
            if (session.practice && session.battle.landable()) session.battle.land();
            if (session.battle.action() != previous) session.actionStart = session.battle.elapsed();
            if (session.battle.result() != FishingBattle.Result.ACTIVE) {
                finish(session);
                continue;
            }
            animate(session);
            if (session.battle.elapsed() % 2 == 0 || previous != session.battle.action()
                    || previousPending != session.battle.pending()) send(session);
        }
    }

    private void finish(Session session) {
        // Remove before giving loot, and guard re-entry through the retrieve mixin.
        sessions.remove(session.player.getUUID());
        FishingBattle.Result result = session.battle.result();
        boolean caught = result == FishingBattle.Result.CAUGHT;
        ServerPlayer player = session.player;
        if (session.dummy != null && player.level() instanceof ServerLevel level) {
            level.sendParticles(caught ? ParticleTypes.SPLASH : ParticleTypes.BUBBLE_POP, session.dummy.getX(),
                    session.surface + 0.1, session.dummy.getZ(), caught ? 30 : 12, 0.4, 0.2, 0.4, 0.2);
        }
        removeDummy(session);
        send(session);
        if (!session.practice) {
            Boss boss = session.specimen.species().boss();
            if (caught && (session.hook == null || !session.hook.isRemoved())) {
                if (session.hook != null) completing.put(session.hook.getUUID(), session);
                try {
                    int damage;
                    if (session.hook == null || session.liveFish != null || boss != null) {
                        player.spawnAtLocation(player.level(), caughtItem(session));
                        player.awardStat(Stats.FISH_CAUGHT, 1);
                        player.giveExperiencePoints(boss != null ? 60 : 1 + player.getRandom().nextInt(6));
                        if (session.liveFish != null) session.liveFish.discard();
                        if (session.hook != null) session.hook.discard();
                        damage = boss != null ? 5 : 1;
                    } else {
                        damage = session.hook.retrieve(session.rod);
                    }
                    if (player.level() instanceof ServerLevel level) {
                        BlockPos where = BlockPos.containing(session.anchor);
                        Ecology.caught(level, where, false);
                        Progression.caught(player, session.specimen, session.battle.quality(), Habitat.at(level, where));
                        if (boss != null) {
                            player.spawnAtLocation(level, new ItemStack(trophy(boss)));
                            bosses.caught(level, player.getUUID(), boss);
                            player.sendSystemMessage(Component.translatable("message.atzen.boss_caught",
                                    Component.translatable(boss.translationKey())).withStyle(ChatFormatting.GOLD));
                        }
                    }
                    // an overloaded rod wears faster
                    session.rod.hurtAndBreak(damage + session.battle.overloadTicks() / 20, player, session.hand);
                } finally {
                    if (session.hook != null) completing.remove(session.hook.getUUID());
                }
            } else {
                if (session.hook != null) session.hook.discard();
                if (result == FishingBattle.Result.SNAPPED || result == FishingBattle.Result.SPOOLED
                        || result == FishingBattle.Result.BITTEN) {
                    AtzenRodItem.loseLure(session.rod);
                }
                if (result == FishingBattle.Result.ROD_BROKEN) {
                    // The blank cracks: a quarter of its life is gone.
                    session.rod.hurtAndBreak(Math.max(1, session.rod.getMaxDamage() / 4), player, session.hand);
                } else if (result != FishingBattle.Result.CANCELLED) {
                    session.rod.hurtAndBreak(1, player, session.hand);
                }
            }
        }
        // The outcome for everyone around: the catch, a snapping line, a fish that got away.
        var outcome = switch (result) {
            case CAUGHT -> AtzenSounds.CATCH;
            case SNAPPED, SPOOLED, BITTEN, ROD_BROKEN -> AtzenSounds.SNAP;
            case ESCAPED, TIMEOUT -> AtzenSounds.ESCAPE;
            default -> SoundEvents.FISHING_BOBBER_RETRIEVE;
        };
        player.level().playSound(null, player.blockPosition(), outcome, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private static Item trophy(Boss boss) {
        return switch (boss) {
            case ANCIENT_CATFISH -> AtzenItems.ANCIENT_BARBEL;
            case REED_KING -> AtzenItems.REED_CROWN;
            case BLACK_STURGEON -> AtzenItems.STURGEON_PLATE;
            case DEEP_WARDEN -> AtzenItems.WARDEN_HEART;
        };
    }

    private void send(Session session) {
        if (ServerPlayNetworking.canSend(session.player, BattlePayload.TYPE)) {
            ServerPlayNetworking.send(session.player, BattlePayload.of(session.id, session.battle, session.practice,
                    session.specimen, session.dummy == null ? -1 : session.dummy.getId(), session.strike, netWindow(session)));
        }
    }

    public ObjectArrayList<ItemStack> decorateLoot(FishingHook hook, ObjectArrayList<ItemStack> original) {
        Session session = completing.get(hook.getUUID());
        if (session == null || session.specimen == null) return original;
        // Only replace the fish slot. Vanilla treasure, junk, luck and XP remain intact.
        var result = new ObjectArrayList<ItemStack>(original.size());
        for (ItemStack stack : original) result.add(stack.is(ItemTags.FISHES) ? caughtItem(session) : stack);
        return result;
    }

    /**
     * The landed fish as an item: a fish from the world keeps its identity and health; a long fight and lifting it
     * out by hand stress it.
     */
    private ItemStack caughtItem(Session session) {
        UUID id = session.liveFish != null ? session.liveFish.identity() : UUID.randomUUID();
        double condition = session.liveFish != null ? session.liveFish.condition() : 100;
        condition -= Math.min(40, session.battle.elapsed() / 60.0) + (session.handLanded ? 10 : 0);
        return FishCatch.create(session.specimen, (ServerLevel) session.player.level(), BlockPos.containing(session.anchor),
                session.battle.quality(), session.player.getName().getString(), id, Math.max(5, condition));
    }

    public void disconnect(ServerPlayer player) {
        Session session = sessions.remove(player.getUUID());
        if (session != null) removeDummy(session);
        if (session != null && session.hook != null) session.hook.discard();
    }

    public void clear() {
        sessions.values().forEach(session -> {
            removeDummy(session);
            if (session.hook != null) session.hook.discard();
        });
        sessions.clear();
        completing.clear();
        biters.clear();
    }

    private int practice(ServerPlayer player, FishSpecies species) {
        if (!ServerPlayNetworking.canSend(player, BattlePayload.TYPE)) {
            player.sendSystemMessage(Component.translatable("message.atzen.client_required"));
            return 0;
        }
        if (sessions.containsKey(player.getUUID()) || player.fishing != null || !player.isAlive() || player.isSpectator()) {
            player.sendSystemMessage(Component.translatable("message.atzen.already_fishing"));
            return 0;
        }
        Random random = new Random(player.getRandom().nextLong());
        FishSpecimen specimen = species.roll(random);
        // Training runs on the tackle in hand, so gear can be tried out.
        ItemStack rod = player.getMainHandItem();
        Rig rig = rod.getItem() instanceof AtzenRodItem ? BattleEquipment.rig(player, rod, AtzenRodItem.bait(rod)) : Rig.VANILLA;
        Session session = new Session(++nextId, player, null, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new FishingBattle(specimen.fighter(), rig, 12, 85, random.nextLong()), specimen, null, player.position(), true);
        session.strike = 85;
        sessions.put(player.getUUID(), session);
        send(session);
        return 1;
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var practice = Commands.literal("practice")
                .executes(context -> practice(context.getSource().getPlayerOrException(), FishSpecies.PIKE));
        for (FishSpecies species : List.of(FishSpecies.ROACH, FishSpecies.RIVER_TROUT, FishSpecies.PIKE, FishSpecies.CARP,
                FishSpecies.CATFISH, FishSpecies.TUNA, FishSpecies.ANCIENT_CATFISH, FishSpecies.REED_KING,
                FishSpecies.BLACK_STURGEON, FishSpecies.DEEP_WARDEN)) {
            practice.then(Commands.literal(species.id().toLowerCase(Locale.ROOT))
                    .executes(context -> practice(context.getSource().getPlayerOrException(), species)));
        }
        dispatcher.register(Commands.literal("atzen")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.translatable("message.atzen.help"), false);
                    return 1;
                })
                .then(practice)
                .then(Commands.literal("cancel").executes(context -> {
                    Session session = sessions.get(context.getSource().getPlayerOrException().getUUID());
                    if (session == null) return 0;
                    session.battle.cancel();
                    return 1;
                }))
                .then(Commands.literal("lexikon").executes(context -> {
                    Progression.openLexicon(context.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("turnier").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("start").then(Commands.argument("minuten", IntegerArgumentType.integer(1, 240))
                                .executes(context -> tournament(context.getSource(), IntegerArgumentType.getInteger(context, "minuten"), ""))
                                .then(Commands.argument("art", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (FishSpecies species : FishSpecies.values()) if (species.boss() == null) builder.suggest(species.id());
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> tournament(context.getSource(), IntegerArgumentType.getInteger(context, "minuten"),
                                                StringArgumentType.getString(context, "art"))))))
                        .then(Commands.literal("ende").executes(context -> {
                            Progression.tick(context.getSource().getServer(), true);
                            return 1;
                        })))
                .then(Commands.literal("leistung").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.translatable("message.atzen.performance",
                                    Component.translatable("performance.atzen." + Performance.current().id())), false);
                            return 1;
                        })
                        .then(Commands.argument("stufe", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (Performance tier : Performance.values()) builder.suggest(tier.word());
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Performance tier = Performance.of(StringArgumentType.getString(context, "stufe"));
                                    Progression.setPerformance(context.getSource().getServer(), tier);
                                    context.getSource().sendSuccess(() -> Component.translatable("message.atzen.performance",
                                            Component.translatable("performance.atzen." + tier.id())), true);
                                    return 1;
                                }))));
    }

    private static int tournament(CommandSourceStack source, int minutes, String species) {
        if (!species.isEmpty() && java.util.Arrays.stream(FishSpecies.values()).noneMatch(fish -> fish.id().equals(species))) {
            source.sendFailure(Component.translatable("message.atzen.unknown_species", species));
            return 0;
        }
        Progression.startTournament(source.getServer(), minutes, species);
        return 1;
    }

    /** Sonar module: anglers waiting at a float hear how many fish are around it. */
    public void sonar(MinecraftServer server) {
        if (server.overworld().getGameTime() % 100 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FishingHook hook = player.fishing;
            if (hook == null || sessions.containsKey(player.getUUID())) continue;
            ItemStack rod = player.getMainHandItem().getItem() instanceof AtzenRodItem ? player.getMainHandItem() : player.getOffhandItem();
            if (!RodMods.has(rod, ModuleType.SONAR)) continue;
            int count = player.level().getEntitiesOfClass(AtzenFishEntity.class, hook.getBoundingBox().inflate(12),
                    fish -> !fish.isBattleDummy()).size();
            player.sendSystemMessage(Component.translatable("message.atzen.sonar_float", count), true);
        }
    }

    private static final class Session {
        final int id;
        final ServerPlayer player;
        final FishingHook hook;
        final ItemStack rod;
        final InteractionHand hand;
        final FishingBattle battle;
        final FishSpecimen specimen;
        final AtzenFishEntity liveFish;
        final Vec3 anchor;
        final double surface;
        final Vec3 direction;
        Vec3 fishPos;
        AtzenFishEntity dummy;
        double lateral;
        int actionStart;
        int strike;
        boolean reeling;
        boolean lifting;
        int steer;
        int inputAge;
        /** Training: no rod, no loot, landed automatically. */
        final boolean practice;
        boolean handLanded;

        Session(int id, ServerPlayer player, FishingHook hook, ItemStack rod, InteractionHand hand, FishingBattle battle,
                FishSpecimen specimen, AtzenFishEntity liveFish, Vec3 anchor, boolean practice) {
            this.id = id;
            this.player = player;
            this.hook = hook;
            this.rod = rod;
            this.hand = hand;
            this.battle = battle;
            this.specimen = specimen;
            this.liveFish = liveFish;
            this.practice = practice;
            this.anchor = anchor;
            this.surface = hook != null ? hook.getY() : practice ? player.getY() : anchor.y;
            Vec3 away = anchor.subtract(player.position()).multiply(1, 0, 1);
            this.direction = away.lengthSqr() < 1e-4 ? player.getLookAngle().multiply(1, 0, 1).normalize() : away.normalize();
            this.fishPos = anchor;
        }

        /** Input goes stale after a second without packets: a frozen client neither reels nor pumps. */
        FishingBattle.Input input() {
            return inputAge <= 20 ? new FishingBattle.Input(reeling, lifting, steer) : FishingBattle.Input.NONE;
        }
    }
}
