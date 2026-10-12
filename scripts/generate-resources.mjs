import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../src/main/resources');
function json(path, value) {
  const target = resolve(root, path);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, JSON.stringify(value, null, 2) + '\n');
}

const items = {
  river_rod: ['Flussrute', 'River Rod', 'Weich und verzeihend (Hubkraft 4 kg, sehr flexibel), etwas schneller.', 'Soft and forgiving (4 kg lift, very supple), a little faster.'],
  iron_rod: ['Eisenrute', 'Iron Rod', 'Steif und kräftig: pumpt mit 7 kg, gibt aber jeden Ruck weiter.', 'Stiff and strong: pumps with 7 kg but passes every jolt on.'],
  tide_rod: ['Gezeitenrute', 'Tide Rod', 'Ausgewogen (6 kg), 15 % schnellerer Einzug.', 'Balanced (6 kg), 15% faster retrieve.'],
  carbon_rod: ['Carbonrute', 'Carbon Rod', 'Die beste Rute: 9 kg Hubkraft und trotzdem geschmeidig, 10 % schneller.', 'The best rod: 9 kg of lift and still supple, 10% faster.'],
  copper_reel: ['Kupfer-Stationärrolle', 'Copper Spinning Reel', 'Einsteiger: 120 m, 5 kg. Verzeihend: schlaffe Schnur kostet weniger Hakenhalt.', 'Beginner: 120 m, 5 kg. Forgiving: slack costs less hook hold.'],
  iron_reel: ['Eisen-Stationärrolle', 'Iron Spinning Reel', '160 m, 7 kg. Verzeihend bei schlaffer Schnur.', '160 m, 7 kg. Forgiving on slack.'],
  diamond_reel: ['Diamant-Stationärrolle', 'Diamond Spinning Reel', '200 m, 9 kg. Feinbremse: Rucke bei Kopfstößen und Sprüngen kommen viel schwächer an.', '200 m, 9 kg. Fine drag: jolts from head shakes and jumps arrive much weaker.'],
  baitcaster_reel: ['Baitcaster', 'Baitcaster', 'Sehr schnell (4 m/s), 110 m. Vorsicht: Flucht gegen lockere Bremse (≤ 3) gibt eine Perücke.', 'Very fast (4 m/s), 110 m. Careful: a run against a loose drag (≤ 3) tangles the spool.'],
  multiplier_reel: ['Multirolle', 'Multiplier Reel', 'Für Großfische: 350 m und 15 kg Kraftübersetzung, aber langsam.', 'For big fish: 350 m and 15 kg power gear, but slow.'],
  fly_reel: ['Fliegenrolle', 'Fly Reel', 'Nur 90 m, aber Pumpen zieht 60 % stärker; Fische unter 3 kg kämpfen schwächer.', 'Only 90 m, but pumping pulls 60% harder; fish under 3 kg fight weaker.'],
  echo_reel: ['Echorolle', 'Echo Reel', '180 m, 8 kg. Hört den Fisch: jede Aktion wird eine Sekunde früher angekündigt.', '180 m, 8 kg. Hears the fish: every action is announced a second earlier.'],
  tide_reel: ['Gezeitenrolle', 'Tide Reel', '220 m, 9 kg. Meeresfische erholen sich am Haken nicht mehr.', '220 m, 9 kg. Sea fish never get their breath back on the hook.'],
  netherite_reel: ['Netheritrolle', 'Netherite Reel', '300 m, 14 kg. Selbstbegrenzende Bremse: hält nie mehr als 90 % der Tragkraft.', '300 m, 14 kg. Self-limiting drag: never holds more than 90% of the breaking load.'],
  ancient_reel: ['Rolle des Alten', 'Reel of the Old One', 'Bossbeute. 320 m, 13 kg. Anker: Fluchten nehmen 35 % weniger Schnur.', 'Boss reward. 320 m, 13 kg. Anchor: runs take 35% less line.'],
  reed_reel: ['Schilfkrone', 'Reed Crown Reel', 'Bossbeute. Schnell, 180 m. Sprünge reißen den Haken nie aus, solange die Rute unten bleibt.', 'Boss reward. Fast, 180 m. Jumps never throw the hook while the rod stays down.'],
  current_reel: ['Strömungsrolle', 'Current Reel', 'Bossbeute. 280 m, 12 kg. Holt von selbst Schnur ein, solange der Fisch ruht.', 'Boss reward. 280 m, 12 kg. Takes in line on its own while the fish rests.'],
  deep_reel: ['Tiefenherz', 'Deep Heart Reel', 'Bossbeute. 300 m, 12 kg, schnell. Der Fisch ermüdet 25 % schneller und zeigt früher, was er vorhat.', 'Boss reward. 300 m, 12 kg, fast. The fish tires 25% faster and shows earlier what it will do.'],
  nylon_line: ['Monofile Schnur', 'Monofilament Line', 'Tragkraft 7 kg. Dehnt sich und schluckt Rucke, verzeiht viel.', 'Breaking load 7 kg. Stretches and soaks up jolts, forgives a lot.'],
  braided_line: ['Geflochtene Schnur', 'Braided Line', 'Tragkraft 14 kg, kaum Dehnung, direkt. Scheuert an Hindernissen schnell durch.', 'Breaking load 14 kg, hardly any stretch, direct. Frays fast on snags.'],
  carbon_line: ['Carbonschnur', 'Carbon Line', 'Die beste Schnur: 20 kg Tragkraft, etwas Dehnung, sehr abriebfest.', 'The best line: 20 kg breaking load, some stretch, very abrasion resistant.'],
  carbon_fiber: ['Kohlefaser', 'Carbon Fiber', 'Aus einem Kohleblock im Hochofen. Für Carbonrute und Carbonschnur.', 'From a coal block in a blast furnace. For the carbon rod and line.'],
  worm_bait: ['Regenwurm', 'Earthworm', 'Naturköder für fast alle: Friedfische, Forellen, Aal, Grundfische.', 'Natural bait for almost all: coarse fish, trout, eel, bottom fish.'],
  maggot_bait: ['Made', 'Maggot', 'Naturköder: Friedfische und Forellen lieben sie.', 'Natural bait: coarse fish and trout love them.'],
  corn_bait: ['Mais', 'Sweetcorn', 'Naturköder nur für Friedfische, Karpfen besonders.', 'Natural bait for coarse fish only, carp above all.'],
  bread_bait: ['Brotflocke', 'Bread Flake', 'Naturköder für Friedfische, Döbel und Rotfeder besonders.', 'Natural bait for coarse fish, chub and rudd above all.'],
  boilie_bait: ['Boilie', 'Boilie', 'Selektiv: nur Friedfische, dafür vor allem große Karpfen.', 'Selective: coarse fish only, but mostly big carp.'],
  baitfish_bait: ['Köderfisch', 'Baitfish', 'Für Raubfische in Süß- und Salzwasser, eher große Exemplare. Wels und Hecht besonders.', 'For predators in fresh and salt water, rather big ones. Wels and pike above all.'],
  fish_strip_bait: ['Fischfetzen', 'Fish Strip', 'Für Raub- und Grundfische im Meer und im Fluss.', 'For predators and bottom fish at sea and in rivers.'],
  larva_bait: ['Insektenlarve', 'Insect Larva', 'Für Forellen und Äschen, auch Friedfische.', 'For trout and grayling, coarse fish too.'],
  shrimp_bait: ['Garnele', 'Shrimp', 'Meeresköder für Grund- und Raubfische.', 'Sea bait for bottom fish and predators.'],
  glow_bait: ['Leuchtköder', 'Glow Bait', 'Lockt alle Fische, nachts besonders. Fluchten ziehen 20 % schwächer.', 'Draws every fish, at night most of all. Runs pull 20% weaker.'],
  hearty_bait: ['Kraftköder', 'Hearty Bait', 'Lockt fast alle Fische. Fisch ermüdet 25 % schneller, aber 12 % mehr Spannung.', 'Draws almost every fish. Fish tire 25% faster, but 12% more strain.'],
  spinner: ['Spinner', 'Spinner', 'Kunstköder für Barsch, Rapfen, Forelle und Hecht. Nutzt sich ab.', 'Lure for perch, asp, trout and pike. Wears down.'],
  spoon: ['Blinker', 'Spoon', 'Kunstköder für Raubfische im Süß- und Salzwasser.', 'Lure for predators in fresh and salt water.'],
  wobbler: ['Wobbler', 'Crankbait', 'Kunstköder für Raubfische, bringt eher große.', 'Lure for predators, tends to bring big ones.'],
  soft_lure: ['Gummifisch', 'Soft Lure', 'Kunstköder, Zander lieben ihn. Verschleißt schnell.', 'Lure, zander love it. Wears out fast.'],
  popper: ['Popper', 'Popper', 'Oberflächenköder fürs Meer: Wolfsbarsch, Barrakuda. Gehakte Fische springen mehr.', 'Surface lure for the sea: sea bass, barracuda. Hooked fish jump more.'],
  jig: ['Jig', 'Jig', 'Kunstköder für Grund und Freiwasser im Meer, Kabeljau und Seelachs besonders.', 'Lure for the bottom and open sea, cod and saithe above all.'],
  streamer: ['Streamer', 'Streamer', 'Fliege für Forellen und Raubfische. Am besten mit Fliegenrolle.', 'Fly for trout and predators. Best with a fly reel.'],
  dry_fly: ['Trockenfliege', 'Dry Fly', 'Für Forelle und Äsche. Braucht eine Fliegenrolle, sonst nur halb so gut.', 'For trout and grayling. Needs a fly reel, otherwise half as good.'],
  nymph: ['Nymphe', 'Nymph', 'Fliege für Forellen und Äschen unter Wasser. Am besten mit Fliegenrolle.', 'Sub-surface fly for trout and grayling. Best with a fly reel.'],
  ancient_bait: ['Uralter Grundköder', 'Ancient Bottom Bait', 'Bossköder: nachts in einem See oder Sumpf, mindestens 5 Blöcke tief, ruft er den Alten vom Grund.', 'Boss bait: at night in a lake or swamp at least 5 blocks deep, it calls the Old One of the Deep.'],
  reed_king_lure: ['Königswobbler', 'King\'s Crankbait', 'Bossköder: im Sumpf bei Regen oder in der Dämmerung lockt er den Schilfkönig.', 'Boss lure: in a swamp in the rain or at dusk and dawn, it draws the Reed King.'],
  current_bait: ['Strömungsköder', 'Current Bait', 'Bossköder: tagsüber in einem Fluss, mindestens 4 Blöcke tief, weckt er den Schwarzen Stör.', 'Boss bait: by day in a river at least 4 blocks deep, it wakes the Black Sturgeon.'],
  deep_light_bait: ['Tiefenlicht', 'Deep Light', 'Bossköder: in verborgenem Wasser unter y 0, ohne Himmel darüber, erscheint der Tiefenwächter.', 'Boss bait: in hidden water below y 0, out of sight of the sky, the Deep Warden appears.'],
  ancient_barbel: ['Uralte Welsbarte', 'Ancient Barbel', 'Trophäe des Alten vom Grund. Für die Rolle des Alten.', 'Trophy of the Old One of the Deep. For the Reel of the Old One.'],
  reed_crown: ['Schilfkrone', 'Reed Crown', 'Trophäe des Schilfkönigs. Für die Schilfkronen-Rolle.', 'Trophy of the Reed King. For the Reed Crown Reel.'],
  sturgeon_plate: ['Schwarze Knochenplatte', 'Black Bone Plate', 'Trophäe des Schwarzen Störs. Für die Strömungsrolle.', 'Trophy of the Black Sturgeon. For the Current Reel.'],
  warden_heart: ['Herz der Tiefe', 'Heart of the Deep', 'Trophäe des Tiefenwächters. Für das Tiefenherz.', 'Trophy of the Deep Warden. For the Deep Heart Reel.'],
  steady_line_tome: ['Zauberbuch: Schnurmeister', 'Tome of Steady Line', 'Rechtsklick mit Angel in der anderen Hand: Schnurmeister +1 (+8 % Tragkraft).', 'Use with a rod in the other hand: Steady Line +1 (+8% breaking load).'],
  reel_power_tome: ['Zauberbuch: Kurbelkraft', 'Tome of Reel Power', 'Rechtsklick mit Angel in der anderen Hand: Kurbelkraft +1 (+8 % Einzug und Kraft).', 'Use with a rod in the other hand: Reel Power +1 (+8% retrieve and force).'],
  calm_waters_tome: ['Zauberbuch: Wellenruhe', 'Tome of Calm Waters', 'Rechtsklick mit Angel in der anderen Hand: Wellenruhe +1 (Fluchten 12 % schwächer).', 'Use with a rod in the other hand: Calm Waters +1 (runs 12% weaker).'],
};

