package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.battle.Boss;
import io.github.jojiuchiha03.atzen.battle.FishAction;
import io.github.jojiuchiha03.atzen.battle.FishingBattle;
import io.github.jojiuchiha03.atzen.battle.Landing;
import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.network.BattlePayload;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The fight panel above the hotbar, as wide as the hotbar: the fish with its name and distance, the line's tension
 * (green up to where the drag slips, red beyond) and the fish's strength. Above it one prompt says which key to
 * press now – R to reel, F to give line, right-click to land. A boss shows its name and phase at the top, a landed
 * fish a catch card.
 */
final class FightHud {
    private static final int WIDTH = 182;
    private static final int HEIGHT = 36;
    private static final int PANEL = 0xC0121820;
    private static final int TRACK = 0xFF26303A;
    private static final int GREEN = 0xFF4FD08A;
    private static final int ORANGE = 0xFFFFA040;
    private static final int RED = 0xFFFF5B6E;

    private FightHud() { }

    /** What to do now: one key, one word. */
    enum Prompt {
        LAND(GREEN), NET(GREEN), WARN(ORANGE), WAIT(ORANGE), GIVE(ORANGE), GIVE_NOW(RED), REEL(GREEN), TIGHTEN(0xFFFFD45B);

        final int colour;

        Prompt(int colour) { this.colour = colour; }

        String key() { return "hud.atzen.prompt." + name().toLowerCase(Locale.ROOT); }

        boolean blinks() { return this == GIVE_NOW || this == WARN || this == LAND || this == NET; }
    }

    /** The same rules the fight runs on (see FishingBattle): reel while it rests, stop when it pulls, give in jumps. */
    static Prompt prompt(BattlePayload state) {
        if (state.has(BattlePayload.LANDABLE)) return state.netWindow() >= 0 ? Prompt.NET : Prompt.LAND;
        boolean giving = state.has(BattlePayload.GIVING);
        boolean away = state.pending() == null && (state.action() == FishAction.RUN || state.action() == FishAction.DIVE
                || state.action() == FishAction.SIDE);
        if (state.has(BattlePayload.TWIST) || away && !giving && AtzenClient.displayedTension >= state.drag() * 0.95f) return Prompt.GIVE_NOW;
        if (state.pending() != null) return Prompt.WARN;
        return switch (state.action()) {
            case JUMP, SHAKE -> Prompt.GIVE;
            case RUN, DIVE, SIDE -> Prompt.WAIT;
            default -> state.has(BattlePayload.SLACK) ? Prompt.TIGHTEN : Prompt.REEL;
        };
    }

    private static KeyMapping keyOf(Prompt prompt) {
        return switch (prompt) {
            case GIVE, GIVE_NOW -> AtzenClient.giveKey;
            case REEL, TIGHTEN -> AtzenClient.reelKey;
            default -> null;
        };
    }

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        if (AtzenClient.trophyTicks > 0 && AtzenClient.trophy != null) catchCard(graphics, font, AtzenClient.trophy);
        var state = AtzenClient.state;
        if (state == null || state.result() != FishingBattle.Result.ACTIVE) return;
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = graphics.guiHeight() - 59 - HEIGHT;
        FishSpecies species = state.species();
        int rarity = rarityColour(species);

