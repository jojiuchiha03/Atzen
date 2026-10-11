package io.github.jojiuchiha03.atzen.battle;

import com.mojang.brigadier.CommandDispatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import io.github.jojiuchiha03.atzen.item.BattleEquipment;
import io.github.jojiuchiha03.atzen.fish.AtzenFishEntity;
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
    private int nextId;

    public boolean interceptRetrieve(FishingHook hook, ItemStack rod, int nibble) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player) || completing.containsKey(hook.getUUID())) return false;
        Session active = sessions.get(player.getUUID());
        if (active != null) return active.hook == hook;
        AtzenFishEntity liveFish = hook.getHookedIn() instanceof AtzenFishEntity fish ? fish : null;
        if ((liveFish == null && (nibble <= 0 || hook.getHookedIn() != null))
                || (liveFish != null && !liveFish.isAlive()) || !ServerPlayNetworking.canSend(player, BattlePayload.TYPE)) return false;
        InteractionHand hand = player.getMainHandItem() == rod ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (player.getItemInHand(hand) != rod) return false;
        ItemStack bait = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Random random = new Random(player.getRandom().nextLong());
        FishSpecimen specimen = liveFish != null ? liveFish.specimen()
                : FishSpecies.choose(FishCatch.isSwamp(player.level(), hook.blockPosition()), random).roll(random);
        FishingBattle battle = new FishingBattle(specimen.species().style(), random.nextLong(),
                specimen.tune(BattleEquipment.tuning(player, rod, bait)));
        if (BattleEquipment.isBait(bait)) bait.consume(1, player);
        Session session = new Session(++nextId, player, hook, rod, hand, battle, specimen, liveFish);
        sessions.put(player.getUUID(), session);
        send(session);
        return true;
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
            if (session.hook != null && session.battle.phase() == FishingBattle.Phase.SURGE
                    && session.battle.elapsed() % 6 == 0) {
                player.level().sendParticles(ParticleTypes.SPLASH, session.hook.getX(), session.hook.getY() + 0.15,
                        session.hook.getZ(), 5, 0.3, 0.1, 0.3, 0.08);
            }
            if (session.battle.elapsed() % 2 == 0 || previous != session.battle.phase()) send(session);
        }
    }

    private void finish(Session session) {
        // Remove before giving loot, and guard re-entry through the retrieve mixin.
        sessions.remove(session.player.getUUID());
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
        if (session != null && session.hook != null) session.hook.discard();
    }

    public void clear() {
        sessions.values().forEach(session -> { if (session.hook != null) session.hook.discard(); });
        sessions.clear();
        completing.clear();
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
        }
    }
}