for (const id of Object.keys(items)) {
  json(`assets/atzen/models/item/${id}.json`, {
    parent: id.endsWith('_rod') ? 'minecraft:item/handheld_rod' : 'minecraft:item/generated',
    textures: { layer0: `atzen:item/${id}` },
  });
  json(`assets/atzen/items/${id}.json`, { model: { type: 'minecraft:model', model: `atzen:item/${id}` } });
}

const rods = ['river_rod', 'iron_rod', 'tide_rod', 'carbon_rod'].map(id => `atzen:${id}`);
json('data/atzen/tags/item/rods.json', { values: ['minecraft:fishing_rod', ...rods] });
for (const tag of ['fishing', 'durability', 'vanishing']) {
  json(`data/minecraft/tags/item/enchantable/${tag}.json`, { replace: false, values: rods });
}

const enchantments = { steady_line: 3, reel_power: 3, calm_waters: 2 };
for (const [id, maxLevel] of Object.entries(enchantments)) {
  json(`data/atzen/enchantment/${id}.json`, {
    description: { translate: `enchantment.atzen.${id}` }, supported_items: '#atzen:rods',
    weight: 4, max_level: maxLevel, min_cost: { base: 8, per_level_above_first: 9 },
    max_cost: { base: 35, per_level_above_first: 9 }, anvil_cost: 2,
    slots: ['mainhand', 'offhand'], effects: {},
  });
}
json('data/minecraft/tags/enchantment/in_enchanting_table.json', {
  replace: false, values: Object.keys(enchantments).map(id => `atzen:${id}`),
});