        // the panel: a thin line in the fish's rarity colour on top
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, PANEL);
        graphics.fill(x, y, x + WIDTH, y + 1, rarity);
        graphics.item(new ItemStack(AtzenFishRegistry.fishItem(species)), x + 3, y + 3);
        graphics.text(font, Component.translatable(species.translationKey()), x + 22, y + 4, rarity, true);
        Component right = state.hookHold() < 35 ? Component.translatable("hud.atzen.hook_loose")
                : Component.translatable("hud.atzen.distance", Math.round(state.distance()));
        graphics.text(font, right, x + WIDTH - 4 - font.width(right), y + 4, state.hookHold() < 35 ? RED : 0xFF9AA8B4, true);

        // the line: green up to the drag, red beyond; the white mark is the tension
        int barX = x + 22, barW = WIDTH - 26, barY = y + 15;
        float tension = Math.clamp(AtzenClient.displayedTension, 0, 1);
        int drag = Math.clamp(Math.round(barW * state.drag()), 1, barW - 1);
        graphics.fill(barX, barY, barX + barW, barY + 6, TRACK);
        graphics.fill(barX + drag, barY, barX + barW, barY + 6, 0xFF4A2630);
        int fill = Math.round(barW * tension);
        float share = state.drag() <= 0 ? 1 : tension / state.drag();
        int colour = share < 0.75f ? GREEN : share < 0.97f ? 0xFFE8C04A : RED;
        graphics.fill(barX, barY + 1, barX + Math.min(fill, barW), barY + 5, colour);
        graphics.fill(barX + drag, barY - 1, barX + drag + 1, barY + 7, 0xFF8AE8F0);
        int mark = barX + Math.clamp(fill, 1, barW - 1);
        graphics.fill(mark - 1, barY - 1, mark + 1, barY + 7, 0xFFFFFFFF);

        // the fish's strength
        int stamina = Math.round(barW * Math.clamp(AtzenClient.displayedStamina, 0, 1));
        graphics.fill(barX, y + 25, barX + barW, y + 28, TRACK);
        graphics.fill(barX, y + 25, barX + stamina, y + 28, 0xFF4CC8E0);
        graphics.text(font, Component.translatable("hud.atzen.strength"), x + 3, y + 23, 0xFF6A7A88, false);

        // the prompt: a key cap and one word; it blinks when it is urgent
        Prompt prompt = prompt(state);
        boolean hidden = prompt.blinks() && (AtzenClient.fightTicks / 5) % 2 == 1;
        if (!hidden) prompt(graphics, font, prompt, graphics.guiWidth() / 2, y - 14);

        if (AtzenClient.fightTicks < 50 && state.strike() > 0) {
            String quality = state.strike() >= 90 ? "perfect" : state.strike() >= 72 ? "good" : "late";
            Component strike = Component.translatable("hud.atzen.strike." + quality);
            graphics.centeredText(font, strike, graphics.guiWidth() / 2, y - 27, 0xFFE0E6EA);
        }
        if (state.has(BattlePayload.LANDABLE) && state.netWindow() >= 0) netGauge(graphics, x, y - 28, state);
        Boss boss = species.boss();
        if (boss != null) bossBar(graphics, font, state, boss);
    }

    /** [R] Reel in – a key cap like on the keyboard, then the word. */
    private static void prompt(GuiGraphicsExtractor graphics, Font font, Prompt prompt, int centre, int y) {
        KeyMapping key = keyOf(prompt);
        Component word = Component.translatable(prompt.key());
        Component cap = key == null ? null : key.getTranslatedKeyMessage();
        int capWidth = cap == null ? 0 : Math.max(11, font.width(cap) + 6);
        int total = capWidth + (cap == null ? 0 : 4) + font.width(word);
        int x = centre - total / 2;
        if (cap != null) {
            graphics.fill(x, y - 2, x + capWidth, y + 10, 0xFF0C1014);
            graphics.outline(x, y - 2, capWidth, 12, prompt.colour);
            graphics.centeredText(font, cap, x + capWidth / 2, y, 0xFFFFFFFF);
            x += capWidth + 4;
        }
        graphics.text(font, word, x, y, prompt.colour, true);
    }

    /**
     * The landing gauge: the fish twists in front of the net; the green field is the net's opening. Scoop
     * (right-click) while the marker is inside it. Same formula as the server, so what is seen is what counts.
     */
    private static void netGauge(GuiGraphicsExtractor graphics, int x, int y, BattlePayload state) {
        int half = WIDTH / 2, centre = x + half;
        int window = Math.round(half * state.netWindow());
        graphics.fill(x, y, x + WIDTH, y + 6, 0xFF3A2A2A);
        graphics.fill(centre - window, y, centre + window, y + 6, 0xFF2F8A5A);
        double ticks = state.landTicks() + AtzenClient.packetAge;
        int marker = centre + (int) Math.round(Landing.marker((int) ticks) * half);
        boolean inside = Math.abs(Landing.marker((int) ticks)) <= state.netWindow();
        graphics.fill(marker - 2, y - 2, marker + 2, y + 8, inside ? 0xFFFFFFFF : 0xFFFF7A5C);
    }

    private static void bossBar(GuiGraphicsExtractor graphics, Font font, BattlePayload state, Boss boss) {
        int width = 182;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 16;
        Component name = Component.translatable(boss.translationKey()).append(" – ")
                .append(Component.translatable(boss.phase(state.bossPhase()).translationKey(boss)));
        graphics.centeredText(font, name, graphics.guiWidth() / 2, y - 11, 0xFFFFC94A);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 5, 0xC0101820);
        graphics.fill(x, y, x + Math.round(width * Math.clamp(AtzenClient.displayedStamina, 0, 1)), y + 4, 0xFFC0392B);
        for (int phase = 0; phase < boss.phases(); phase++) {
            int pip = x + width / 2 + (phase - boss.phases() / 2) * 10;
            graphics.fill(pip, y + 7, pip + 6, y + 9, phase < state.bossPhase() ? 0xFF505050 : phase == state.bossPhase() ? 0xFFFFC94A : 0xFFC0392B);
        }
    }

    /** The catch: a card at the top with the fish, its rarity, its size and how cleanly it was fought. */
    private static void catchCard(GuiGraphicsExtractor graphics, Font font, BattlePayload state) {
        FishSpecies species = state.species();
        int colour = rarityColour(species);
        int width = 210, x = (graphics.guiWidth() - width) / 2, y = 30;
        graphics.fill(x, y, x + width, y + 44, 0xD0101418);
        graphics.outline(x, y, width, 44, colour);
        graphics.fill(x + 6, y + 8, x + 34, y + 36, 0xFF1C242C);
        graphics.item(new ItemStack(AtzenFishRegistry.fishItem(species)), x + 12, y + 14);
        int text = x + 42;
        graphics.text(font, Component.translatable(species.translationKey()), text, y + 6, colour, true);
        Component rarity = Component.translatable(species.rarityKey());
        graphics.text(font, rarity, x + width - 6 - font.width(rarity), y + 6, 0xFF8A98A4, false);
        graphics.text(font, Component.translatable("catch.atzen.measurements", String.format(Locale.GERMAN, "%.1f", state.lengthCm()),
                String.format(Locale.GERMAN, "%.2f", state.weightKg())), text, y + 18, 0xFFFFFFFF, true);
        graphics.text(font, Component.translatable("hud.atzen.trophy_quality", state.quality()), text, y + 30, 0xFF7FD8C8, false);
    }

    static int rarityColour(FishSpecies species) {
        return switch (species.rarity()) {
            case COMMON -> 0xFFE8ECEF;
            case UNCOMMON -> 0xFF6CE07A;
            case RARE -> 0xFF5BC8FF;
            case EPIC -> 0xFFC880FF;
            case LEGENDARY -> 0xFFFFC94A;
            case MYTHIC -> 0xFFFF6060;
        };
    }
}
