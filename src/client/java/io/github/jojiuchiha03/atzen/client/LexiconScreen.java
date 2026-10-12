package io.github.jojiuchiha03.atzen.client;

import io.github.jojiuchiha03.atzen.fish.AtzenFishRegistry;
import io.github.jojiuchiha03.atzen.fish.FishCatch;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.progress.Area;
import io.github.jojiuchiha03.atzen.progress.Progression;
import io.github.jojiuchiha03.atzen.progress.Quest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The fish lexicon (Anforderung 20): every species with how often and how big it was caught (unknown ones stay
 * hidden), the open orders, the fishing grounds, the server records and ranking, and the angler's statistics.
 */
final class LexiconScreen extends Screen {
    private enum Tab { SPECIES, QUESTS, RECORDS, STATS }

    private static final int ROW = 20;
    private final CompoundTag data;
    private Tab tab = Tab.SPECIES;
    private int scroll;

    LexiconScreen(CompoundTag data) {
        super(Component.translatable("lexicon.atzen.title"));
        this.data = data;
    }

    @Override protected void init() {
        int x = width / 2 - 162;
        for (Tab each : Tab.values()) {
            addRenderableWidget(Button.builder(Component.translatable("lexicon.atzen.tab." + each.name().toLowerCase(Locale.ROOT)), button -> {
                tab = each;
                scroll = 0;
            }).bounds(x + each.ordinal() * 82, 8, 80, 20).build());
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.signum(vertical));
        return true;
    }

    private CompoundTag player() { return data.getCompoundOrEmpty("player"); }

    private static String kg(double value) { return String.format(Locale.GERMAN, "%.2f", value); }

    /** One line of the list: an optional icon, a text and its colour. */
    private record Line(ItemStack icon, Component text, int colour) {
        Line(Component text, int colour) { this(ItemStack.EMPTY, text, colour); }
    }

