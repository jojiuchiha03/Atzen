package io.github.jojiuchiha03.atzen.battle;

import com.mojang.brigadier.CommandDispatcher;
import io.github.jojiuchiha03.atzen.AtzenSounds;
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

        FishSpecimen specimen;
        Boss boss = liveFish == null ? bosses.check(level, hook.blockPosition(), baitType, player.getUUID(), random) : null;
        if (liveFish != null) specimen = liveFish.specimen();
        else if (boss != null) specimen = FishSpecies.of(boss).roll(random);
        else {
            boolean night = night(level);
            boolean raining = level.isRainingAt(hook.blockPosition());
            long time = level.getOverworldClockTime();
            FishSpecies species = FishSpecies.choose(Habitat.at(level, hook.blockPosition()), random, candidate -> {
                double appeal = BattleEquipment.appeal(bait, rod, candidate, night);
                return candidate.boss() != null || appeal < 0.5 ? 0 : appeal * candidate.activity(time, raining);
            });
            specimen = species.roll(random, baitType == null ? 0 : baitType.sizeBias());
        }

        Fighter fighter = specimen.fighter();
        if (baitType == BaitType.POPPER && fighter.boss() == null) {
            // A popper on the surface: hooked fish come up and jump more.
            int[] weights = fighter.weights().clone();
            weights[FishAction.JUMP.ordinal()] += 3;
            fighter = new Fighter(fighter.power(), fighter.endurance(), fighter.speed(), fighter.weight(), weights,
                    fighter.strikeIdeal(), fighter.salt(), fighter.teeth(), null);
        }
        int sinceBite = liveFish != null && nibble <= 0 ? fighter.strikeIdeal() : ((BiteTarget) hook).atzen$ticksSinceBite();
        RodType rodType = BattleEquipment.rodType(rod);
        double hold = FishingBattle.strikeHold(sinceBite, fighter.strikeIdeal(), rodType.sensitivity());
        // A circle hook sets itself; a fish far too small for the rod is hardly felt.
        if (AtzenItems.hook(AtzenRodItem.get(rod, GearSlot.HOOK)) == HookType.CIRCLE) hold = Math.max(hold, 85);
        if (fighter.weight() < rodType.fishMin()) hold -= 15;
        Rig rig = BattleEquipment.rig(player, rod, bait);
        FishingBattle battle = new FishingBattle(fighter, rig, Math.sqrt(hook.distanceToSqr(player)), hold, random.nextLong());
        if (!loaded.isEmpty()) AtzenRodItem.consumeBait(rod, player);
        else if (!bait.isEmpty() && !player.hasInfiniteMaterials()) {
            ItemStack used = AtzenRodItem.use(bait);
            player.setItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, used);
        }
        Session session = new Session(++nextId, player, hook, rod, hand, battle, specimen, liveFish);
        session.strike = (int) Math.round(hold);
        sessions.put(player.getUUID(), session);
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

    private static boolean night(ServerLevel level) {
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return time >= 13000 && time < 23000;
    }

    private void land(Session session) {
        if (session.battle.land()) return;
        // It kicked free and runs again.
        if (session.player.level() instanceof ServerLevel level && session.dummy != null) {
            level.sendParticles(ParticleTypes.SPLASH, session.dummy.getX(), session.surface + 0.1, session.dummy.getZ(),
                    30, 0.4, 0.2, 0.4, 0.2);
            level.playSound(null, session.dummy.blockPosition(), AtzenSounds.LEAP, SoundSource.NEUTRAL, 1.0f, 0.9f);
        }
        session.player.sendSystemMessage(Component.translatable("message.atzen.landing_failed").withStyle(ChatFormatting.RED), true);
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
        if (session.hook == null || !(session.player.level() instanceof ServerLevel level)) return;
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
            if (!player.isAlive() || player.isSpectator() || (session.hook != null &&
                    (session.hook.isRemoved() || player.fishing != session.hook
                            || session.hook.level() != player.level() || session.hook.distanceToSqr(player) > 4096
                            || player.getItemInHand(session.hand) != session.rod || session.rod.isEmpty()
                            || (session.liveFish != null && (!session.liveFish.isAlive() || session.liveFish.isRemoved()))))
                    || (session.hook == null && player.fishing != null)) {
                session.battle.cancel();
            }
            FishAction previous = session.battle.action();
            FishAction previousPending = session.battle.pending();
            session.inputAge++;
            session.battle.tick(session.input());
            // Training has no float to right-click: a worn-out fish close in is landed by hand at once.
            if (session.hook == null && session.battle.landable()) session.battle.land();
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
        if (session.dummy != null && session.player.level() instanceof ServerLevel level) {
            level.sendParticles(caught ? ParticleTypes.SPLASH : ParticleTypes.BUBBLE_POP, session.dummy.getX(),
                    session.surface + 0.1, session.dummy.getZ(), caught ? 30 : 12, 0.4, 0.2, 0.4, 0.2);
        }
        removeDummy(session);
        send(session);
        if (session.hook != null) {
            if (caught && !session.hook.isRemoved()) {
                completing.put(session.hook.getUUID(), session);
                try {
                    int damage;
                    Boss boss = session.specimen.species().boss();
                    if (session.liveFish != null || boss != null) {
                        session.player.spawnAtLocation(session.player.level(), caughtItem(session));
                        session.player.awardStat(Stats.FISH_CAUGHT, 1);
                        session.player.giveExperiencePoints(boss != null ? 60 : 1 + session.player.getRandom().nextInt(6));
                        if (session.liveFish != null) session.liveFish.discard();
                        session.hook.discard();
                        damage = boss != null ? 5 : 1;
                    } else {
                        damage = session.hook.retrieve(session.rod);
                    }
                    if (boss != null && session.player.level() instanceof ServerLevel level) {
                        session.player.spawnAtLocation(level, new ItemStack(trophy(boss)));
                        bosses.caught(level, session.player.getUUID(), boss);
                        session.player.sendSystemMessage(Component.translatable("message.atzen.boss_caught",
                                Component.translatable(boss.translationKey())).withStyle(ChatFormatting.GOLD));
                    }
                    // an overloaded rod wears faster
                    session.rod.hurtAndBreak(damage + session.battle.overloadTicks() / 20, session.player, session.hand);
                } finally {
                    completing.remove(session.hook.getUUID());
                }
            } else {
                session.hook.discard();
                if (result == FishingBattle.Result.SNAPPED || result == FishingBattle.Result.SPOOLED
                        || result == FishingBattle.Result.BITTEN) {
                    AtzenRodItem.loseLure(session.rod);
                }
                if (result == FishingBattle.Result.ROD_BROKEN) {
                    // The blank cracks: a quarter of its life is gone.
                    session.rod.hurtAndBreak(Math.max(1, session.rod.getMaxDamage() / 4), session.player, session.hand);
                } else if (result != FishingBattle.Result.CANCELLED) {
                    session.rod.hurtAndBreak(1, session.player, session.hand);
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
        session.player.level().playSound(null, session.player.blockPosition(), outcome, SoundSource.PLAYERS, 0.8f, 1.0f);
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
            ServerPlayNetworking.send(session.player, BattlePayload.of(session.id, session.battle, session.hook == null,
                    session.specimen, session.dummy == null ? -1 : session.dummy.getId(), session.strike));
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

    private ItemStack caughtItem(Session session) {
        return FishCatch.create(session.specimen, (ServerLevel) session.player.level(), session.hook.blockPosition(),
                session.battle.quality(), session.player.getName().getString());
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
                new FishingBattle(specimen.fighter(), rig, 12, 85, random.nextLong()), specimen, null);
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
                })));
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

        Session(int id, ServerPlayer player, FishingHook hook, ItemStack rod, InteractionHand hand, FishingBattle battle,
                FishSpecimen specimen, AtzenFishEntity liveFish) {
            this.id = id;
            this.player = player;
            this.hook = hook;
            this.rod = rod;
            this.hand = hand;
            this.battle = battle;
            this.specimen = specimen;
            this.liveFish = liveFish;
            this.anchor = hook != null ? hook.position() : player.position();
            this.surface = hook != null ? hook.getY() : player.getY();
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
