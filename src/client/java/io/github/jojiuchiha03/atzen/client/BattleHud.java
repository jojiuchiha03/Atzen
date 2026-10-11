package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.Locale;

final class BattleHud {
    private static final int WIDTH = 290;
    private static final int WHITE = 0xFFF0F6FA;
    private static final int MUTED = 0xFF95A9BA;
    private static final int CYAN = 0xFF4CDED0;
    private static final int GOLD = 0xFFFFC76B;
    private static final int RED = 0xFFFF667D;

    private BattleHud() { }

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var state = AtzenClient.state;
        if (state == null) return;
        Minecraft client = Minecraft.getInstance();
        float scale = Math.min(1f, (graphics.guiWidth() - 12f) / WIDTH);
        graphics.pose().pushMatrix();
        graphics.pose().translate((graphics.guiWidth() - WIDTH * scale) / 2f, 18f);
        graphics.pose().scale(scale, scale);
        boolean active = state.result() == FishingBattle.Result.ACTIVE;
        int height = active ? 155 : 79;
        graphics.fill(0, 0, WIDTH, height, 0xEF0B1722);
        graphics.fill(0, 0, WIDTH, 2, CYAN);
        graphics.fill(0, 2, 1, height, 0xFF294555);
        graphics.fill(WIDTH - 1, 2, WIDTH, height, 0xFF294555);
        text(graphics, "ATZEN", 12, 11, CYAN);
        text(graphics, translate(state.practice() ? "hud.atzen.practice" : "hud.atzen.title"), 55, 11, WHITE);

        if (!active) {
            boolean caught = state.result() == FishingBattle.Result.CAUGHT;
            text(graphics, translate("result.atzen." + state.result().name().toLowerCase(Locale.ROOT)), 12, 32, caught ? CYAN : RED);
            text(graphics, translate(caught ? "hud.atzen.quality" : "hud.atzen.try_again", state.quality()), 12, 48, MUTED);
            text(graphics, translate(state.practice() ? "hud.atzen.no_rewards" : "hud.atzen.ready"), 12, 64, MUTED);
            graphics.pose().popMatrix();
            return;
        }

        String timer = state.secondsLeft() + "s";
        text(graphics, timer, WIDTH - 12 - client.font.width(timer), 11, MUTED);
        if (!state.practice()) {
            text(graphics, translate(state.species().translationKey()), 12, 29, GOLD);
            String measurements = String.format(Locale.ROOT, "%.1f cm / %.2f kg", state.lengthCm(), state.weightKg());
            text(graphics, measurements, WIDTH - 12 - client.font.width(measurements), 29, MUTED);
        }
        graphics.pose().translate(0, 15);
        text(graphics, translate(state.style().translationKey()), 12, 29, GOLD);
        String status = "phase.atzen." + state.phase().name().toLowerCase(Locale.ROOT);
        if (state.tension() >= 85) status = "hud.atzen.danger";
        else if (state.slack()) status = "hud.atzen.slack";
        String hint = translate(status);
        text(graphics, hint, WIDTH - 12 - client.font.width(hint), 29,
                state.tension() >= 85 || state.slack() ? RED : WHITE);

        int barWidth = WIDTH - 24;
        int tension = Math.round(AtzenClient.displayedTension);
        text(graphics, translate("hud.atzen.tension", tension), 12, 47, MUTED);
        graphics.fill(12, 60, 12 + barWidth, 70, 0xFF293743);
        graphics.fill(12 + (int) (barWidth * 0.30), 60, 12 + (int) (barWidth * 0.65), 70, 0xFF235C4F);
        graphics.fill(12 + (int) (barWidth * 0.85), 60, 12 + barWidth, 70, 0xFF743448);
        int marker = 12 + Math.clamp(Math.round(barWidth * tension / 100f), 1, barWidth - 2);
        graphics.fill(marker - 1, 58, marker + 2, 72, tension >= 85 ? RED : WHITE);

        int stamina = Math.round(100 - AtzenClient.displayedProgress);
        text(graphics, translate("hud.atzen.stamina", stamina), 12, 79, MUTED);
        graphics.fill(12, 92, 12 + barWidth, 98, 0xFF293743);
        graphics.fill(12, 92, 12 + Math.round(barWidth * stamina / 100f), 98, CYAN);

        text(graphics, translate(AtzenClient.reeling ? "hud.atzen.reeling" : "hud.atzen.hold",
                AtzenClient.reelKey.getTranslatedKeyMessage()), 12, 108, AtzenClient.reeling ? CYAN : WHITE);
        text(graphics, translate("hud.atzen.controls", AtzenClient.cancelKey.getTranslatedKeyMessage()), 12, 124, MUTED);
        graphics.pose().popMatrix();
    }

    private static void text(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.text(Minecraft.getInstance().font, text, x, y, color, false);
    }

    private static String translate(String key, Object... arguments) {
        return Component.translatable(key, arguments).getString();
    }
}