    private List<Line> lines() {
        List<Line> lines = new ArrayList<>();
        CompoundTag player = player();
        switch (tab) {
            case SPECIES -> {
                CompoundTag species = player.getCompoundOrEmpty("species");
                int known = 0;
                for (FishSpecies fish : FishSpecies.values()) if (species.contains(fish.id())) known++;
                lines.add(new Line(Component.translatable("lexicon.atzen.known", known, FishSpecies.values().length), 0xFFFFC94A));
                for (FishSpecies fish : FishSpecies.values()) {
                    CompoundTag entry = species.getCompoundOrEmpty(fish.id());
                    if (entry.isEmpty()) {
                        lines.add(new Line(Component.literal("??? ").append(Component.translatable(fish.rarityKey())), 0xFF707070));
                        continue;
                    }
                    Component text = Component.translatable(fish.translationKey()).withStyle(FishCatch.colour(fish.rarity()))
                            .append(Component.translatable("lexicon.atzen.species_line", entry.getIntOr("count", 0),
                                    kg(entry.getDoubleOr("kg", 0)), String.format(Locale.GERMAN, "%.0f", entry.getDoubleOr("cm", 0)))
                                    .withStyle(ChatFormatting.GRAY));
                    lines.add(new Line(new ItemStack(AtzenFishRegistry.fishItem(fish)), text, 0xFFFFFFFF));
                }
            }
            case QUESTS -> {
                ListTag quests = data.getListOrEmpty("quests");
                for (int i = 0; i < quests.size(); i++) {
                    CompoundTag q = quests.getCompoundOrEmpty(i);
                    Quest quest = new Quest(q.getStringOr("id", ""), Quest.Kind.valueOf(q.getStringOr("kind", "SPECIES")),
                            q.getStringOr("target", ""), q.getDoubleOr("amount", 0), q.getIntOr("count", 1), q.getIntOr("emeralds", 0),
                            q.getIntOr("experience", 0), q.getBooleanOr("weekly", false));
                    boolean done = q.getBooleanOr("done", false);
                    Component text = Component.translatable(quest.weekly() ? "lexicon.atzen.weekly" : "lexicon.atzen.daily")
                            .append(Progression.questText(quest))
                            .append(Component.translatable("lexicon.atzen.quest_progress", Math.min(q.getIntOr("progress", 0), quest.count()),
                                    quest.count(), quest.emeralds()));
                    lines.add(new Line(text, done ? 0xFF5BE38A : 0xFFFFFFFF));
                }
                lines.add(new Line(Component.empty(), 0));
                lines.add(new Line(Component.translatable("lexicon.atzen.areas"), 0xFFFFC94A));
                int level = data.getIntOr("level", 0);
                for (Area area : Area.values()) {
                    lines.add(new Line(Component.translatable("lexicon.atzen.area_line", Component.translatable(area.translationKey()), area.level()),
                            area.open(level) ? 0xFF5BE38A : 0xFF909090));
                }
                if (data.contains("tournament_seconds")) {
                    lines.add(new Line(Component.translatable("lexicon.atzen.tournament_running", data.getLongOr("tournament_seconds", 0) / 60), 0xFFFFC94A));
                }
            }
            case RECORDS -> {
                CompoundTag server = data.getCompoundOrEmpty("server");
                lines.add(new Line(Component.translatable("lexicon.atzen.ranking"), 0xFFFFC94A));
                ListTag top = server.getListOrEmpty("top");
                for (int i = 0; i < top.size(); i++) {
                    CompoundTag entry = top.getCompoundOrEmpty(i);
                    lines.add(new Line(Component.literal((i + 1) + ". ").append(Component.translatable("entity.atzen." + entry.getStringOr("species", "")))
                            .append(" – " + kg(entry.getDoubleOr("kg", 0)) + " kg – " + entry.getStringOr("angler", "")), 0xFFFFFFFF));
                }
                lines.add(new Line(Component.empty(), 0));
                lines.add(new Line(Component.translatable("lexicon.atzen.records"), 0xFFFFC94A));
                CompoundTag records = server.getCompoundOrEmpty("records");
                for (FishSpecies fish : FishSpecies.values()) {
                    CompoundTag best = records.getCompoundOrEmpty(fish.id());
                    if (best.isEmpty()) continue;
                    lines.add(new Line(new ItemStack(AtzenFishRegistry.fishItem(fish)), Component.translatable(fish.translationKey())
                            .append(" – " + kg(best.getDoubleOr("kg", 0)) + " kg – " + best.getStringOr("angler", "")), 0xFFFFFFFF));
                }
            }
            case STATS -> {
                long xp = player.getLongOr("xp", 0);
                lines.add(new Line(Component.translatable("lexicon.atzen.level", data.getIntOr("level", 0), xp, data.getLongOr("next", 0)), 0xFFFFC94A));
                lines.add(new Line(Component.translatable("lexicon.atzen.stat.catches", player.getIntOr("catches", 0)), 0xFFFFFFFF));
                lines.add(new Line(Component.translatable("lexicon.atzen.stat.released", player.getIntOr("released", 0)), 0xFFFFFFFF));
                lines.add(new Line(Component.translatable("lexicon.atzen.stat.bosses", player.getIntOr("bosses", 0)), 0xFFFFFFFF));
                lines.add(new Line(Component.translatable("lexicon.atzen.stat.earned",
                        String.format(Locale.GERMAN, "%.1f", player.getDoubleOr("earned", 0))), 0xFFFFFFFF));
            }
        }
        return lines;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractRenderState(graphics, mouseX, mouseY, partial);
        int left = width / 2 - 162, top = 36, visible = (height - top - 10) / ROW;
        graphics.fill(left - 4, top - 4, left + 330, height - 6, 0xC0101820);
        List<Line> lines = lines();
        scroll = Math.min(scroll, Math.max(0, lines.size() - visible));
        for (int i = 0; i < visible && i + scroll < lines.size(); i++) {
            Line line = lines.get(i + scroll);
            int y = top + i * ROW;
            if (!line.icon().isEmpty()) graphics.item(line.icon(), left, y);
            graphics.text(font, line.text(), left + (line.icon().isEmpty() ? 0 : 20), y + 4, line.colour(), true);
        }
    }
}
