package io.github.jojiuchiha03.atzen.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import io.github.jojiuchiha03.atzen.network.ReelPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public final class AtzenClient implements ClientModInitializer {
    static KeyMapping reelKey;
    static KeyMapping cancelKey;
    static BattlePayload state;
    static float displayedTension;
    static float displayedProgress;
    static boolean reeling;
    private static int ticks;
    private static int resultTicks;
    private static int packetAge;

    @Override
    public void onInitializeClient() {
        AtzenFishRenderer.register();
        var category = KeyMapping.Category.register(Atzen.id("fishing"));
        reelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.atzen.reel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        cancelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.atzen.cancel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, category));
        ClientPlayNetworking.registerGlobalReceiver(BattlePayload.TYPE, (payload, context) -> receive(payload, context.client()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(AtzenClient::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Atzen.id("fishing_battle"), BattleHud::render);
    }

    private static void receive(BattlePayload payload, Minecraft client) {
        boolean newBattle = state == null || state.battleId() != payload.battleId();
        if (newBattle) {
            displayedTension = payload.tension();
            displayedProgress = payload.progress();
            reeling = false;
            if (client.player != null) client.player.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.7f, 1.1f);
        } else if (state.phase() != payload.phase() && payload.phase() == FishingBattle.Phase.WARNING && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 0.65f);
        }
        state = payload;
        packetAge = 0;
        if (payload.result() != FishingBattle.Result.ACTIVE) resultTicks = 90;
    }

    private static void tick(Minecraft client) {
        ticks++;
        if (client.player == null || client.level == null) { clear(); return; }
        boolean cancel = false;
        while (cancelKey.consumeClick()) cancel = true;
        // Consume the click queue even though reeling uses the held-key state.
        while (reelKey.consumeClick()) { }
        if (state == null) return;
        displayedTension += (state.tension() - displayedTension) * 0.5f;
        displayedProgress += (state.progress() - displayedProgress) * 0.5f;
        if (state.result() != FishingBattle.Result.ACTIVE) {
            if (--resultTicks <= 0) clear();
            return;
        }
        if (!client.isPaused() && ++packetAge > 100) { clear(); return; }
        boolean nextReeling = client.gui.screen() == null && reelKey.isDown();
        cancel &= client.gui.screen() == null;
        if ((nextReeling != reeling || cancel || ticks % 4 == 0) && ClientPlayNetworking.canSend(ReelPayload.TYPE)) {
            ClientPlayNetworking.send(new ReelPayload(state.battleId(), nextReeling, cancel));
        }
        reeling = nextReeling;
    }

    private static void clear() {
        state = null;
        reeling = false;
        packetAge = 0;
        resultTicks = 0;
    }
}
