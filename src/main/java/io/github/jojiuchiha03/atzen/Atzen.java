package io.github.jojiuchiha03.atzen;

import io.github.jojiuchiha03.atzen.battle.BattleManager;
import io.github.jojiuchiha03.atzen.block.AtzenBlocks;
import io.github.jojiuchiha03.atzen.ecology.Ecology;
import io.github.jojiuchiha03.atzen.holding.FishRelease;
import io.github.jojiuchiha03.atzen.network.LexiconPayload;
import io.github.jojiuchiha03.atzen.progress.Progression;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import io.github.jojiuchiha03.atzen.network.ReelPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Atzen implements ModInitializer {
    public static final String MOD_ID = "atzen";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final BattleManager BATTLES = new BattleManager();

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    @Override
    public void onInitialize() {
        AtzenSounds.initialize();
        ModItems.register();
        AtzenItems.initialize();
        AtzenFishRegistry.initialize();
        AtzenBlocks.initialize();
        Ecology.initialize();
        Progression.initialize();
        PayloadTypeRegistry.serverboundPlay().register(ReelPayload.TYPE, ReelPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BattlePayload.TYPE, BattlePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LexiconPayload.TYPE, LexiconPayload.CODEC);
        FishRelease.register();
        ServerPlayNetworking.registerGlobalReceiver(ReelPayload.TYPE,
                (payload, context) -> BATTLES.input(context.player(), payload));
        ServerTickEvents.END_SERVER_TICK.register(BATTLES::tick);
        ServerTickEvents.END_SERVER_TICK.register(BATTLES::sonar);
        ServerTickEvents.END_SERVER_TICK.register(server -> Progression.tick(server, false));
        ServerLifecycleEvents.SERVER_STARTED.register(Progression::load);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BATTLES.disconnect(handler.getPlayer()));
        // Before the world is saved: hooked fish get their AI back and battle dummies are gone.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> BATTLES.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BATTLES.clear());
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> BATTLES.registerCommands(dispatcher));
        LOGGER.info("Atzen geladen: fishing battles initialized for Minecraft 26.2");
    }
}
