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
        int x = width / 2 - 168;
        for (Tab each : Tab.values()) {
            addRenderableWidget(Button.builder(Component.translatable("lexicon.atzen.tab." + each.name().toLowerCase(Locale.ROOT)), button -> {
                tab = each;
                scroll = 0;
            }).bounds(x + each.ordinal() * 84, 8, 82, 20).build());
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) Math.signum(vertical));
        return true;
    }

    private CompoundTag player() { return data.getCompoundOrEmpty("player"); }

    private static String kg(double value) { return String.format(Locale.GERMAN, "%.2f", value); }

    /** One row: an optional icon, a text, an optional right-aligned detail; a heading spans the row in gold. */
    private record Line(ItemStack icon, Component text, Component detail, int colour, boolean heading) {
        Line(Component text, int colour) { this(ItemStack.EMPTY, text, Component.empty(), colour, false); }
        Line(ItemStack icon, Component text, Component detail, int colour) { this(icon, text, detail, colour, false); }
        static Line heading(Component text) { return new Line(ItemStack.EMPTY, text, Component.empty(), 0xFFFFC94A, true); }
    }

    /** Where the fish lives, as in Tide's journal: shown for unknown fish too, as a hint where to look. */
    private static Component where(FishSpecies fish) {
        var text = Component.empty();
        boolean first = true;
        for (var habitat : fish.habitats()) {
            if (!first) text.append(", ");
            text.append(Component.translatable(habitat.translationKey()));
            first = false;
        }
        return text;
    }

    private List<Line> lines() {
        List<Line> lines = new ArrayList<>();
        CompoundTag player = player();
        switch (tab) {
            case SPECIES -> {
                CompoundTag species = player.getCompoundOrEmpty("species");
                for (FishSpecies fish : FishSpecies.values()) {
                    if (fish.boss() != null && !species.contains(fish.id())) continue;
                    CompoundTag entry = species.getCompoundOrEmpty(fish.id());
                    if (entry.isEmpty()) {
                        lines.add(new Line(ItemStack.EMPTY, Component.literal("???  ").append(Component.translatable(fish.rarityKey())),
                                where(fish), 0xFF6A747E));
                        continue;
                    }
                    Component detail = Component.translatable("lexicon.atzen.species_detail", entry.getIntOr("count", 0),
                            kg(entry.getDoubleOr("kg", 0)));
                    lines.add(new Line(new ItemStack(AtzenFishRegistry.fishItem(fish)),
                            Component.translatable(fish.translationKey()).withStyle(FishCatch.colour(fish.rarity())), detail, 0xFFFFFFFF));
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
                lines.add(Line.heading(Component.translatable("lexicon.atzen.areas")));
                int level = data.getIntOr("level", 0);
                for (Area area : Area.values()) {
                    lines.add(new Line(ItemStack.EMPTY, Component.translatable(area.translationKey()),
                            Component.translatable("lexicon.atzen.area_level", area.level()), area.open(level) ? 0xFF5BE38A : 0xFF8A949E));
                }
                if (data.contains("tournament_seconds")) {
                    lines.add(new Line(Component.translatable("lexicon.atzen.tournament_running", data.getLongOr("tournament_seconds", 0) / 60), 0xFFFFC94A));
                }
            }
            case RECORDS -> {
                CompoundTag server = data.getCompoundOrEmpty("server");
                lines.add(Line.heading(Component.translatable("lexicon.atzen.ranking")));
                ListTag top = server.getListOrEmpty("top");
                for (int i = 0; i < top.size(); i++) {
                    CompoundTag entry = top.getCompoundOrEmpty(i);
                    lines.add(new Line(ItemStack.EMPTY, Component.literal((i + 1) + ". ").append(Component.translatable("entity.atzen." + entry.getStringOr("species", "")))
                            .append("  " + entry.getStringOr("angler", "")), Component.literal(kg(entry.getDoubleOr("kg", 0)) + " kg"),
                            i == 0 ? 0xFFFFC94A : 0xFFFFFFFF));
                }
                lines.add(Line.heading(Component.translatable("lexicon.atzen.records")));
                CompoundTag records = server.getCompoundOrEmpty("records");
                for (FishSpecies fish : FishSpecies.values()) {
                    CompoundTag best = records.getCompoundOrEmpty(fish.id());
                    if (best.isEmpty()) continue;
                    lines.add(new Line(new ItemStack(AtzenFishRegistry.fishItem(fish)), Component.translatable(fish.translationKey())
                            .append("  " + best.getStringOr("angler", "")), Component.literal(kg(best.getDoubleOr("kg", 0)) + " kg"), 0xFFFFFFFF));
                }
            }
            case STATS -> {
                long xp = player.getLongOr("xp", 0);
                lines.add(Line.heading(Component.translatable("lexicon.atzen.level", data.getIntOr("level", 0), xp, data.getLongOr("next", 0))));
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
        int panelWidth = 336, left = width / 2 - panelWidth / 2, top = 36, bottom = height - 10;
        // the book: a dark page with a gold edge, the title and how much is discovered
        graphics.fill(left, top - 2, left + panelWidth, bottom, 0xE0141A20);
        graphics.outline(left, top - 2, panelWidth, bottom - top + 2, 0xFF6A5420);
        graphics.fill(left, top - 2, left + panelWidth, top - 1, 0xFFFFC94A);
        CompoundTag species = player().getCompoundOrEmpty("species");
        int known = 0, total = 0;
        for (FishSpecies fish : FishSpecies.values()) {
            if (fish.boss() != null) continue;
            total++;
            if (species.contains(fish.id())) known++;
        }
        graphics.text(font, title, left + 8, top + 4, 0xFFFFC94A, true);
        Component progress = Component.translatable("lexicon.atzen.known", known, total);
        graphics.text(font, progress, left + panelWidth - 8 - font.width(progress), top + 4, 0xFFB8C4CC, false);
        int barLeft = left + 8, barWidth = panelWidth - 16;
        graphics.fill(barLeft, top + 15, barLeft + barWidth, top + 17, 0xFF26303A);
        graphics.fill(barLeft, top + 15, barLeft + barWidth * known / Math.max(1, total), top + 17, 0xFFFFC94A);

        int listTop = top + 22, visible = Math.max(1, (bottom - listTop - 4) / ROW);
        List<Line> lines = lines();
        scroll = Math.min(scroll, Math.max(0, lines.size() - visible));
        for (int i = 0; i < visible && i + scroll < lines.size(); i++) {
            Line line = lines.get(i + scroll);
            int y = listTop + i * ROW;
            if (line.heading()) {
                graphics.fill(left + 4, y + 2, left + panelWidth - 4, y + ROW - 2, 0x40FFC94A);
                graphics.text(font, line.text(), left + 10, y + 6, line.colour(), true);
                continue;
            }
            if ((i + scroll) % 2 == 0) graphics.fill(left + 4, y, left + panelWidth - 4, y + ROW, 0x18FFFFFF);
            int textLeft = left + 30;
            if (!line.icon().isEmpty()) graphics.item(line.icon(), left + 8, y + 2);
            else graphics.fill(left + 9, y + 4, left + 23, y + 16, 0xFF20262C);
            graphics.text(font, line.text(), textLeft, y + 6, line.colour(), true);
            int detailWidth = font.width(line.detail());
            if (detailWidth > 0) graphics.text(font, line.detail(), left + panelWidth - 10 - detailWidth, y + 6, 0xFF8A98A4, false);
        }
        // a slim scroll bar on the right edge
        if (lines.size() > visible) {
            int trackTop = listTop, trackHeight = visible * ROW;
            int thumb = Math.max(10, trackHeight * visible / lines.size());
            int offset = (trackHeight - thumb) * scroll / Math.max(1, lines.size() - visible);
            graphics.fill(left + panelWidth - 3, trackTop + offset, left + panelWidth - 1, trackTop + offset + thumb, 0xFF8A7A50);
        }
    }
}
