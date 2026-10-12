package io.github.jojiuchiha03.atzen.progress;

import io.github.jojiuchiha03.atzen.Atzen;
import io.github.jojiuchiha03.atzen.fish.FishSpecies;
import io.github.jojiuchiha03.atzen.fish.FishSpecimen;
import io.github.jojiuchiha03.atzen.fish.Habitat;
import io.github.jojiuchiha03.atzen.item.AtzenItems;
import io.github.jojiuchiha03.atzen.network.LexiconPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The angler's progress (Anforderung 20): experience and level, the fish lexicon (every species caught, how many,
 * the best one), statistics, daily and weekly orders, market credit, server-wide records and a ranking of the
 * heaviest catches, and tournaments. Levels open fishing grounds (Anforderung 19, see {@link Area}).
 *
 * <p>Player data lives in a persistent attachment on the player, server data on the overworld.</p>
 */
public final class Progression {
    private Progression() { }

    public static final AttachmentType<CompoundTag> PLAYER = AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC).copyOnDeath().initializer(CompoundTag::new).buildAndRegister(Atzen.id("angler"));
    public static final AttachmentType<CompoundTag> SERVER = AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC).initializer(CompoundTag::new).buildAndRegister(Atzen.id("records"));

    public static void initialize() { }

    /** Loads the performance tier kept with the world. */
    public static void load(MinecraftServer server) {
        io.github.jojiuchiha03.atzen.Performance.set(io.github.jojiuchiha03.atzen.Performance.of(server(server).getStringOr("performance", "normal")));
    }

    public static void setPerformance(MinecraftServer server, io.github.jojiuchiha03.atzen.Performance tier) {
        io.github.jojiuchiha03.atzen.Performance.set(tier);
        CompoundTag data = server(server);
        data.putString("performance", tier.id());
        saveServer(server, data);
    }

    // ------------------------------------------------------------------ data access

    public static CompoundTag data(ServerPlayer player) { return player.getAttachedOrCreate(PLAYER).copy(); }

    private static void save(ServerPlayer player, CompoundTag data) { player.setAttached(PLAYER, data); }

    private static CompoundTag server(MinecraftServer server) { return server.overworld().getAttachedOrCreate(SERVER).copy(); }

    private static void saveServer(MinecraftServer server, CompoundTag data) { server.overworld().setAttached(SERVER, data); }

    public static long day(ServerLevel level) { return level.getOverworldClockTime() / 24000; }

    public static int level(ServerPlayer player) { return Valuation.level(data(player).getLongOr("xp", 0)); }

    /** Counter per species for today (resets with the day); used against farming one fish. */
    private static int todayCount(CompoundTag data, String key, long day, String species) {
        CompoundTag today = data.getCompoundOrEmpty(key);
        if (today.getLongOr("day", -1) != day) return 0;
        return today.getCompoundOrEmpty("counts").getIntOr(species, 0);
    }

    private static void countToday(CompoundTag data, String key, long day, String species) {
        CompoundTag today = data.getCompoundOrEmpty(key);
        if (today.getLongOr("day", -1) != day) {
            today = new CompoundTag();
            today.putLong("day", day);
        }
        CompoundTag counts = today.getCompoundOrEmpty("counts");
        counts.putInt(species, counts.getIntOr(species, 0) + 1);
        today.put("counts", counts);
        data.put(key, today);
    }

    // ------------------------------------------------------------------ events

    /** A fish was landed. Gives experience, fills the lexicon, records, orders and the tournament. */
    public static void caught(ServerPlayer player, FishSpecimen fish, int quality, Habitat habitat) {
        ServerLevel level = (ServerLevel) player.level();
        long day = day(level);
        CompoundTag data = data(player);
        String id = fish.species().id();
        int before = Valuation.level(data.getLongOr("xp", 0));
        int xp = Valuation.experience(fish.species().rarity(), fish.sizeFraction(), Math.max(0, quality),
                todayCount(data, "caught_today", day, id));
        data.putLong("xp", data.getLongOr("xp", 0) + xp);
        data.putInt("catches", data.getIntOr("catches", 0) + 1);
        if (fish.species().boss() != null) data.putInt("bosses", data.getIntOr("bosses", 0) + 1);
        countToday(data, "caught_today", day, id);
        CompoundTag species = data.getCompoundOrEmpty("species");
        CompoundTag entry = species.getCompoundOrEmpty(id);
        entry.putInt("count", entry.getIntOr("count", 0) + 1);
        if (fish.weightKg() > entry.getDoubleOr("kg", 0)) {
            entry.putDouble("kg", fish.weightKg());
            entry.putDouble("cm", fish.lengthCm());
        }
        species.put(id, entry);
        data.put("species", species);
        quests(player, data, new Quest.Event(fish.species(), fish.weightKg(), habitat, false));
        save(player, data);
        player.giveExperiencePoints(Math.max(1, xp / 5));
        player.sendSystemMessage(Component.translatable("progress.atzen.xp", xp).withStyle(ChatFormatting.AQUA), true);
        int after = Valuation.level(data.getLongOr("xp", 0));
        if (after > before) levelUp(player, after);
        // rare catches sometimes bring mother-of-pearl, the material for rod upgrades
        if (fish.species().rarity().ordinal() >= FishSpecies.Rarity.EPIC.ordinal() && player.getRandom().nextFloat() < 0.35f) {
            player.spawnAtLocation(level, new ItemStack(AtzenItems.MOTHER_OF_PEARL));
        }
        record(player, fish);
    }

    public static void released(ServerPlayer player, FishSpecimen fish, Habitat habitat) {
        CompoundTag data = data(player);
        data.putInt("released", data.getIntOr("released", 0) + 1);
        int xp = 2 + fish.species().rarity().ordinal() * 2;
        data.putLong("xp", data.getLongOr("xp", 0) + xp);
        quests(player, data, new Quest.Event(fish.species(), fish.weightKg(), habitat, true));
        save(player, data);
        player.sendSystemMessage(Component.translatable("progress.atzen.released", xp).withStyle(ChatFormatting.GREEN), true);
    }

    private static void levelUp(ServerPlayer player, int level) {
        player.sendSystemMessage(Component.translatable("progress.atzen.level_up", level).withStyle(ChatFormatting.GOLD));
        for (Area area : Area.values()) {
            if (area.level() == level) {
                player.sendSystemMessage(Component.translatable("progress.atzen.area_unlocked",
                        Component.translatable(area.translationKey())).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static void quests(ServerPlayer player, CompoundTag data, Quest.Event event) {
        long day = day((ServerLevel) player.level());
        CompoundTag progress = data.getCompoundOrEmpty("quests");
        List<Quest> active = new ArrayList<>(Quest.daily(day));
        active.addAll(Quest.weekly(day / 7));
        for (Quest quest : active) {
            if (progress.getBooleanOr(quest.id() + ".done", false) || !quest.counts(event)) continue;
            int count = progress.getIntOr(quest.id(), 0) + 1;
            progress.putInt(quest.id(), count);
            if (count >= quest.count()) {
                progress.putBoolean(quest.id() + ".done", true);
                data.putLong("xp", data.getLongOr("xp", 0) + quest.experience());
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, quest.emeralds()));
                player.sendSystemMessage(Component.translatable("progress.atzen.quest_done", questText(quest),
                        quest.emeralds(), quest.experience()).withStyle(ChatFormatting.GREEN));
            }
        }
        // forget orders older than two weeks
        for (String key : List.copyOf(progress.keySet())) {
            String number = key.replaceAll("^[dw](\\d+).*$", "$1");
            if (number.matches("\\d+") && Long.parseLong(number) < (key.startsWith("w") ? day / 7 - 2 : day - 14)) progress.remove(key);
        }
        data.put("quests", progress);
    }

    public static Component questText(Quest quest) {
        Component target = switch (quest.kind()) {
            case SPECIES -> Component.translatable("entity.atzen." + quest.target());
            case HABITAT -> Component.translatable("habitat.atzen." + quest.target().toLowerCase(Locale.ROOT));
            case RARITY -> Component.translatable("rarity.atzen." + quest.target().toLowerCase(Locale.ROOT));
            default -> Component.empty();
        };
        return Component.translatable(quest.translationKey(), quest.count(), target, String.format(Locale.GERMAN, "%.0f", quest.amount()));
    }

    // ------------------------------------------------------------------ market

    /** Sells one fish; returns emeralds paid (the market keeps the change as credit). */
    public static int sell(ServerPlayer player, FishSpecimen fish, int quality, double condition) {
        long day = day((ServerLevel) player.level());
        CompoundTag data = data(player);
        String id = fish.species().id();
        double value = Valuation.price(fish.species().rarity(), fish.weightKg(), quality, condition, todayCount(data, "sold_today", day, id));
        countToday(data, "sold_today", day, id);
        double credit = data.getDoubleOr("credit", 0) + value;
        int emeralds = (int) Math.floor(credit);
        data.putDouble("credit", credit - emeralds);
        data.putDouble("earned", data.getDoubleOr("earned", 0) + value);
        save(player, data);
        if (emeralds > 0) player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, emeralds));
        return emeralds;
    }

    // ------------------------------------------------------------------ records, ranking, tournament

    private static void record(ServerPlayer player, FishSpecimen fish) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        CompoundTag data = server(server);
        String name = player.getName().getString();
        CompoundTag records = data.getCompoundOrEmpty("records");
        CompoundTag best = records.getCompoundOrEmpty(fish.species().id());
        if (fish.weightKg() > best.getDoubleOr("kg", 0)) {
            best.putDouble("kg", fish.weightKg());
            best.putDouble("cm", fish.lengthCm());
            best.putString("angler", name);
            records.put(fish.species().id(), best);
            data.put("records", records);
            if (best.getDoubleOr("kg", 0) > 0) {
                player.sendSystemMessage(Component.translatable("progress.atzen.record",
                        Component.translatable(fish.species().translationKey())).withStyle(ChatFormatting.GOLD));
            }
        }
        // the ranking of the ten heaviest catches
        ListTag top = data.getListOrEmpty("top");
        List<CompoundTag> list = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) list.add(top.getCompoundOrEmpty(i));
        CompoundTag entry = new CompoundTag();
        entry.putString("species", fish.species().id());
        entry.putDouble("kg", fish.weightKg());
        entry.putString("angler", name);
        list.add(entry);
        list.sort(Comparator.comparingDouble((CompoundTag tag) -> tag.getDoubleOr("kg", 0)).reversed());
        ListTag trimmed = new ListTag();
        list.stream().limit(10).forEach(trimmed::add);
        data.put("top", trimmed);
        // a running tournament counts the heaviest fish of each angler (of the chosen species, if any)
        CompoundTag tournament = data.getCompoundOrEmpty("tournament");
        if (tournament.getBooleanOr("active", false)) {
            String species = tournament.getStringOr("species", "");
            if (species.isEmpty() || species.equals(fish.species().id())) {
                CompoundTag entries = tournament.getCompoundOrEmpty("entries");
                if (fish.weightKg() > entries.getDoubleOr(name, 0)) entries.putDouble(name, fish.weightKg());
                tournament.put("entries", entries);
                data.put("tournament", tournament);
            }
        }
        saveServer(server, data);
    }

    public static void startTournament(MinecraftServer server, int minutes, String species) {
        CompoundTag data = server(server);
        CompoundTag tournament = new CompoundTag();
        tournament.putBoolean("active", true);
        tournament.putLong("end", server.overworld().getGameTime() + minutes * 1200L);
        tournament.putString("species", species);
        data.put("tournament", tournament);
        saveServer(server, data);
        Component what = species.isEmpty() ? Component.translatable("progress.atzen.any_fish") : Component.translatable("entity.atzen." + species);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("progress.atzen.tournament_start", minutes, what)
                .withStyle(ChatFormatting.GOLD), false);
    }

    /** Ends the tournament if its time is up (or now): announces the podium and pays the winners. */
    public static void tick(MinecraftServer server, boolean force) {
        if (server.overworld().getGameTime() % 20 != 0 && !force) return;
        CompoundTag data = server(server);
        CompoundTag tournament = data.getCompoundOrEmpty("tournament");
        if (!tournament.getBooleanOr("active", false)) return;
        if (!force && server.overworld().getGameTime() < tournament.getLongOr("end", 0)) return;
        CompoundTag entries = tournament.getCompoundOrEmpty("entries");
        List<String> anglers = new ArrayList<>(entries.keySet());
        anglers.sort(Comparator.comparingDouble((String name) -> entries.getDoubleOr(name, 0)).reversed());
        server.getPlayerList().broadcastSystemMessage(Component.translatable("progress.atzen.tournament_end").withStyle(ChatFormatting.GOLD), false);
        int[] prizes = {16, 8, 4};
        for (int place = 0; place < Math.min(3, anglers.size()); place++) {
            String name = anglers.get(place);
            server.getPlayerList().broadcastSystemMessage(Component.translatable("progress.atzen.tournament_place", place + 1, name,
                    String.format(Locale.GERMAN, "%.2f", entries.getDoubleOr(name, 0))), false);
            ServerPlayer winner = server.getPlayerList().getPlayerByName(name);
            if (winner != null) winner.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, prizes[place]));
        }
        tournament.putBoolean("active", false);
        data.put("tournament", tournament);
        data.put("last_tournament", entries);
        saveServer(server, data);
    }

    // ------------------------------------------------------------------ lexicon

    /** Everything the lexicon screen shows, sent when the player opens it. */
    public static void openLexicon(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, LexiconPayload.TYPE)) return;
        MinecraftServer server = player.level().getServer();
        CompoundTag tag = new CompoundTag();
        CompoundTag data = data(player);
        long xp = data.getLongOr("xp", 0);
        int level = Valuation.level(xp);
        tag.put("player", data);
        tag.putInt("level", level);
        tag.putLong("next", Valuation.experienceFor(level + 1));
        tag.putLong("this", Valuation.experienceFor(level));
        long day = day((ServerLevel) player.level());
        ListTag quests = new ListTag();
        List<Quest> active = new ArrayList<>(Quest.daily(day));
        active.addAll(Quest.weekly(day / 7));
        CompoundTag progress = data.getCompoundOrEmpty("quests");
        for (Quest quest : active) {
            CompoundTag q = new CompoundTag();
            q.putString("id", quest.id());
            q.putString("kind", quest.kind().name());
            q.putString("target", quest.target());
            q.putDouble("amount", quest.amount());
            q.putInt("progress", progress.getIntOr(quest.id(), 0));
            q.putInt("count", quest.count());
            q.putBoolean("done", progress.getBooleanOr(quest.id() + ".done", false));
            q.putBoolean("weekly", quest.weekly());
            q.putInt("emeralds", quest.emeralds());
            q.putInt("experience", quest.experience());
            quests.add(q);
        }
        tag.put("quests", quests);
        if (server != null) {
            CompoundTag serverData = server(server);
            tag.put("server", serverData);
            CompoundTag tournament = serverData.getCompoundOrEmpty("tournament");
            if (tournament.getBooleanOr("active", false)) {
                tag.putLong("tournament_seconds", Math.max(0, (tournament.getLongOr("end", 0) - server.overworld().getGameTime()) / 20));
            }
        }
        ServerPlayNetworking.send(player, new LexiconPayload(tag));
    }
}
