package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * A slim line-tension gauge just above the hotbar during a fight: the green band is where reeling pays best, the
 * red end snaps the line, the white mark is the tension now. Above it one word says what to do: hold R, ease off,
 * let go, or brace for the fish's run. A thin bar underneath shows how much fight the fish has left.
 */
final class TensionGauge {
    private static final int WIDTH = 120;

    private TensionGauge() { }

    enum Advice {
        REEL(0xFF5BE38A), EASE(0xFFFFD45B), RELEASE(0xFFFF5B6E), BRACE(0xFFFF9A3C), SLACK(0xFF5BE38A);

        final int colour;

        Advice(int colour) { this.colour = colour; }

        String key() { return "hud.atzen.advice." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    static Advice advice() {
        var state = AtzenClient.state;
        float tension = AtzenClient.displayedTension;
        if (state.slack()) return Advice.SLACK;
        if (state.phase() == FishingBattle.Phase.SURGE || tension >= 82) return Advice.RELEASE;
        if (state.phase() == FishingBattle.Phase.WARNING) return Advice.BRACE;
        if (tension > FishingBattle.SWEET_MAX) return Advice.EASE;
        return Advice.REEL;
    }

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var state = AtzenClient.state;
        if (state == null || state.result() != FishingBattle.Result.ACTIVE) return;
        Minecraft client = Minecraft.getInstance();
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = graphics.guiHeight() - 66;

        // the bar: zones, then the mark
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + 6, 0xC0101820);
        graphics.fill(x, y, x + WIDTH, y + 5, 0xFF2A3540);
        graphics.fill(x + (int) (WIDTH * FishingBattle.SWEET_MIN / 100), y, x + (int) (WIDTH * FishingBattle.SWEET_MAX / 100), y + 5, 0xFF2F8A5A);
        graphics.fill(x + (int) (WIDTH * 0.82), y, x + WIDTH, y + 5, 0xFF9A2F42);
        int mark = x + Math.clamp(Math.round(WIDTH * AtzenClient.displayedTension / 100f), 1, WIDTH - 1);
        Advice advice = advice();
        graphics.fill(mark - 1, y - 2, mark + 1, y + 7, advice == Advice.RELEASE ? 0xFFFF5B6E : 0xFFFFFFFF);

        // the fish's remaining strength
        int stamina = Math.round(WIDTH * (100 - AtzenClient.displayedProgress) / 100f);
        graphics.fill(x, y + 7, x + WIDTH, y + 8, 0x80303A44);
        graphics.fill(x, y + 7, x + stamina, y + 8, 0xFF4CDED0);

        // what to do now; it blinks when it is urgent
        boolean urgent = advice == Advice.RELEASE || advice == Advice.SLACK;
        if (urgent && (AtzenClient.cameraTicks / 4) % 2 == 1) return;
        Component text = Component.translatable(advice.key(), AtzenClient.reelKey.getTranslatedKeyMessage());
        graphics.text(client.font, text, (graphics.guiWidth() - client.font.width(text)) / 2, y - 11, advice.colour, true);
    }
}
