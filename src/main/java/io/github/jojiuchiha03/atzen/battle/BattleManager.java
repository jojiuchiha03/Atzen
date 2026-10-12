package io.github.jojiuchiha03.atzen.battle;

import com.mojang.brigadier.CommandDispatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import io.github.jojiuchiha03.atzen.network.ReelPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class BattleManager {
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Session> completing = new HashMap<>();
    /** Fish swimming to or biting a float, by float UUID (see BiteHookGoal). */
    private final Map<UUID, AtzenFishEntity> biters = new HashMap<>();
    private int nextId;

    public boolean interceptRetrieve(FishingHook hook, ItemStack rod, int nibble) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player) || completing.containsKey(hook.getUUID())) return false;
        Session active = sessions.get(player.getUUID());
        if (active != null) return active.hook == hook;
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
        ItemStack bait = !loaded.isEmpty() ? loaded
                : player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Random random = new Random(player.getRandom().nextLong());
        FishSpecimen specimen = liveFish != null ? liveFish.specimen()
                : FishSpecies.choose(FishCatch.isSwamp(player.level(), hook.blockPosition()), random).roll(random);
        FishingBattle battle = new FishingBattle(specimen.species().style(), random.nextLong(),
                specimen.tune(BattleEquipment.tuning(player, rod, bait)));
        if (!loaded.isEmpty()) AtzenRodItem.consumeBait(rod, player);
        else if (BattleEquipment.isBait(bait)) bait.consume(1, player);
        Session session = new Session(++nextId, player, hook, rod, hand, battle, specimen, liveFish);
        sessions.put(player.getUUID(), session);
        biters.remove(hook.getUUID());
        if (liveFish == null) spawnDummy(session);
        else {
            liveFish.getNavigation().stop();
            liveFish.setNoAi(true);
            session.dummy = liveFish;
        }
        send(session);
        return true;
    }

    /** The fish everyone sees on the line: a non-saved, invulnerable copy of the specimen that is being fought. */
    private void spawnDummy(Session session) {
        if (!(session.player.level() instanceof ServerLevel level)) return;
        AtzenFishEntity dummy = AtzenFishRegistry.entity(session.specimen.species()).create(level, EntitySpawnReason.TRIGGERED);
        if (dummy == null) return;
        dummy.becomeBattleDummy(session.specimen);
        dummy.snapTo(session.anchor.x, session.anchor.y - 0.35, session.anchor.z, session.player.getYRot(), 0);
        session.dummy = dummy;
        level.addFreshEntity(dummy);
        level.sendParticles(ParticleTypes.SPLASH, session.anchor.x, session.anchor.y + 0.1, session.anchor.z, 14, 0.3, 0.1, 0.3, 0.12);
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
     * Server-side choreography, visible to every player: the fish circles under the float, thrashes when it is
     * about to run, leaps out of the water during a surge and is dragged towards the angler as the fight is won.
     * The float follows the fish so the line moves with it.
     */
    private void animate(Session session) {
        if (session.hook == null || !(session.player.level() instanceof ServerLevel level)) return;
        FishingBattle battle = session.battle;
        int t = battle.elapsed();
        if (session.reeling && session.inputAge <= 20 && t % 7 == 0) session.player.swing(session.hand, true);
        if (session.dummy == null || session.dummy.isRemoved()) return;

        Vec3 toAngler = session.player.position().subtract(session.anchor).multiply(1, 0, 1);
        Vec3 wanted = session.anchor.add(toAngler.scale(battle.progress() / 100.0 * 0.6));
        // Only drag the fish over water; at the shore it stays where it last could swim.
        if (level.getFluidState(BlockPos.containing(wanted.x, session.anchor.y - 0.3, wanted.z)).is(FluidTags.WATER)) {
            session.center = new Vec3(wanted.x, session.anchor.y, wanted.z);
        }
        FishingBattle.Phase phase = battle.phase();
        session.angle += switch (phase) { case STEADY -> 0.10; case WARNING -> 0.05; case SURGE -> 0.32; case RECOVERY -> 0.06; };
        double radius = phase == FishingBattle.Phase.SURGE ? 1.1 : 0.55;
        double x = session.center.x + Math.cos(session.angle) * radius;
        double z = session.center.z + Math.sin(session.angle) * radius;
        double y = session.anchor.y - 0.35;
        float pitch = 0;
        switch (phase) {
            case WARNING -> {
                x += (session.player.getRandom().nextDouble() - 0.5) * 0.25;
                z += (session.player.getRandom().nextDouble() - 0.5) * 0.25;
                if (t % 4 == 0) level.sendParticles(ParticleTypes.BUBBLE, x, y + 0.3, z, 4, 0.2, 0.05, 0.2, 0.02);
            }
            case SURGE -> {
                int cycle = t % 18;
                if (cycle < 12) {
                    double arc = Math.sin(cycle / 12.0 * Math.PI);
                    y += arc * 1.4;
                    pitch = (float) ((6 - cycle) * -9.0);
                }
                if (cycle == 0 || cycle == 12) {
                    level.sendParticles(ParticleTypes.SPLASH, x, session.anchor.y + 0.1, z, 18, 0.35, 0.1, 0.35, 0.15);
                    level.playSound(null, x, session.anchor.y, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.5f,
                            1.1f + session.player.getRandom().nextFloat() * 0.3f);
                }
            }
            case RECOVERY -> y -= 0.15;
            case STEADY -> {
                if (t % 10 == 0) level.sendParticles(ParticleTypes.BUBBLE, x, y + 0.2, z, 2, 0.1, 0.05, 0.1, 0.01);
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(Math.sin(session.angle), Math.cos(session.angle)));
        session.dummy.snapTo(x, y, z, yaw, pitch);
        session.dummy.setYHeadRot(yaw);
        session.dummy.setYBodyRot(yaw);
        session.dummy.setDeltaMovement(Vec3.ZERO);
        session.hook.setPos(x, session.anchor.y, z);
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
                            || session.hook.level() != player.level() || session.hook.distanceToSqr(player) > 1024
                            || player.getItemInHand(session.hand) != session.rod || session.rod.isEmpty()
                            || (session.liveFish != null && (!session.liveFish.isAlive() || session.liveFish.isRemoved()))))
                    || (session.hook == null && player.fishing != null)) {
                session.battle.cancel();
            }
            FishingBattle.Phase previous = session.battle.phase();
            session.inputAge++;
            session.battle.tick(session.reeling && session.inputAge <= 20);
            if (session.battle.result() != FishingBattle.Result.ACTIVE) {
                finish(session);
                continue;
            }
            animate(session);
            if (session.battle.elapsed() % 2 == 0 || previous != session.battle.phase()) send(session);
        }
    }

    private void finish(Session session) {
        // Remove before giving loot, and guard re-entry through the retrieve mixin.
        sessions.remove(session.player.getUUID());
        if (session.dummy != null && session.player.level() instanceof ServerLevel level) {
            boolean caught = session.battle.result() == FishingBattle.Result.CAUGHT;
            level.sendParticles(caught ? ParticleTypes.SPLASH : ParticleTypes.BUBBLE_POP, session.dummy.getX(),
                    session.anchor.y + 0.1, session.dummy.getZ(), caught ? 30 : 12, 0.4, 0.2, 0.4, 0.2);
        }
        removeDummy(session);
        send(session);
        if (session.hook != null) {
            if (session.battle.result() == FishingBattle.Result.CAUGHT && !session.hook.isRemoved()) {
                completing.put(session.hook.getUUID(), session);
                try {
                    int damage;
                    if (session.liveFish != null) {
                        session.player.spawnAtLocation(session.player.level(), caughtItem(session));
                        session.player.awardStat(Stats.FISH_CAUGHT, 1);
                        session.player.giveExperiencePoints(1 + session.player.getRandom().nextInt(6));
                        session.liveFish.discard();
                        session.hook.discard();
                        damage = 1;
                    } else {
                        damage = session.hook.retrieve(session.rod);
                    }
                    session.rod.hurtAndBreak(damage, session.player, session.hand);
                } finally {
                    completing.remove(session.hook.getUUID());
                }
            } else {
                session.hook.discard();
                if (session.battle.result() != FishingBattle.Result.CANCELLED) {
                    session.rod.hurtAndBreak(1, session.player, session.hand);
                }
            }
        }
        session.player.level().playSound(null, session.player.blockPosition(),
                session.battle.result() == FishingBattle.Result.CAUGHT
                        ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 0.7f, session.battle.result() == FishingBattle.Result.CAUGHT ? 1.3f : 0.7f);
    }

    private void send(Session session) {
        if (ServerPlayNetworking.canSend(session.player, BattlePayload.TYPE)) {
            ServerPlayNetworking.send(session.player, BattlePayload.of(session.id, session.battle, session.hook == null, session.specimen));
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
        return FishCatch.create(session.specimen, session.player.level(), session.hook.blockPosition(),
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

    private int practice(ServerPlayer player, FishStyle style) {
        if (!ServerPlayNetworking.canSend(player, BattlePayload.TYPE)) {
            player.sendSystemMessage(Component.translatable("message.atzen.client_required"));
            return 0;
        }
        if (sessions.containsKey(player.getUUID()) || player.fishing != null || !player.isAlive() || player.isSpectator()) {
            player.sendSystemMessage(Component.translatable("message.atzen.already_fishing"));
            return 0;
        }
        Session session = new Session(++nextId, player, null, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new FishingBattle(style, player.getRandom().nextLong()), null, null);
        sessions.put(player.getUUID(), session);
        send(session);
        return 1;
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var practice = Commands.literal("practice")
                .executes(context -> practice(context.getSource().getPlayerOrException(), FishStyle.FIGHTER));
        for (FishStyle style : FishStyle.values()) {
            practice.then(Commands.literal(style.name().toLowerCase(Locale.ROOT))
                    .executes(context -> practice(context.getSource().getPlayerOrException(), style)));
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
        Vec3 center;
        AtzenFishEntity dummy;
        double angle;
        boolean reeling;
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
            this.center = anchor;
        }
    }
}
