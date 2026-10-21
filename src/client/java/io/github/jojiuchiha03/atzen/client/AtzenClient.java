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
 * The client side of a fight: two keys (R reels in, F gives line), the line's sounds and the panel above the
 * hotbar ({@link FightHud}). The view stays the player's own the whole time.
 */
public final class AtzenClient implements ClientModInitializer {
    /** How long the catch card stays on screen. */
    static final int TROPHY_TICKS = 80;
    static KeyMapping reelKey, giveKey;
    static BattlePayload state;
    /** The last landed fish, for the catch card. */
    static BattlePayload trophy;
    static int trophyTicks;
    static float displayedTension;
    static float displayedStamina;
    /** Ticks since the current fight started. */
    static int fightTicks;
    private static boolean reeling, giving;
    private static int outroTicks;
    private static boolean hintShown;
    private static int ticks;
    static int packetAge;

    @Override
    public void onInitializeClient() {
        AtzenFishRenderer.register();
        var category = KeyMapping.Category.register(Atzen.id("fishing"));
        reelKey = key("reel", GLFW.GLFW_KEY_R, category);
        giveKey = key("give", GLFW.GLFW_KEY_F, category);
        ClientPlayNetworking.registerGlobalReceiver(BattlePayload.TYPE, (payload, context) -> receive(payload, context.client()));
        ClientPlayNetworking.registerGlobalReceiver(LexiconPayload.TYPE,
                (payload, context) -> context.client().gui.setScreen(new LexiconScreen(payload.data())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        // F is also vanilla's swap-hands key: during a fight it only gives line, the rod stays in hand.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (fighting()) while (client.options.keySwapOffhand.consumeClick()) { }
        });
        ClientTickEvents.END_CLIENT_TICK.register(AtzenClient::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Atzen.id("fight"), FightHud::render);
        ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof AtzenRodItem.GearTooltip gear ? new GearSlotsTooltip(gear) : null);
    }

    private static KeyMapping key(String name, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.atzen." + name, InputConstants.Type.KEYSYM, key, category));
    }

    static boolean fighting() { return state != null && state.result() == FishingBattle.Result.ACTIVE; }

    private static void receive(BattlePayload payload, Minecraft client) {
        boolean newBattle = state == null || state.battleId() != payload.battleId();
        if (newBattle) {
            displayedTension = payload.tension();
            displayedStamina = payload.stamina();
            reeling = giving = false;
            fightTicks = 0;
            outroTicks = 0;
            trophyTicks = 0;
            if (client.player != null) {
                client.player.playSound(AtzenSounds.HOOK_SET, 0.8f, 1.0f);
                if (!hintShown) {
                    hintShown = true;
                    client.player.sendSystemMessage(Component.translatable("message.atzen.controls_hint",
                            reelKey.getTranslatedKeyMessage(), giveKey.getTranslatedKeyMessage()));
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
                trophyTicks = TROPHY_TICKS;
            }
        }
        state = payload;
        packetAge = 0;
    }

    private static void tick(Minecraft client) {
        ticks++;
        if (trophyTicks > 0) trophyTicks--;
        // Consume the click queues even though these keys use the held-key state.
        while (reelKey.consumeClick()) { }
        while (giveKey.consumeClick()) { }
        if (client.player == null || client.level == null) {
            clear();
            return;
        }
        if (state == null) return;
        fightTicks++;
        displayedTension += (state.tension() - displayedTension) * 0.5f;
        displayedStamina += (state.stamina() - displayedStamina) * 0.4f;
        if (state.result() != FishingBattle.Result.ACTIVE) {
            if (--outroTicks <= 0) state = null;
            return;
        }
        if (!client.isPaused() && ++packetAge > 100) { clear(); return; }
        boolean free = client.gui.screen() == null;
        boolean nextGiving = free && giveKey.isDown();
        boolean nextReeling = free && reelKey.isDown() && !nextGiving;
        if ((nextReeling != reeling || nextGiving != giving || ticks % 4 == 0) && ClientPlayNetworking.canSend(ReelPayload.TYPE)) {
            ClientPlayNetworking.send(new ReelPayload(state.battleId(), nextReeling, nextGiving, false));
        }
        reeling = nextReeling;
        giving = nextGiving;
        sounds(client);
    }

    static boolean reeling() { return reeling; }
    static boolean giving() { return giving; }

    /** The line speaks too: creaking with rising tension, the reel's ratchet, a screaming drag, a limp splash. */
    private static void sounds(Minecraft client) {
        var player = client.player;
        float tension = displayedTension;
        int every = tension >= 0.6f ? 4 : tension >= 0.45f ? 8 : 0;
        if (every > 0 && ticks % every == 0) player.playSound(AtzenSounds.CREAK, 0.2f + tension / 2.5f, 0.8f + tension / 2.5f);
        boolean slipping = tension >= state.drag() * 0.97f && (state.action() == FishAction.RUN || state.action() == FishAction.DIVE
                || state.action() == FishAction.SIDE) && state.pending() == null;
        if ((slipping || giving) && ticks % 2 == 0) player.playSound(AtzenSounds.REEL, 0.45f, 1.6f + player.getRandom().nextFloat() * 0.2f);
        else if (reeling && ticks % 2 == 0) player.playSound(AtzenSounds.REEL, 0.35f, 0.9f + (1 - displayedStamina) * 0.25f);
        if (state.has(BattlePayload.SLACK) && ticks % 12 == 0) player.playSound(AtzenSounds.SLACK, 0.4f, 1.0f);
    }

    private static void clear() {
        state = null;
        reeling = giving = false;
        packetAge = 0;
        outroTicks = 0;
    }
}
