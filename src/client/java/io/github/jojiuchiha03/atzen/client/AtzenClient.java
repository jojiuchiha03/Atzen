package io.github.jojiuchiha03.atzen.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.AtzenSounds;
import io.github.jojiuchiha03.atzen.battle.FishAction;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.item.AtzenRodItem;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import io.github.jojiuchiha03.atzen.network.LexiconPayload;
import io.github.jojiuchiha03.atzen.network.ReelPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The client side of a fight: the keys (reel, lift the rod, steer, drag, cancel, camera), the line's sounds, the
 * gauge above the hotbar ({@link FightHud}) and the cinematic camera ({@link CameraController}).
 */
public final class AtzenClient implements ClientModInitializer {
    static KeyMapping reelKey, liftKey, steerLeftKey, steerRightKey, dragUpKey, dragDownKey, cancelKey, cameraKey;
    static BattlePayload state;
    /** The last landed fish, for the trophy view. */
    static BattlePayload trophy;
    static float displayedTension;
    static float displayedStamina;
    /** Ticks since the current fight started. */
    static int cameraTicks;
    private static boolean reeling, lifting;
    private static int steer;
    private static int outroTicks;
    private static boolean hintShown;
    private static int ticks;
    static int packetAge;

    @Override
    public void onInitializeClient() {
        AtzenFishRenderer.register();
        var category = KeyMapping.Category.register(Atzen.id("fishing"));
        reelKey = key("reel", GLFW.GLFW_KEY_R, category);
        liftKey = key("lift", GLFW.GLFW_KEY_G, category);
        steerLeftKey = key("steer_left", GLFW.GLFW_KEY_LEFT, category);
        steerRightKey = key("steer_right", GLFW.GLFW_KEY_RIGHT, category);
        dragUpKey = key("drag_up", GLFW.GLFW_KEY_UP, category);
        dragDownKey = key("drag_down", GLFW.GLFW_KEY_DOWN, category);
        cancelKey = key("cancel", GLFW.GLFW_KEY_X, category);
        cameraKey = key("camera", GLFW.GLFW_KEY_C, category);
        ClientPlayNetworking.registerGlobalReceiver(BattlePayload.TYPE, (payload, context) -> receive(payload, context.client()));
        ClientPlayNetworking.registerGlobalReceiver(LexiconPayload.TYPE,
                (payload, context) -> context.client().gui.setScreen(new LexiconScreen(payload.data())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(AtzenClient::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Atzen.id("fight"), FightHud::render);
        ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof AtzenRodItem.GearTooltip gear ? new GearSlotsTooltip(gear) : null);
    }

    private static KeyMapping key(String name, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.atzen." + name, InputConstants.Type.KEYSYM, key, category));
    }

    private static void receive(BattlePayload payload, Minecraft client) {
        boolean newBattle = state == null || state.battleId() != payload.battleId();
        if (newBattle) {
            displayedTension = payload.tension();
            displayedStamina = payload.stamina();
            reeling = lifting = false;
            steer = 0;
            cameraTicks = 0;
            outroTicks = 0;
            CameraController.reset();
            if (client.player != null) {
                client.player.playSound(AtzenSounds.HOOK_SET, 0.8f, 1.0f);
                if (!hintShown) {
                    hintShown = true;
                    client.player.sendSystemMessage(Component.translatable("message.atzen.controls_hint",
                            reelKey.getTranslatedKeyMessage(), liftKey.getTranslatedKeyMessage(),
                            steerLeftKey.getTranslatedKeyMessage(), steerRightKey.getTranslatedKeyMessage(),
                            dragUpKey.getTranslatedKeyMessage(), dragDownKey.getTranslatedKeyMessage(),
                            cancelKey.getTranslatedKeyMessage(), cameraKey.getTranslatedKeyMessage()));
                }
            }
        } else if (state.pending() == null && payload.pending() != null && client.player != null) {
            client.player.playSound(AtzenSounds.WARNING, 0.7f, payload.pending() == FishAction.JUMP ? 1.3f : 1.0f);
        }
        if (payload.result() != FishingBattle.Result.ACTIVE && (state == null || state.result() == FishingBattle.Result.ACTIVE)) {
            outroTicks = 30;
            if (client.player != null) {
                client.player.sendOverlayMessage(Component.translatable("result.atzen."
                        + payload.result().name().toLowerCase(java.util.Locale.ROOT)));
            }
            if (payload.result() == FishingBattle.Result.CAUGHT && !payload.has(BattlePayload.PRACTICE)) {
                trophy = payload;
                CameraController.trophy();
            }
        }
        state = payload;
        packetAge = 0;
    }

    private static void tick(Minecraft client) {
        ticks++;
        while (cameraKey.consumeClick()) CameraController.toggle(client);
        if (client.player == null || client.level == null) {
            clear();
            CameraController.tick(client);
            return;
        }
        boolean cancel = false;
        while (cancelKey.consumeClick()) cancel = true;
        int drag = 0;
        while (dragUpKey.consumeClick()) drag++;
        while (dragDownKey.consumeClick()) drag--;
        // Consume the click queues even though these keys use the held-key state.
        while (reelKey.consumeClick()) { }
        while (liftKey.consumeClick()) { }
        while (steerLeftKey.consumeClick()) { }
        while (steerRightKey.consumeClick()) { }
        if (cancel && (state == null || state.result() != FishingBattle.Result.ACTIVE)) CameraController.skip();
        CameraController.tick(client);
        if (state == null) return;
        cameraTicks++;
        displayedTension += (state.tension() - displayedTension) * 0.5f;
        displayedStamina += (state.stamina() - displayedStamina) * 0.4f;
        if (state.result() != FishingBattle.Result.ACTIVE) {
            if (--outroTicks <= 0) state = null;
            return;
        }
        if (!client.isPaused() && ++packetAge > 100) { clear(); return; }
        boolean free = client.gui.screen() == null;
        boolean nextReeling = free && reelKey.isDown();
        boolean nextLifting = free && liftKey.isDown();
        int nextSteer = !free ? 0 : (steerLeftKey.isDown() ? -1 : 0) + (steerRightKey.isDown() ? 1 : 0);
        cancel &= free;
        if (!free) drag = 0;
        if ((nextReeling != reeling || nextLifting != lifting || nextSteer != steer || cancel || drag != 0 || ticks % 4 == 0)
                && ClientPlayNetworking.canSend(ReelPayload.TYPE)) {
            ClientPlayNetworking.send(new ReelPayload(state.battleId(), nextReeling, nextLifting, nextSteer, drag, cancel));
        }
        reeling = nextReeling;
        lifting = nextLifting;
        steer = nextSteer;
        sounds(client);
    }

    /** The line speaks too: creaking with rising tension, the reel's ratchet, a screaming drag, a limp splash. */
    private static void sounds(Minecraft client) {
        var player = client.player;
        float tension = displayedTension;
        int every = tension >= 0.85f ? 3 : tension >= 0.7f ? 6 : tension >= 0.55f ? 12 : 0;
        if (every > 0 && ticks % every == 0) player.playSound(AtzenSounds.CREAK, 0.25f + tension / 2.2f, 0.8f + tension / 2.5f);
        boolean slipping = tension >= state.drag() * 0.97f && (state.action() == FishAction.RUN || state.action() == FishAction.DIVE
                || state.action() == FishAction.SIDE) && state.pending() == null;
        if (slipping && ticks % 2 == 0) player.playSound(AtzenSounds.REEL, 0.5f, 1.6f + player.getRandom().nextFloat() * 0.2f);
        else if (reeling && ticks % 2 == 0) player.playSound(AtzenSounds.REEL, 0.35f, 0.9f + (1 - displayedStamina) * 0.25f);
        if (state.has(BattlePayload.SLACK) && ticks % 12 == 0) player.playSound(AtzenSounds.SLACK, 0.4f, 1.0f);
    }

    private static void clear() {
        state = null;
        reeling = lifting = false;
        steer = 0;
        packetAge = 0;
        outroTicks = 0;
    }
}