const recipes = {
  river_rod: ['minecraft:fishing_rod', 'minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:string'],
  iron_rod: ['atzen:river_rod', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:leather'],
  tide_rod: ['atzen:iron_rod', 'minecraft:prismarine_shard', 'minecraft:prismarine_shard', 'minecraft:prismarine_shard', 'minecraft:heart_of_the_sea', 'minecraft:diamond'],
  carbon_rod: ['atzen:iron_rod', 'atzen:carbon_fiber', 'atzen:carbon_fiber', 'atzen:carbon_fiber', 'minecraft:diamond'],
  copper_reel: ['minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:iron_nugget', 'minecraft:stick'],
  iron_reel: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:iron_nugget', 'minecraft:redstone'],
  diamond_reel: ['atzen:iron_reel', 'minecraft:diamond', 'minecraft:gold_ingot', 'minecraft:redstone'],
  baitcaster_reel: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:redstone', 'minecraft:quartz'],
  multiplier_reel: ['atzen:iron_reel', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:copper_block'],
  fly_reel: ['minecraft:iron_ingot', 'minecraft:leather', 'minecraft:string', 'minecraft:stick'],
  echo_reel: ['atzen:iron_reel', 'minecraft:echo_shard', 'minecraft:amethyst_shard', 'minecraft:redstone'],
  tide_reel: ['atzen:diamond_reel', 'minecraft:heart_of_the_sea', 'minecraft:prismarine_shard', 'minecraft:prismarine_shard'],
  netherite_reel: ['atzen:diamond_reel', 'minecraft:netherite_ingot'],
  ancient_reel: ['atzen:multiplier_reel', 'atzen:ancient_barbel', 'minecraft:gold_ingot'],
  reed_reel: ['atzen:baitcaster_reel', 'atzen:reed_crown', 'minecraft:emerald'],
  current_reel: ['atzen:multiplier_reel', 'atzen:sturgeon_plate', 'minecraft:prismarine_crystals'],
  deep_reel: ['atzen:echo_reel', 'atzen:warden_heart', 'minecraft:diamond'],
  nylon_line: ['minecraft:string', 'minecraft:string', 'minecraft:string', 'minecraft:slime_ball'],
  braided_line: ['atzen:nylon_line', 'minecraft:string', 'minecraft:string', 'minecraft:string'],
  carbon_line: ['atzen:braided_line', 'atzen:carbon_fiber', 'atzen:carbon_fiber'],
  worm_bait: { count: 6, ingredients: ['minecraft:rotten_flesh', 'minecraft:wheat_seeds'] },
  maggot_bait: { count: 8, ingredients: ['minecraft:rotten_flesh', 'minecraft:rotten_flesh'] },
  corn_bait: { count: 8, ingredients: ['minecraft:wheat', 'minecraft:yellow_dye'] },
  bread_bait: { count: 8, ingredients: ['minecraft:bread'] },
  boilie_bait: { count: 6, ingredients: ['minecraft:egg', 'minecraft:wheat', 'minecraft:sugar', 'minecraft:bone_meal'] },
  baitfish_bait: { count: 2, ingredients: ['minecraft:cod'] },
  fish_strip_bait: { count: 4, ingredients: ['minecraft:salmon'] },
  larva_bait: { count: 6, ingredients: ['minecraft:spider_eye', 'minecraft:wheat_seeds'] },
  shrimp_bait: { count: 4, ingredients: ['minecraft:kelp', 'minecraft:cod', 'minecraft:bone_meal'] },
  glow_bait: { count: 4, ingredients: ['atzen:worm_bait', 'minecraft:glow_ink_sac'] },
  hearty_bait: { count: 4, ingredients: ['minecraft:salmon', 'minecraft:kelp', 'minecraft:wheat'] },
  spinner: ['minecraft:iron_nugget', 'minecraft:iron_nugget', 'minecraft:copper_ingot', 'minecraft:string'],
  spoon: ['minecraft:iron_ingot', 'minecraft:gold_nugget'],
  wobbler: ['minecraft:oak_planks', 'minecraft:red_dye', 'minecraft:iron_nugget', 'minecraft:iron_nugget'],
  soft_lure: ['minecraft:slime_ball', 'minecraft:lime_dye', 'minecraft:iron_nugget'],
  popper: ['minecraft:oak_planks', 'minecraft:white_dye', 'minecraft:iron_nugget', 'minecraft:feather'],
  jig: ['minecraft:iron_ingot', 'minecraft:string', 'minecraft:slime_ball'],
  streamer: ['minecraft:feather', 'minecraft:string', 'minecraft:iron_nugget', 'minecraft:green_dye'],
  dry_fly: ['minecraft:feather', 'minecraft:string', 'minecraft:iron_nugget'],
  nymph: ['minecraft:feather', 'minecraft:string', 'minecraft:iron_nugget', 'minecraft:gold_nugget'],
  ancient_bait: { count: 2, ingredients: ['minecraft:beef', 'minecraft:spider_eye', 'minecraft:mud', 'minecraft:glow_ink_sac'] },
  reed_king_lure: ['atzen:wobbler', 'minecraft:emerald', 'minecraft:gold_ingot', 'minecraft:feather'],
  current_bait: { count: 2, ingredients: ['atzen:boilie_bait', 'minecraft:prismarine_crystals', 'minecraft:clay_ball', 'minecraft:bone_meal'] },
  deep_light_bait: { count: 2, ingredients: ['atzen:glow_bait', 'minecraft:echo_shard', 'minecraft:sculk'] },
  steady_line_tome: ['minecraft:book', 'minecraft:lapis_lazuli', 'minecraft:string', 'minecraft:iron_ingot'],
  reel_power_tome: ['minecraft:book', 'minecraft:lapis_lazuli', 'minecraft:redstone', 'minecraft:amethyst_shard'],
  calm_waters_tome: ['minecraft:book', 'minecraft:lapis_lazuli', 'minecraft:prismarine_crystals', 'minecraft:glow_ink_sac'],
};
for (const [id, recipe] of Object.entries(recipes)) {
  const ingredients = Array.isArray(recipe) ? recipe : recipe.ingredients;
  const count = Array.isArray(recipe) ? 1 : recipe.count;
  json(`data/atzen/recipe/${id}.json`, {
    type: 'minecraft:crafting_shapeless', category: /_(rod|reel|line)$/.test(id) ? 'equipment' : 'misc',
    ingredients, result: { id: `atzen:${id}`, count },
  });
  json(`data/atzen/advancement/recipes/${id}.json`, {
    parent: 'minecraft:recipes/root',
    criteria: {
      has_ingredient: { trigger: 'minecraft:inventory_changed', conditions: { items: [{ items: ingredients[0] }] } },
      has_the_recipe: { trigger: 'minecraft:recipe_unlocked', conditions: { recipe: `atzen:${id}` } },
    },
    requirements: [['has_ingredient', 'has_the_recipe']], rewards: { recipes: [`atzen:${id}`] },
  });
}

json('data/atzen/recipe/carbon_fiber.json', {
  type: 'minecraft:blasting', category: 'misc', ingredient: 'minecraft:coal_block',
  result: { id: 'atzen:carbon_fiber' }, experience: 0.5, cookingtime: 200,
});

const translations = {
  'tooltip.atzen.gear.reel': ['Rolle: %s', 'Reel: %s'],
  'tooltip.atzen.gear.line': ['Schnur: %s', 'Line: %s'],
  'tooltip.atzen.gear.bait': ['Köder: %s', 'Bait: %s'],
  'tooltip.atzen.gear.empty': ['– leer –', '– empty –'],
  'tooltip.atzen.gear.count': ['%s ×%s', '%s ×%s'],
  'tooltip.atzen.gear.stats': ['Tragkraft %s kg · %s m · Einzug %s m/s · Kraft %s kg', 'Breaking load %s kg · %s m · Retrieve %s m/s · Force %s kg'],
  'tooltip.atzen.gear.ability': ['Rolle: %s', 'Reel: %s'],
  'hud.atzen.advice.land': ['Rechtsklick – Fisch landen!', 'Right-click – land the fish!'],
  'hud.atzen.advice.release': ['Gleich reißt die Schnur! Loslassen, Bremse lockern (%4$s)', 'The line is about to break! Let go, ease the drag (%4$s)'],
  'hud.atzen.advice.tangled': ['Perücke! Schnur verheddert – kurz nicht kurbeln', 'Backlash! Line tangled – no reeling for a moment'],
  'hud.atzen.advice.warn_run': ['Flucht kommt – nicht kurbeln', 'A run is coming – stop reeling'],
  'hud.atzen.advice.warn_dive': ['Er will abtauchen – gleich Schnur geben', 'It wants to dive – give line'],
  'hud.atzen.advice.warn_side': ['Er zieht gleich seitwärts zu Hindernissen', 'It is about to head sideways for cover'],
  'hud.atzen.advice.warn_shake': ['Kopfstöße kommen – Spannung mittel halten', 'Head shakes coming – keep medium tension'],
  'hud.atzen.advice.warn_jump': ['Sprung kommt – Rute runter (%2$s loslassen)', 'A jump is coming – rod down (release %2$s)'],
  'hud.atzen.advice.slack': ['Schnur hängt durch – %1$s!', 'The line is slack – %1$s!'],
  'hud.atzen.advice.jump': ['Er springt! Rute unten, leicht kurbeln (%1$s)', 'It jumps! Rod down, reel gently (%1$s)'],
  'hud.atzen.advice.steer_left': ['Gegensteuern: %5$s halten!', 'Steer against it: hold %5$s!'],
  'hud.atzen.advice.steer_right': ['Gegensteuern: %5$s halten!', 'Steer against it: hold %5$s!'],
  'hud.atzen.advice.spool': ['Spule fast leer! Bremse fester (%4$s)', 'Spool almost empty! Tighten the drag (%4$s)'],
  'hud.atzen.advice.taking': ['Er nimmt Schnur – nicht kurbeln, ermüden lassen', 'It is taking line – stop reeling, let it tire'],
  'hud.atzen.advice.twist': ['Du kurbelst gegen die Bremse – die Schnur verdrallt!', 'You are reeling against the drag – the line twists!'],
  'hud.atzen.advice.shake': ['Kopfstöße – Spannung mittel halten', 'Head shakes – keep medium tension'],
  'hud.atzen.advice.pump': ['Pumpen: %2$s heben, beim Absenken %1$s', 'Pump: lift with %2$s, reel with %1$s as it drops'],
  'hud.atzen.advice.reel': ['%1$s halten – einholen', 'Hold %1$s – reel in'],
  'hud.atzen.info': ['Schnur %s/%s m · Bremse %s/%s · Haken %s %%', 'Line %s/%s m · Drag %s/%s · Hook %s%%'],
  'hud.atzen.strike.perfect': ['Anschlag: perfekt (%s %%)', 'Strike: perfect (%s%%)'],
  'hud.atzen.strike.good': ['Anschlag: gut (%s %%)', 'Strike: good (%s%%)'],
  'hud.atzen.strike.late': ['Anschlag: ungünstig (%s %%) – Haken sitzt locker', 'Strike: poor (%s%%) – the hook sits loose'],
  'hud.atzen.trophy_quality': ['Kontrolle im Drill: %s %%', 'Control during the fight: %s%%'],
  'key.category.atzen.fishing': ['Atzen: Angeln', 'Atzen: Fishing'],
  'key.atzen.reel': ['Schnur einholen (halten)', 'Reel in (hold)'],
  'key.atzen.lift': ['Rute heben / pumpen (halten)', 'Lift the rod / pump (hold)'],
  'key.atzen.steer_left': ['Gegensteuern links', 'Steer left'],
  'key.atzen.steer_right': ['Gegensteuern rechts', 'Steer right'],
  'key.atzen.drag_up': ['Bremse fester', 'Tighten drag'],
  'key.atzen.drag_down': ['Bremse lockern', 'Loosen drag'],
  'key.atzen.cancel': ['Drill abbrechen / Szene überspringen', 'Cancel the fight / skip a shot'],
  'key.atzen.camera': ['Kinokamera an/aus', 'Cinematic camera on/off'],
  'fish.atzen.darter': ['Flitzer', 'Darter'],
  'fish.atzen.fighter': ['Kämpfer', 'Fighter'],
  'fish.atzen.bruiser': ['Brocken', 'Bruiser'],
  'result.atzen.caught': ['FANG GELANDET!', 'CATCH LANDED!'],
  'result.atzen.snapped': ['SCHNUR GERISSEN!', 'LINE SNAPPED!'],
  'result.atzen.escaped': ['FISCH ENTKOMMEN!', 'FISH ESCAPED!'],
  'result.atzen.timeout': ['DER FISCH WAR ZU AUSDAUERND.', 'THE FISH OUTLASTED YOU.'],
  'result.atzen.cancelled': ['DUELL ABGEBROCHEN.', 'BATTLE CANCELLED.'],
  'result.atzen.spooled': ['SPULE LEER!', 'SPOOLED!'],
  'action.atzen.calm': ['Hält dagegen', 'Holding'],
  'action.atzen.run': ['Flucht', 'Run'],
  'action.atzen.dive': ['Abtauchen', 'Dive'],
  'action.atzen.side': ['Seitwärts zu Hindernissen', 'Sideways to cover'],
  'action.atzen.shake': ['Kopfstöße', 'Head shakes'],
  'action.atzen.jump': ['Sprung', 'Jump'],
  'action.atzen.tired': ['Erschöpft', 'Worn out'],
  'ability.atzen.none': ['keine Besonderheit', 'nothing special'],
  'ability.atzen.forgiving': ['Verzeihend', 'Forgiving'],
  'ability.atzen.fine_drag': ['Feinbremse', 'Fine drag'],
  'ability.atzen.baitcaster': ['Baitcaster (Perückengefahr)', 'Baitcaster (backlash risk)'],
  'ability.atzen.power': ['Kraftübersetzung', 'Power gear'],
  'ability.atzen.fly': ['Fliegenrolle', 'Fly reel'],
  'ability.atzen.echo': ['Echo – frühe Warnung', 'Echo – early warning'],
  'ability.atzen.tide': ['Gezeiten – Meeresfische erholen sich nicht', 'Tide – sea fish never recover'],
  'ability.atzen.auto_drag': ['Selbstbegrenzende Bremse', 'Self-limiting drag'],
  'ability.atzen.anchor': ['Anker – kürzere Fluchten', 'Anchor – shorter runs'],
  'ability.atzen.jump_master': ['Sprungmeister', 'Jump master'],
  'ability.atzen.current': ['Strömung – holt in Ruhephasen selbst ein', 'Current – reels in on its own while the fish rests'],
  'ability.atzen.adapt': ['Tiefenherz – Fisch ermüdet schneller', 'Deep heart – the fish tires faster'],
  'boss.atzen.ancient_catfish': ['Der Alte vom Grund', 'The Old One of the Deep'],
  'boss.atzen.ancient_catfish.phase.awakening': ['Erwachen', 'Awakening'],
  'boss.atzen.ancient_catfish.phase.deadwood': ['Ins Totholz', 'Into the deadwood'],
  'boss.atzen.ancient_catfish.phase.deception': ['Täuschung', 'Deception'],
  'boss.atzen.reed_king': ['Der Schilfkönig', 'The Reed King'],
  'boss.atzen.reed_king.phase.hunt': ['Jagd im Schilf', 'Hunt in the reeds'],
  'boss.atzen.reed_king.phase.dance': ['Der Tanz', 'The dance'],
  'boss.atzen.reed_king.phase.last_stand': ['Letzter Widerstand', 'Last stand'],
  'boss.atzen.black_sturgeon': ['Der Schwarze Stör', 'The Black Sturgeon'],
  'boss.atzen.black_sturgeon.phase.current': ['Gegen die Strömung', 'Against the current'],
  'boss.atzen.black_sturgeon.phase.bottom': ['Am Grund', 'On the bottom'],
  'boss.atzen.black_sturgeon.phase.eruption': ['Ausbruch', 'Eruption'],
  'boss.atzen.deep_warden': ['Der Tiefenwächter', 'The Deep Warden'],
  'boss.atzen.deep_warden.phase.hunter': ['Jäger', 'Hunter'],
  'boss.atzen.deep_warden.phase.shadow': ['Schatten', 'Shadow'],
  'boss.atzen.deep_warden.phase.frenzy': ['Raserei', 'Frenzy'],
  'message.atzen.boss_hooked': ['%s hängt am Haken!', '%s is on the hook!'],
  'message.atzen.boss_caught': ['Du hast %s gelandet!', 'You landed %s!'],
  'message.atzen.landing_failed': ['Er schlägt aus und flüchtet noch einmal!', 'It kicks free and runs once more!'],
  'message.atzen.camera_on': ['Kinokamera an', 'Cinematic camera on'],
  'message.atzen.camera_off': ['Kinokamera aus', 'Cinematic camera off'],
  'enchantment.atzen.steady_line': ['Schnurmeister', 'Steady Line'],
  'enchantment.atzen.reel_power': ['Kurbelkraft', 'Reel Power'],
  'enchantment.atzen.calm_waters': ['Wellenruhe', 'Calm Waters'],
  'message.atzen.tome_instruction': ['Halte eine eingeholte Angel in der anderen Hand und benutze das Buch.', 'Hold a reeled-in rod in the other hand, then use the tome.'],
  'message.atzen.tome_max': ['Diese Verzauberung hat bereits ihre höchste Stufe.', 'This enchantment is already at its maximum level.'],
  'message.atzen.enchanted': ['Angel verzaubert: %s %s', 'Rod enchanted: %s %s'],
  'message.atzen.client_required': ['Installiere Atzen auch auf deinem Client, um Angelduelle zu spielen.', 'Install Atzen on your client to play fishing battles.'],
  'message.atzen.already_fishing': ['Beende zuerst deinen aktuellen Wurf oder dein Duell.', 'Finish your current cast or battle first.'],
  'message.atzen.help': ['Atzen: auswerfen, beim Biss rechtsklicken (der richtige Moment setzt den Haken fester). Im Drill: R einholen, G Rute heben (pumpen), Pfeil hoch/runter Bremse, Pfeil links/rechts gegensteuern, ist der Fisch nah und müde: Rechtsklick zum Landen. Rolle, Schnur und Köder im Inventar auf die Rute ziehen. /atzen practice [Fisch|Boss] startet ein Training ohne Beute, /atzen cancel bricht ab, C schaltet die Kinokamera. Tasten sind einstellbar.', 'Atzen: cast, right-click at a bite (good timing sets the hook firmer). In the fight: R reels, G lifts the rod (pumping), arrow up/down sets the drag, arrow left/right steers, and when the fish is close and worn out, right-click to land it. Drop reel, line and bait onto the rod in your inventory. /atzen practice [fish|boss] starts a no-loot training, /atzen cancel stops, C toggles the cinematic camera. Keys are configurable.'],
  'subtitles.atzen.bite': ['Fisch beißt an', 'Fish bites'],
  'subtitles.atzen.hook_set': ['Angel schlägt an', 'Rod strikes'],
  'subtitles.atzen.warning': ['Fisch sammelt Kraft', 'Fish gathers strength'],
  'subtitles.atzen.reel': ['Rolle ratscht', 'Reel clicks'],
  'subtitles.atzen.creak': ['Schnur knarzt', 'Line creaks'],
  'subtitles.atzen.slack': ['Schnur hängt durch', 'Line goes slack'],
  'subtitles.atzen.leap': ['Fisch springt', 'Fish leaps'],
  'subtitles.atzen.snap': ['Schnur reißt', 'Line snaps'],
  'subtitles.atzen.catch': ['Fang gelandet', 'Catch landed'],
  'subtitles.atzen.escape': ['Fisch entkommt', 'Fish escapes'],
  'subtitles.atzen.bait': ['Köder eingelegt', 'Bait loaded'],
  'message.atzen.controls_hint': ['Erster Drill! %1$s einholen · %2$s Rute heben (pumpen) · %3$s/%4$s gegensteuern · %5$s/%6$s Bremse · nah und müde: Rechtsklick landen · %7$s abbrechen · %8$s Kinokamera. Die Leiste über der Hotbar sagt dir, was gerade dran ist.', 'First fight! %1$s reel · %2$s lift (pump) · %3$s/%4$s steer · %5$s/%6$s drag · close and worn out: right-click to land · %7$s cancel · %8$s cinematic camera. The bar above the hotbar tells you what to do.'],
};

const de = { 'itemGroup.atzen': 'Atzen – Angeln' };
const en = { 'itemGroup.atzen': 'Atzen – Fishing' };
for (const [id, [german, english, germanTip, englishTip]] of Object.entries(items)) {
  de[`item.atzen.${id}`] = german; en[`item.atzen.${id}`] = english;
  de[`tooltip.atzen.${id}`] = germanTip; en[`tooltip.atzen.${id}`] = englishTip;
}
for (const [key, [german, english]] of Object.entries(translations)) { de[key] = german; en[key] = english; }
json('assets/atzen/lang/de_de.json', de);
json('assets/atzen/lang/en_us.json', en);
console.log(`Generated models, recipes, advancements, enchantments and translations for ${Object.keys(items).length} items.`);

const fish = {
  river_trout: ['Bachforelle', 'Brown Trout', 0xB9A2A0],
  rainbow_trout: ['Regenbogenforelle', 'Rainbow Trout', 0xC88AA8],
  grayling: ['Äsche', 'Grayling', 0x8C93A8],
  salmon: ['Atlantischer Lachs', 'Atlantic Salmon', 0xC9CFD6],
  sea_trout: ['Meerforelle', 'Sea Trout', 0xB8C2C8],
  pike: ['Hecht', 'Pike', 0x566E3A],
  zander: ['Zander', 'Zander', 0x8A9478],
  perch: ['Flussbarsch', 'Perch', 0x86A848],
  catfish: ['Wels', 'Wels Catfish', 0x73638A],
  eel: ['Aal', 'Eel', 0x4A4A30],
  asp: ['Rapfen', 'Asp', 0xA8B4BC],
  carp: ['Karpfen', 'Carp', 0xBE8540],
  barbel: ['Barbe', 'Barbel', 0xA89060],
  roach: ['Rotauge', 'Roach', 0xB8C0C4],
  rudd: ['Rotfeder', 'Rudd', 0xC8A848],
  bream: ['Brasse', 'Bream', 0x9C8A5A],
  tench: ['Schleie', 'Tench', 0x5A7038],
  crucian_carp: ['Karausche', 'Crucian Carp', 0xC8A040],
  sturgeon: ['Stör', 'Sturgeon', 0x6A7480],
  chub: ['Döbel', 'Chub', 0x9AA0A0],
  atlantic_cod: ['Kabeljau', 'Atlantic Cod', 0x9C8A5E],
  saithe: ['Seelachs', 'Saithe', 0x4E5A60],
  alaska_pollock: ['Alaska-Seelachs', 'Alaska Pollock', 0x8A8A70],
  haddock: ['Schellfisch', 'Haddock', 0x7A7E84],
  hake: ['Seehecht', 'Hake', 0x9AA4AE],
  whiting: ['Wittling', 'Whiting', 0xB4B0A0],
  herring: ['Hering', 'Herring', 0x5E86A8],
  sardine: ['Sardine', 'Sardine', 0x4E7EA0],
  anchovy: ['Sardelle', 'Anchovy', 0x5A8098],
  sprat: ['Sprotte', 'Sprat', 0x6A90A8],
  sea_bass: ['Wolfsbarsch', 'Sea Bass', 0x9AA6B0],
  gilthead_bream: ['Dorade', 'Gilthead Bream', 0xB0B8C0],
  red_snapper: ['Roter Schnapper', 'Red Snapper', 0xD0504A],
  red_mullet: ['Rotbarbe', 'Red Mullet', 0xE07858],
  grouper: ['Zackenbarsch', 'Grouper', 0x7A5A3A],
  barracuda: ['Barrakuda', 'Barracuda', 0x8A98A4],
  mackerel: ['Makrele', 'Mackerel', 0x3E7A8A],
  tuna: ['Blauflossen-Thun', 'Bluefin Tuna', 0x2A3E6A],
  skipjack: ['Echter Bonito', 'Skipjack Tuna', 0x3A4E7A],
  swordfish: ['Schwertfisch', 'Swordfish', 0x4A5A7A],
  plaice: ['Scholle', 'Plaice', 0x8A7A5A],
  flounder: ['Flunder', 'Flounder', 0x7A6E50],
  sole: ['Seezunge', 'Sole', 0x8A7650],
  turbot: ['Steinbutt', 'Turbot', 0x6A6250],
  halibut: ['Heilbutt', 'Halibut', 0x5E5A50],
  redfish: ['Rotbarsch', 'Redfish', 0xD05A3A],
  monkfish: ['Seeteufel', 'Monkfish', 0x6A5A44],
  gurnard: ['Knurrhahn', 'Gurnard', 0xC8604A],
  wolffish: ['Seewolf', 'Atlantic Wolffish', 0x6A7078],
  golden_koi: ['Goldkoi', 'Golden Koi', 0xFFD95B],
  ancient_catfish: ['Der Alte vom Grund', 'The Old One of the Deep', 0x3A3428],
  reed_king: ['Der Schilfkönig', 'The Reed King', 0x3E5A20],
  black_sturgeon: ['Der Schwarze Stör', 'The Black Sturgeon', 0x1A1C22],
  deep_warden: ['Der Tiefenwächter', 'The Deep Warden', 0x0E3A44],
};
for (const [id, [german, english, color]] of Object.entries(fish)) {
  de[`item.atzen.${id}`] = german; en[`item.atzen.${id}`] = english;
  de[`entity.atzen.${id}`] = german; en[`entity.atzen.${id}`] = english;
  de[`item.atzen.${id}_bucket`] = `${german} im Eimer`; en[`item.atzen.${id}_bucket`] = `Bucket of ${english}`;
  de[`item.atzen.${id}_spawn_egg`] = `${german}-Spawn-Ei`; en[`item.atzen.${id}_spawn_egg`] = `${english} Spawn Egg`;
  json(`assets/atzen/models/item/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `atzen:item/${id}` } });
  json(`assets/atzen/models/item/${id}_bucket.json`, {
    parent: 'minecraft:item/generated', textures: { layer0: `atzen:item/${id}_bucket` },
  });
  for (const suffix of ['', '_bucket']) {
    json(`assets/atzen/items/${id}${suffix}.json`, { model: { type: 'minecraft:model', model: `atzen:item/${id}${suffix}` } });
  }
  json(`assets/atzen/items/${id}_spawn_egg.json`, {
    model: { type: 'minecraft:model', model: 'minecraft:item/cod_spawn_egg', tints: [{ type: 'minecraft:constant', value: color }] },
  });
  for (const type of ['smelting', 'smoking', 'campfire_cooking']) {
    json(`data/atzen/recipe/${id}_${type}.json`, {
      type: `minecraft:${type}`, category: 'food', ingredient: `atzen:${id}`,
      result: { id: 'minecraft:cooked_cod' }, experience: 0.35,
      cookingtime: type === 'smoking' ? 100 : type === 'campfire_cooking' ? 600 : 200,
    });
  }
}
json('data/minecraft/tags/item/fishes.json', { replace: false, values: Object.keys(fish).map(id => `atzen:${id}`) });
const fishTranslations = {
  'rarity.atzen.common': ['Gewöhnlich', 'Common'],
  'rarity.atzen.uncommon': ['Ungewöhnlich', 'Uncommon'],
  'rarity.atzen.rare': ['Selten', 'Rare'],
  'rarity.atzen.epic': ['Episch', 'Epic'],
  'rarity.atzen.legendary': ['Legendär', 'Legendary'],
  'rarity.atzen.mythic': ['Mythisch', 'Mythic'],
  'size.atzen.small': ['Kleines Exemplar', 'Small specimen'],
  'size.atzen.normal': ['Ausgewachsen', 'Mature'],
  'size.atzen.large': ['Großes Exemplar', 'Large specimen'],
  'size.atzen.trophy': ['Trophäenfisch', 'Trophy fish'],
  'catch.atzen.measurements': ['Länge: %s cm | Gewicht: %s kg', 'Length: %s cm | Weight: %s kg'],
  'catch.atzen.location': ['Fangort: %s (%s, %s)', 'Caught in: %s (%s, %s)'],
  'catch.atzen.day': ['Welt-Tag: %s', 'World day: %s'],
  'catch.atzen.quality': ['Kontrolle: %s %% | Angler: %s', 'Control: %s%% | Angler: %s'],
  'catch.atzen.wild': ['Wildfang', 'Wild catch'],
  'habitat.atzen.river': ['Fluss', 'River'],
  'habitat.atzen.lake': ['See', 'Lake'],
  'habitat.atzen.swamp': ['Sumpf', 'Swamp'],
  'habitat.atzen.cold': ['Kaltes Gewässer', 'Cold waters'],
  'habitat.atzen.ocean': ['Meer', 'Sea'],
  'habitat.atzen.warm_ocean': ['Warmes Meer', 'Warm sea'],
  'habitat.atzen.cold_ocean': ['Nordmeer', 'Northern sea'],
  'habitat.atzen.deep_ocean': ['Tiefsee', 'Deep sea'],
};
for (const [key, [german, english]] of Object.entries(fishTranslations)) { de[key] = german; en[key] = english; }
json('assets/atzen/lang/de_de.json', de);
json('assets/atzen/lang/en_us.json', en);
console.log(`Generated ${Object.keys(fish).length} fish species with buckets, spawn eggs, cooking recipes and catch translations.`);
