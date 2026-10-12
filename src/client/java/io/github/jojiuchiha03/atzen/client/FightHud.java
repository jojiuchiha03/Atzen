package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.Boss;
import io.github.jojiuchiha03.atzen.battle.FishAction;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The fight display above the hotbar. One bar for the line: how much of its breaking load the tension uses,
 * with a cyan mark where the drag slips; under it the fish's strength and the hook hold. One line of text says
 * what to do now. A boss shows its name and phase at the top, a landed fish a trophy card.
 */
final class FightHud {
    private static final int WIDTH = 160;

    private FightHud() { }

    enum Advice {
        LAND(0xFF5BE38A), RELEASE(0xFFFF5B6E), TANGLED(0xFFFF5B6E), WARN_RUN(0xFFFF9A3C), WARN_DIVE(0xFFFF9A3C),
        WARN_SIDE(0xFFFF9A3C), WARN_SHAKE(0xFFFF9A3C), WARN_JUMP(0xFFFF9A3C), SLACK(0xFFFFD45B), JUMP(0xFFFFD45B),
        STEER_LEFT(0xFFFFD45B), STEER_RIGHT(0xFFFFD45B), SPOOL(0xFFFF5B6E), TAKING(0xFFFFD45B), TWIST(0xFFFFD45B),
        SHAKE(0xFFFFD45B), PUMP(0xFF5BE38A), REEL(0xFF5BE38A);

        final int colour;

        Advice(int colour) { this.colour = colour; }

        String key() { return "hud.atzen.advice." + name().toLowerCase(Locale.ROOT); }

        boolean urgent() { return colour == 0xFFFF5B6E; }
    }

    static Advice advice(BattlePayload state) {
        float tension = AtzenClient.displayedTension;
        if (state.has(BattlePayload.TANGLED)) return Advice.TANGLED;
        if (state.has(BattlePayload.LANDABLE)) return Advice.LAND;
        if (tension >= 0.9f) return Advice.RELEASE;
        if (state.pending() != null) return switch (state.pending()) {
            case DIVE -> Advice.WARN_DIVE;
            case SIDE -> Advice.WARN_SIDE;
            case SHAKE -> Advice.WARN_SHAKE;
            case JUMP -> Advice.WARN_JUMP;
            default -> Advice.WARN_RUN;
        };
        if (state.lineOut() > state.capacity() * 0.8f) return Advice.SPOOL;
        if (state.has(BattlePayload.SLACK)) return Advice.SLACK;
        if (state.action() == FishAction.JUMP) return Advice.JUMP;
        if (state.action() == FishAction.SIDE) return state.sideDir() > 0 ? Advice.STEER_LEFT : Advice.STEER_RIGHT;
        if (state.has(BattlePayload.TWIST)) return Advice.TWIST;
        if ((state.action() == FishAction.RUN || state.action() == FishAction.DIVE) && tension >= state.drag() * 0.95f) return Advice.TAKING;
        if (state.action() == FishAction.SHAKE) return Advice.SHAKE;
        return state.distance() > 8 ? Advice.PUMP : Advice.REEL;
    }

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        if (CameraController.state() == CameraController.State.TROPHY && AtzenClient.trophy != null) {
            trophy(graphics, font, AtzenClient.trophy);
            return;
        }
        var state = AtzenClient.state;
        if (state == null || state.result() != FishingBattle.Result.ACTIVE) return;
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = graphics.guiHeight() - 72;

