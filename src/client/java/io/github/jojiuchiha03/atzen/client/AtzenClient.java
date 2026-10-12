package io.github.jojiuchiha03.atzen.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.AtzenSounds;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import io.github.jojiuchiha03.atzen.network.ReelPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A fight is shown through the camera (see {@link BattleCamera}), the fish on the line, camera shake and
 * sound, plus one slim tension gauge above the hotbar (see {@link TensionGauge}) that says whether to reel.
 */
public final class AtzenClient implements ClientModInitializer {
    static KeyMapping reelKey;
    static KeyMapping cancelKey;
    static BattlePayload state;
    static float displayedTension;
    static float displayedProgress;
    static boolean reeling;
    /** Ticks since the current fight started; drives the orbit and the blend into it. */
    static int cameraTicks;
    /** Ticks left for blending back to the player's own view after a fight. */
    static int outroTicks;
    private static CameraType previousCamera;
    private static boolean hintShown;
    private static int ticks;
    private static int packetAge;

    @Override
    public void onInitializeClient() {
        AtzenFishRenderer.register();
        var category = KeyMapping.Category.register(Atzen.id("fishing"));
        reelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.atzen.reel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        cancelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.atzen.cancel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, category));
        ClientPlayNetworking.registerGlobalReceiver(BattlePayload.TYPE, (payload, context) -> receive(payload, context.client()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear(client));
        ClientTickEvents.END_CLIENT_TICK.register(AtzenClient::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Atzen.id("line_tension"), TensionGauge::render);
        ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof AtzenRodItem.GearTooltip gear ? new GearSlotsTooltip(gear) : null);
    }

    private static void receive(BattlePayload payload, Minecraft client) {
        boolean newBattle = state == null || state.battleId() != payload.battleId();
        if (newBattle) {
            displayedTension = payload.tension();
            displayedProgress = payload.progress();
            reeling = false;
            cameraTicks = 0;
            BattleCamera.reset();
            outroTicks = 0;
            if (client.player != null) {
                client.player.playSound(AtzenSounds.HOOK_SET, 0.8f, 1.0f);
                if (!hintShown) {
                    hintShown = true;
                    client.player.sendSystemMessage(Component.translatable("message.atzen.controls_hint",
                            reelKey.getTranslatedKeyMessage(), cancelKey.getTranslatedKeyMessage()));
                }
            }
            // Third person while fighting, so the own player model and rod are drawn; restored afterwards.
            if (previousCamera == null) {
                previousCamera = client.options.getCameraType();
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else if (state.phase() != payload.phase() && payload.phase() == FishingBattle.Phase.WARNING && client.player != null) {
            client.player.playSound(AtzenSounds.WARNING, 0.7f, 1.0f);
        }
        if (payload.result() != FishingBattle.Result.ACTIVE && (state == null || state.result() == FishingBattle.Result.ACTIVE)) {
            outroTicks = BattleCamera.OUTRO;
            // the outcome sound comes from the server, for everyone around
        }
        state = payload;
        packetAge = 0;
    }

    private static void tick(Minecraft client) {
        ticks++;
        if (client.player == null || client.level == null) { clear(client); return; }
        boolean cancel = false;
        while (cancelKey.consumeClick()) cancel = true;
        // Consume the click queue even though reeling uses the held-key state.
        while (reelKey.consumeClick()) { }
        if (state == null) return;
        cameraTicks++;
        displayedTension += (state.tension() - displayedTension) * 0.5f;
        displayedProgress += (state.progress() - displayedProgress) * 0.5f;
        if (state.result() != FishingBattle.Result.ACTIVE) {
            if (--outroTicks <= 0) clear(client);
            return;
        }
        if (!client.isPaused() && ++packetAge > 100) { clear(client); return; }
        boolean nextReeling = client.gui.screen() == null && reelKey.isDown();
        cancel &= client.gui.screen() == null;
        if ((nextReeling != reeling || cancel || ticks % 4 == 0) && ClientPlayNetworking.canSend(ReelPayload.TYPE)) {
            ClientPlayNetworking.send(new ReelPayload(state.battleId(), nextReeling, cancel));
        }
        reeling = nextReeling;
        sounds(client);
    }

    /** The line speaks instead of a bar: creaking with rising tension, a reel clicking, a limp splash when slack. */
    private static void sounds(Minecraft client) {
        var player = client.player;
        float tension = displayedTension;
        int every = tension >= 85 ? 3 : tension >= 70 ? 6 : tension >= 55 ? 12 : 0;
        if (every > 0 && ticks % every == 0) {
            player.playSound(AtzenSounds.CREAK, 0.25f + tension / 220f, 0.8f + tension / 250f);
        }
        if (reeling && ticks % 2 == 0) player.playSound(AtzenSounds.REEL, 0.35f, 0.9f + displayedProgress / 400f);
        if (state.slack() && ticks % 12 == 0) player.playSound(AtzenSounds.SLACK, 0.4f, 1.0f);
    }

    static boolean fighting() { return state != null; }

    private static void clear(Minecraft client) {
        state = null;
        reeling = false;
        packetAge = 0;
        outroTicks = 0;
        if (previousCamera != null && client != null) {
            client.options.setCameraType(previousCamera);
            previousCamera = null;
        }
    }
}