        // the line: tension against breaking load, zones, drag mark, tension mark
        float tension = Math.clamp(AtzenClient.displayedTension, 0, 1);
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + 7, 0xC0101820);
        graphics.fill(x, y, x + WIDTH, y + 6, 0xFF2A3540);
        int fill = Math.round(WIDTH * tension);
        int colour = tension < 0.6f ? 0xFF2F8A5A : tension < 0.85f ? 0xFFB8962F : 0xFFB83A4E;
        graphics.fill(x, y + 1, x + fill, y + 5, colour);
        graphics.fill(x + (int) (WIDTH * 0.85f), y, x + WIDTH, y + 1, 0xFF9A2F42);
        int drag = x + Math.clamp(Math.round(WIDTH * state.drag()), 1, WIDTH - 1);
        graphics.fill(drag - 1, y - 2, drag + 1, y + 8, 0xFF4CD6E8);
        int mark = x + Math.clamp(fill, 1, WIDTH - 1);
        graphics.fill(mark - 1, y - 1, mark + 1, y + 7, 0xFFFFFFFF);

        // the fish's strength and the hook hold
        int stamina = Math.round(WIDTH * Math.clamp(AtzenClient.displayedStamina, 0, 1));
        graphics.fill(x, y + 8, x + WIDTH, y + 10, 0x80303A44);
        graphics.fill(x, y + 8, x + stamina, y + 10, 0xFF4CDED0);
        int hook = Math.round(WIDTH * Math.clamp(state.hookHold() / 100f, 0, 1));
        graphics.fill(x, y + 11, x + WIDTH, y + 12, 0x80303A44);
        graphics.fill(x, y + 11, x + hook, y + 12, state.hookHold() < 30 ? 0xFFFF5B6E : 0xFFE8A040);

        // snags: how close a sideways run is to cover
        if (state.danger() > 1) {
            int danger = Math.round(WIDTH * Math.clamp(state.danger() / 100f, 0, 1));
            graphics.fill(x, y - 4, x + danger, y - 3, 0xFFFF7A3C);
        }

        // what to do now; it blinks when it is urgent
        Advice advice = advice(state);
        boolean blink = (advice.urgent() || advice == Advice.LAND) && (AtzenClient.cameraTicks / 4) % 2 == 1;
        if (!blink) {
            Component text = Component.translatable(advice.key(), AtzenClient.reelKey.getTranslatedKeyMessage(),
                    AtzenClient.liftKey.getTranslatedKeyMessage(), AtzenClient.dragDownKey.getTranslatedKeyMessage(),
                    AtzenClient.dragUpKey.getTranslatedKeyMessage(),
                    (state.sideDir() > 0 ? AtzenClient.steerLeftKey : AtzenClient.steerRightKey).getTranslatedKeyMessage());
            graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y - 14, advice.colour, true);
        }
        // the numbers: line out, drag step, hook hold
        Component info = Component.translatable("hud.atzen.info", Math.round(state.lineOut()), Math.round(state.capacity()),
                state.dragStep(), FishingBattle.DRAG_STEPS, Math.round(state.hookHold()));
        graphics.text(font, info, (graphics.guiWidth() - font.width(info)) / 2, y + 14, 0xFFB8C4CC, true);

        if (AtzenClient.cameraTicks < 50 && state.strike() > 0) {
            String quality = state.strike() >= 90 ? "perfect" : state.strike() >= 72 ? "good" : "late";
            Component strike = Component.translatable("hud.atzen.strike." + quality, state.strike());
            graphics.text(font, strike, (graphics.guiWidth() - font.width(strike)) / 2, y - 26, 0xFFFFFFFF, true);
        }
        Boss boss = state.species().boss();
        if (boss != null) bossBar(graphics, font, state, boss);
    }

    private static void bossBar(GuiGraphicsExtractor graphics, Font font, BattlePayload state, Boss boss) {
        int width = 182;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 14;
        Component name = Component.translatable(boss.translationKey()).append(" – ")
                .append(Component.translatable(boss.phase(state.bossPhase()).translationKey(boss)));
        graphics.text(font, name, (graphics.guiWidth() - font.width(name)) / 2, y - 10, 0xFFFFC94A, true);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 5, 0xC0101820);
        graphics.fill(x, y, x + Math.round(width * Math.clamp(AtzenClient.displayedStamina, 0, 1)), y + 4, 0xFFC0392B);
        for (int phase = 0; phase < boss.phases(); phase++) {
            int pip = x + width / 2 + (phase - boss.phases() / 2) * 10;
            graphics.fill(pip, y + 7, pip + 6, y + 9, phase < state.bossPhase() ? 0xFF505050 : phase == state.bossPhase() ? 0xFFFFC94A : 0xFFC0392B);
        }
    }

    private static void trophy(GuiGraphicsExtractor graphics, Font font, BattlePayload state) {
        FishSpecies species = state.species();
        int colour = switch (species.rarity()) {
            case COMMON -> 0xFFFFFFFF;
            case UNCOMMON -> 0xFF55FF55;
            case RARE -> 0xFF55D8FF;
            case EPIC -> 0xFFD070FF;
            case LEGENDARY -> 0xFFFFC94A;
            case MYTHIC -> 0xFFFF5555;
        };
        int width = 200, x = (graphics.guiWidth() - width) / 2, y = 24;
        graphics.fill(x, y, x + width, y + 46, 0xB0101820);
        graphics.fill(x, y, x + width, y + 1, colour);
        Component name = Component.translatable(species.translationKey());
        graphics.text(font, name, (graphics.guiWidth() - font.width(name)) / 2, y + 6, colour, true);
        Component rarity = Component.translatable(species.rarityKey());
        graphics.text(font, rarity, (graphics.guiWidth() - font.width(rarity)) / 2, y + 17, 0xFFB8C4CC, true);
        Component size = Component.translatable("catch.atzen.measurements", String.format(Locale.GERMAN, "%.1f", state.lengthCm()),
                String.format(Locale.GERMAN, "%.2f", state.weightKg()));
        graphics.text(font, size, (graphics.guiWidth() - font.width(size)) / 2, y + 28, 0xFFFFFFFF, true);
        Component quality = Component.translatable("hud.atzen.trophy_quality", state.quality());
        graphics.text(font, quality, (graphics.guiWidth() - font.width(quality)) / 2, y + 38, 0xFF7FD8C8, false);
    }
}
