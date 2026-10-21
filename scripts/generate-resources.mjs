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
  carbon_rod: ['Carbonrute', 'Carbon Rod', 'Die Allround-Rute: hält 24 kg, wirft weit und ist stark genug für die Bossfische.', 'The all-rounder: bears 24 kg, casts far and is strong enough for the boss fish.'],
  bamboo_rod: ['Bambusrute', 'Bamboo Rod', 'Für den Anfang: leicht und feinfühlig, hält 7 kg – reicht für Fische bis ein paar Kilo.', 'To start with: light and sensitive, bears 7 kg – enough for fish of a few kilos.'],
  netherite_rod: ['Netheritrute', 'Netherite Rod', 'Die stärkste Rute (30 kg) und die einzige, die in Lava angelt – im Nether und in der Oberwelt.', 'The strongest rod (30 kg) and the only one that fishes in lava – in the Nether and the overworld.'],
  copper_reel: ['Kupferrolle', 'Copper Reel', 'Für den Anfang: 120 m Schnur, holt gegen 5 kg ein.', 'To start with: 120 m of line, winches against 5 kg.'],
  iron_reel: ['Eisenrolle', 'Iron Reel', '180 m Schnur, holt gegen 8 kg ein, etwas schneller.', '180 m of line, winches against 8 kg, a little faster.'],
  diamond_reel: ['Diamantrolle', 'Diamond Reel', 'Die beste Rolle: 280 m Schnur, holt gegen 12 kg ein, am schnellsten.', 'The best reel: 280 m of line, winches against 12 kg, the fastest.'],
  carbon_fiber: ['Kohlefaser', 'Carbon Fiber', 'Aus einem Kohleblock im Hochofen. Für die Carbonrute.', 'From a coal block in a blast furnace. For the carbon rod.'],
  fishing_line: ['Angelschnur', 'Fishing Line', 'Trägt 16 kg. Ohne Schnur angelt die Rute nur mit Faden (3,5 kg).', 'Bears 16 kg. Without it the rod fishes with plain string (3.5 kg).'],
  worm_bait: ['Regenwurm', 'Earthworm', 'Für alle friedlichen Fische: Karpfen, Forellen, Grundfische, Schwärme.', 'For all peaceful fish: carp, trout, bottom fish, shoals.'],
  baitfish_bait: ['Köderfisch', 'Baitfish', 'Für Raubfische in Süß- und Salzwasser, eher große.', 'For predators in fresh and salt water, rather big ones.'],
  glow_bait: ['Leuchtköder', 'Glow Bait', 'Lockt alle Fische, nachts besonders. Fluchten ziehen 20 % schwächer.', 'Draws every fish, at night most of all. Runs pull 20% weaker.'],
  ancient_bait: ['Uralter Grundköder', 'Ancient Bottom Bait', 'Bossköder: nachts in einem See oder Sumpf, mindestens 5 Blöcke tief, ruft er den Alten vom Grund.', 'Boss bait: at night in a lake or swamp at least 5 blocks deep, it calls the Old One of the Deep.'],
  reed_king_lure: ['Königswobbler', 'King\'s Crankbait', 'Bossköder: im Sumpf bei Regen oder in der Dämmerung lockt er den Schilfkönig.', 'Boss lure: in a swamp in the rain or at dusk and dawn, it draws the Reed King.'],
  current_bait: ['Strömungsköder', 'Current Bait', 'Bossköder: tagsüber in einem Fluss, mindestens 4 Blöcke tief, weckt er den Schwarzen Stör.', 'Boss bait: by day in a river at least 4 blocks deep, it wakes the Black Sturgeon.'],
  deep_light_bait: ['Tiefenlicht', 'Deep Light', 'Bossköder: in verborgenem Wasser unter y 0, ohne Himmel darüber, erscheint der Tiefenwächter.', 'Boss bait: in hidden water below y 0, out of sight of the sky, the Deep Warden appears.'],
  ancient_barbel: ['Uralte Welsbarte', 'Ancient Barbel', 'Trophäe des Alten vom Grund.', 'Trophy of the Old One of the Deep.'],
  reed_crown: ['Schilfkrone', 'Reed Crown', 'Trophäe des Schilfkönigs.', 'Trophy of the Reed King.'],
  sturgeon_plate: ['Schwarze Knochenplatte', 'Black Bone Plate', 'Trophäe des Schwarzen Störs.', 'Trophy of the Black Sturgeon.'],
  warden_heart: ['Herz der Tiefe', 'Heart of the Deep', 'Trophäe des Tiefenwächters.', 'Trophy of the Deep Warden.'],
};

for (const id of Object.keys(items)) {
  json(`assets/atzen/models/item/${id}.json`, {
    parent: id.endsWith('_rod') ? 'minecraft:item/handheld_rod' : 'minecraft:item/generated',
    textures: { layer0: `atzen:item/${id}` },
  });
  json(`assets/atzen/items/${id}.json`, { model: { type: 'minecraft:model', model: `atzen:item/${id}` } });
}

const rods = ['bamboo_rod', 'carbon_rod', 'netherite_rod'].map(id => `atzen:${id}`);
json('data/atzen/tags/item/rods.json', { values: ['minecraft:fishing_rod', ...rods] });
for (const tag of ['fishing', 'durability', 'vanishing']) {
  json(`data/minecraft/tags/item/enchantable/${tag}.json`, { replace: false, values: rods });
}

const enchantments = {};
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
  bamboo_rod: ['minecraft:bamboo', 'minecraft:bamboo', 'minecraft:bamboo', 'minecraft:string', 'minecraft:string'],
  carbon_rod: ['atzen:bamboo_rod', 'atzen:carbon_fiber', 'atzen:carbon_fiber', 'atzen:carbon_fiber', 'minecraft:diamond'],
  fishing_line: ['minecraft:string', 'minecraft:string', 'minecraft:string', 'minecraft:slime_ball'],
  netherite_rod: ['atzen:carbon_rod', 'minecraft:netherite_ingot', 'minecraft:blaze_rod'],
  copper_reel: ['minecraft:copper_ingot', 'minecraft:copper_ingot', 'minecraft:iron_nugget', 'minecraft:stick'],
  iron_reel: ['atzen:copper_reel', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:redstone'],
  diamond_reel: ['atzen:iron_reel', 'minecraft:diamond', 'minecraft:gold_ingot', 'minecraft:redstone'],
  worm_bait: { count: 6, ingredients: ['minecraft:rotten_flesh', 'minecraft:wheat_seeds'] },
  baitfish_bait: { count: 2, ingredients: ['minecraft:cod'] },
  glow_bait: { count: 4, ingredients: ['atzen:worm_bait', 'minecraft:glow_ink_sac'] },
  ancient_bait: { count: 2, ingredients: ['minecraft:beef', 'minecraft:spider_eye', 'minecraft:mud', 'minecraft:glow_ink_sac'] },
  reed_king_lure: ['atzen:baitfish_bait', 'minecraft:emerald', 'minecraft:gold_ingot', 'minecraft:feather'],
  current_bait: { count: 2, ingredients: ['atzen:worm_bait', 'minecraft:prismarine_crystals', 'minecraft:clay_ball', 'minecraft:bone_meal'] },
  deep_light_bait: { count: 2, ingredients: ['atzen:glow_bait', 'minecraft:echo_shard', 'minecraft:sculk'] },
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
  'tooltip.atzen.gear.reel': ['Rolle', 'Reel'],
  'tooltip.atzen.gear.line': ['Schnur', 'Line'],
  'tooltip.atzen.gear.bait': ['Köder', 'Bait'],
  'tooltip.atzen.gear.holds': ['Hält %s kg', 'Holds %s kg'],
  'tooltip.atzen.gear.speed': ['Einzug %s m/s · Kraft %s kg · %s m', 'Retrieve %s m/s · force %s kg · %s m'],
  'tooltip.atzen.gear.upgrades': ['Verstärkt %s', 'Reinforced %s'],
  'hud.atzen.prompt.reel': ['Einholen', 'Reel in'],
  'hud.atzen.prompt.tighten': ['Schnur straff halten', 'Keep the line tight'],
  'hud.atzen.prompt.give': ['Schnur geben', 'Give line'],
  'hud.atzen.prompt.warn': ['Achtung – nicht kurbeln!', 'Watch out – stop reeling!'],
  'hud.atzen.prompt.wait': ['Nicht kurbeln – er zieht', 'Stop reeling – it pulls'],
  'hud.atzen.prompt.give_now': ['Schnur geben – sonst reißt sie!', 'Give line – or it snaps!'],
  'hud.atzen.prompt.land': ['Rechtsklick – Fisch landen!', 'Right-click – land the fish!'],
  'hud.atzen.prompt.net': ['Rechtsklick im grünen Feld – keschern!', 'Right-click in the green field – net it!'],
  'hud.atzen.distance': ['%s m', '%s m'],
  'hud.atzen.hook_loose': ['Haken sitzt locker!', 'Hook is loose!'],
  'hud.atzen.strength': ['Kraft', 'Strength'],
  'hud.atzen.strike.perfect': ['Anschlag: perfekt', 'Strike: perfect'],
  'hud.atzen.strike.good': ['Anschlag: gut', 'Strike: good'],
  'hud.atzen.strike.late': ['Anschlag: zu spät – Haken sitzt locker', 'Strike: late – the hook sits loose'],
  'key.atzen.give': ['Schnur geben (halten)', 'Give line (hold)'],
  'message.atzen.controls_hint': ['Erster Drill! %1$s halten = einholen, %2$s halten = Schnur geben. Hält der Fisch still: %1$s. Zieht er: loslassen. Springt er oder schüttelt den Kopf: %2$s. Nah und müde: Rechtsklick. Die Anzeige über der Hotbar sagt dir, was dran ist.', 'First fight! Hold %1$s to reel in, hold %2$s to give line. Fish holds still: %1$s. It pulls: let go. It jumps or shakes its head: %2$s. Close and worn out: right-click. The panel above the hotbar tells you what to do.'],
  'message.atzen.help': ['Atzen: auswerfen, beim Biss rechtsklicken. Im Drill: R einholen, F Schnur geben, ist der Fisch nah und müde: Rechtsklick zum Landen. Rolle, Schnur und Köder im Inventar auf die Rute ziehen. /atzen practice [Fisch|Boss] startet ein Training, /atzen cancel bricht ab.', 'Atzen: cast, right-click at a bite. In the fight: R reels in, F gives line, and when the fish is close and worn out, right-click to land it. Drop reel, line and bait onto the rod in your inventory. /atzen practice [fish|boss] starts a training, /atzen cancel stops.'],
  'lexicon.atzen.species_detail': ['%s× · bis %s kg', '%s× · up to %s kg'],
  'lexicon.atzen.area_level': ['ab Stufe %s', 'from level %s'],
  'hud.atzen.trophy_quality': ['Kontrolle im Drill: %s %%', 'Control during the fight: %s%%'],
  'key.category.atzen.fishing': ['Atzen: Angeln', 'Atzen: Fishing'],
  'key.atzen.reel': ['Schnur einholen (halten)', 'Reel in (hold)'],
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
  'message.atzen.client_required': ['Installiere Atzen auch auf deinem Client, um Angelduelle zu spielen.', 'Install Atzen on your client to play fishing battles.'],
  'message.atzen.already_fishing': ['Beende zuerst deinen aktuellen Wurf oder dein Duell.', 'Finish your current cast or battle first.'],
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
  cave_tetra: ['Höhlensalmler', 'Cave Tetra', 0xE8D0D0],
  olm: ['Grottenolm', 'Olm', 0xF0D8D0],
  amethyst_perch: ['Amethystbarsch', 'Amethyst Perch', 0x9A6AD0],
  dripstone_eel: ['Tropfsteinaal', 'Dripstone Eel', 0x8A6A50],
  glow_carp: ['Leuchtkarpfen', 'Glow Carp', 0x5AE0C0],
  ember_carp: ['Glutkarpfen', 'Ember Carp', 0xE8682A],
  magma_eel: ['Magmaaal', 'Magma Eel', 0x8A2A1A],
  blaze_pike: ['Lohenhecht', 'Blaze Pike', 0xF0A030],
  basalt_catfish: ['Basaltwels', 'Basalt Catfish', 0x4A4A50],
  obsidian_sturgeon: ['Obsidianstör', 'Obsidian Sturgeon', 0x2A1A40],
  chorus_fish: ['Chorusfisch', 'Chorus Fish', 0xA878A8],
  ender_eel: ['Enderaal', 'Ender Eel', 0x1A3A34],
  void_ray: ['Leererochen', 'Void Ray', 0x2A2040],
  shulker_puffer: ['Shulker-Kugelfisch', 'Shulker Puffer', 0xA87AA8],
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
  'habitat.atzen.cave': ['Höhlenwasser', 'Cave water'],
  'habitat.atzen.lava': ['Lava', 'Lava'],
  'habitat.atzen.end': ['Wasser im End', 'End water'],
};
for (const [key, [german, english]] of Object.entries(fishTranslations)) { de[key] = german; en[key] = english; }
json('assets/atzen/lang/de_de.json', de);
json('assets/atzen/lang/en_us.json', en);
console.log(`Generated ${Object.keys(fish).length} fish species with buckets, spawn eggs, cooking recipes and catch translations.`);

// ---------------------------------------------------------------- 0.8–0.14: nets, tools, modules, blocks, progress

const extraItems = {
  small_landing_net: ['Kleiner Kescher', 'Small Landing Net', 'In die andere Hand: landet Fische bis 60 cm und 3 kg. Zielanzeige im Drill – schöpfen, wenn der Fisch im grünen Feld ist.', 'In the other hand: lands fish up to 60 cm and 3 kg. Aim gauge in the fight – scoop while the fish is in the green field.'],
  carp_landing_net: ['Karpfenkescher', 'Carp Landing Net', 'In die andere Hand: Fische bis 120 cm und 25 kg, weite Öffnung.', 'In the other hand: fish up to 120 cm and 25 kg, wide opening.'],
  big_game_landing_net: ['Großfischkescher', 'Big Game Landing Net', 'In die andere Hand: für Riesen bis 3,2 m und alle Bossfische.', 'In the other hand: for giants up to 3.2 m and every boss fish.'],
  mother_of_pearl: ['Perlmutt', 'Mother-of-Pearl', 'Seltene Beigabe epischer Fänge. In der Werkstatt: Rute verstärken (bis 3×, je +6 % Hub und Belastbarkeit).', 'Rare bonus from epic catches. In the workshop: reinforce a rod (up to 3×, +6% lift and load each).'],
  groundbait: ['Futtermischung', 'Groundbait', 'Aufs Wasser werfen: 5 Minuten Futterplatz, Friedfische kommen und beißen eher. Füttert Fische im Netz.', 'Throw on water: a feeding spot for 5 minutes, coarse fish gather and bite sooner. Feeds penned fish.'],
  echo_sounder: ['Echolot', 'Echo Sounder', 'Rechtsklick: Tiefe, Fangdruck und welche Fische in 24 Blöcken stehen.', 'Use: depth, fishing pressure and which fish are within 24 blocks.'],
  bait_box: ['Köderbox', 'Bait Box', 'Rechtsklick packt alle Köder ein (9 Stapel), Schleichen leert sie. Füllt den Köderplatz der Rute automatisch nach.', 'Use packs all bait (9 stacks), sneak empties it. Refills the rod\'s bait slot automatically.'],
  fish_lexicon: ['Fischlexikon', 'Fish Lexicon', 'Arten, Aufträge, Angelgebiete, Rekorde, Rangliste und Statistik.', 'Species, orders, fishing grounds, records, ranking and statistics.'],
  fishing_crate: ['Angelkiste', 'Fishing Crate', 'Statt eines Fisches an den Haken gegangen. Rechtsklick öffnet sie: Fundstücke aus dem Gewässer, Köder oder Haken, oft ein Fundzettel.', 'Hooked instead of a fish. Use to open: finds from the water, bait or hooks, often a fishy note.'],
  fishy_note: ['Fundzettel', 'Fishy Note', 'Ein nasser Zettel aus einer Angelkiste. Er verrät, wo, wann und womit ein Fisch beißt.', 'A soggy note from a fishing crate. It tells where, when and with what a fish bites.'],
};
const modules = {};
for (const [id, [german, english, germanTip, englishTip]] of Object.entries(modules)) extraItems[`${id}_module`] = [german, english, germanTip, englishTip];
for (const id of Object.keys(extraItems)) {
  json(`assets/atzen/models/item/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `atzen:item/${id}` } });
  json(`assets/atzen/items/${id}.json`, { model: { type: 'minecraft:model', model: `atzen:item/${id}` } });
}

const blocks = {
  keepnet: ['Setzkescher', 'Keepnet', 'Ins Wasser stellen: hält bis 8 Fische (40 kg) am Leben. Fisch rein: Rechtsklick mit Fisch; raus: leere Hand; alle freilassen: Schleichen.', 'Set it in water: keeps up to 8 fish (40 kg) alive. In: use a fish on it; out: empty hand; release all: sneak.'],
  rod_holder: ['Rutenhalter', 'Rod Holder', 'Rute daraufklicken: wirft vor dem Halter aus. Bei Biss (Klicken + Spritzer) den Halter anklicken und drillen.', 'Click a rod on it: casts in front of the holder. On a bite (click + splash) click the holder and fight.'],
  double_rod_holder: ['Doppelständer', 'Double Rod Stand', 'Zwei Ruten nebeneinander. Liegen zwei Köder zu nah, verheddern sich die Schnüre.', 'Two rods side by side. Baits lying too close make the lines tangle.'],
  rod_pod: ['Rod Pod', 'Rod Pod', 'Drei Ruten mit elektronischem Bissanzeiger (lauter Alarm); große Fische ziehen hier keine Rute ins Wasser.', 'Three rods with an electronic bite alarm (loud); big fish cannot pull a rod in here.'],
  fish_pen: ['Unterwasser-Netz', 'Fish Pen', 'Unter Wasser setzen: Fische leben darin, je 24 Wasserblöcke einer. Füttern mit Köder/Futter; satte, gesunde Paare vermehren sich.', 'Place under water: fish live in it, one per 24 water blocks. Feed with bait/groundbait; well-fed healthy pairs breed.'],
  fish_market: ['Fischmarkt', 'Fish Market', 'Fisch verkaufen: Rechtsklick mit Fisch, Schleichen verkauft alle. Seltenheit, Gewicht, Kontrolle und Frische zählen.', 'Sell fish: use a fish on it, sneak sells all. Rarity, weight, control and freshness count.'],
  workshop: ['Angelwerkstatt', 'Angler\'s Workshop', 'Rute in der Haupthand, in der anderen: Smaragd (reparieren) oder Perlmutt (verstärken, bis 3×).', 'Rod in the main hand, in the other: emerald (repair) or mother-of-pearl (reinforce, up to 3×).'],
};
const holders = { rod_holder: 'holder_wood', double_rod_holder: 'holder_wood', rod_pod: 'holder_metal' };
for (const id of Object.keys(blocks)) {
  json(`data/atzen/loot_table/blocks/${id}.json`, {
    type: 'minecraft:block', pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: `atzen:${id}` }],
      conditions: [{ condition: 'minecraft:survives_explosion' }] }],
  });
  json(`assets/atzen/items/${id}.json`, { model: { type: 'minecraft:model', model: `atzen:block/${id}` } });
}
const box = (from, to, texture, faces = ['north', 'south', 'east', 'west', 'up', 'down']) => ({
  from, to, faces: Object.fromEntries(faces.map(face => [face, { texture }])),
});
json('assets/atzen/models/block/keepnet.json', {
  parent: 'minecraft:block/block', textures: { particle: 'atzen:block/net', net: 'atzen:block/net', ring: 'atzen:block/holder_wood' },
  elements: [box([2, 0, 2], [14, 12, 14], '#net'), box([1, 12, 1], [15, 14, 15], '#ring', ['north', 'south', 'east', 'west', 'up'])],
});
json('assets/atzen/blockstates/keepnet.json', { variants: { '': { model: 'atzen:block/keepnet' } } });
json('assets/atzen/models/block/fish_pen.json', {
  parent: 'minecraft:block/block', textures: { particle: 'atzen:block/net', net: 'atzen:block/net', frame: 'atzen:block/holder_wood' },
  elements: [box([0, 0, 0], [16, 4, 16], '#net'), box([0, 0, 0], [2, 5, 2], '#frame'), box([14, 0, 0], [16, 5, 2], '#frame'),
    box([0, 0, 14], [2, 5, 16], '#frame'), box([14, 0, 14], [16, 5, 16], '#frame')],
});
json('assets/atzen/blockstates/fish_pen.json', { variants: { '': { model: 'atzen:block/fish_pen' } } });
for (const id of ['fish_market', 'workshop']) {
  json(`assets/atzen/models/block/${id}.json`, {
    parent: 'minecraft:block/cube_bottom_top',
    textures: { side: `atzen:block/${id}_side`, top: `atzen:block/${id}_top`, bottom: 'minecraft:block/spruce_planks' },
  });
  json(`assets/atzen/blockstates/${id}.json`, { variants: { '': { model: `atzen:block/${id}` } } });
}
// Rod holders: posts with a rest for each rod; a rod lying in when loaded, the indicator red on a bite.
const rodsOf = { rod_holder: 1, double_rod_holder: 2, rod_pod: 3 };
for (const [id, material] of Object.entries(holders)) {
  const count = rodsOf[id];
  for (const variant of ['', '_loaded', '_bite']) {
    const elements = [];
    for (let i = 0; i < count; i++) {
      const x = count === 1 ? 7 : count === 2 ? 3 + i * 8 : 1.5 + i * 5.5;
      elements.push(box([x, 0, 7], [x + 2, 10, 9], '#post'));
      elements.push(box([x - 1, 10, 6], [x + 3, 11, 10], '#post'));
      if (variant) {
        elements.push({ from: [x + 0.5, 10.5, -6], to: [x + 1.5, 11.5, 16], rotation: { angle: 22.5, axis: 'x', origin: [x + 1, 11, 8] },
          faces: Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map(face => [face, { texture: '#rod' }])) });
        elements.push(box([x + 0.5, 4, 5.5], [x + 1.5, 6, 6.5], variant === '_bite' ? '#alarm_on' : '#alarm_off'));
      }
    }
    if (count === 3) elements.push(box([0, 0, 6.5], [16, 1.5, 9.5], '#post'));
    json(`assets/atzen/models/block/${id}${variant}.json`, {
      parent: 'minecraft:block/block',
      textures: { particle: `atzen:block/${material}`, post: `atzen:block/${material}`, rod: 'atzen:block/rod_blank',
        alarm_on: 'atzen:block/alarm_on', alarm_off: 'atzen:block/alarm_off' },
      elements,
    });
  }
  const variants = {};
  const yRot = { north: 0, east: 90, south: 180, west: 270 };
  for (const [facing, y] of Object.entries(yRot)) for (const loaded of [false, true]) for (const bite of [false, true]) {
    const model = !loaded ? id : bite ? `${id}_bite` : `${id}_loaded`;
    variants[`bite=${bite},facing=${facing},loaded=${loaded}`] = { model: `atzen:block/${model}`, ...(y ? { y } : {}) };
  }
  json(`assets/atzen/blockstates/${id}.json`, { variants });
}

const extraRecipes = {
  small_landing_net: ['minecraft:stick', 'minecraft:stick', 'minecraft:string', 'minecraft:string', 'minecraft:string'],
  carp_landing_net: ['atzen:small_landing_net', 'minecraft:iron_ingot', 'minecraft:string', 'minecraft:string', 'minecraft:string'],
  big_game_landing_net: ['atzen:carp_landing_net', 'minecraft:iron_block', 'atzen:fishing_line', 'minecraft:leather'],
  groundbait: { count: 4, ingredients: ['minecraft:wheat', 'minecraft:bread', 'minecraft:bone_meal', 'minecraft:dirt'] },
  echo_sounder: ['minecraft:copper_ingot', 'minecraft:redstone', 'minecraft:amethyst_shard', 'minecraft:glass_pane'],
  bait_box: ['minecraft:chest', 'minecraft:copper_ingot'],
  fish_lexicon: ['minecraft:book', 'minecraft:cod', 'minecraft:ink_sac'],
  keepnet: ['minecraft:string', 'minecraft:string', 'minecraft:string', 'minecraft:string', 'minecraft:stick', 'minecraft:stick'],
  rod_holder: ['minecraft:stick', 'minecraft:stick', 'minecraft:stick', 'minecraft:oak_slab'],
  double_rod_holder: ['atzen:rod_holder', 'atzen:rod_holder', 'minecraft:stick'],
  rod_pod: ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:redstone', 'minecraft:note_block'],
  fish_pen: ['atzen:keepnet', 'atzen:keepnet', 'minecraft:iron_bars', 'minecraft:string'],
  fish_market: ['minecraft:barrel', 'minecraft:emerald', 'minecraft:oak_sign', 'minecraft:cod'],
  workshop: ['minecraft:smithing_table', 'minecraft:fishing_rod', 'minecraft:iron_ingot'],
};
for (const [id, recipe] of Object.entries(extraRecipes)) {
  const ingredients = Array.isArray(recipe) ? recipe : recipe.ingredients;
  const count = Array.isArray(recipe) ? 1 : recipe.count;
  json(`data/atzen/recipe/${id}.json`, { type: 'minecraft:crafting_shapeless', category: 'misc', ingredients, result: { id: `atzen:${id}`, count } });
  json(`data/atzen/advancement/recipes/${id}.json`, {
    parent: 'minecraft:recipes/root',
    criteria: {
      has_ingredient: { trigger: 'minecraft:inventory_changed', conditions: { items: [{ items: ingredients[0] }] } },
      has_the_recipe: { trigger: 'minecraft:recipe_unlocked', conditions: { recipe: `atzen:${id}` } },
    },
    requirements: [['has_ingredient', 'has_the_recipe']], rewards: { recipes: [`atzen:${id}`] },
  });
}

const progressTranslations = {
  'catch.atzen.condition': ['Zustand: %s %%', 'Condition: %s%%'],
  'message.atzen.need_net': ['Zu schwer für die Hand – nimm einen Kescher in die andere Hand!', 'Too heavy for your hand – hold a landing net in the other hand!'],
  'message.atzen.net_too_small': ['Der Kescher ist zu klein für diesen Fisch!', 'The net is too small for this fish!'],
  'message.atzen.groundbait': ['Futterplatz angelegt – Friedfische ziehen heran.', 'Feeding spot made – coarse fish are coming.'],
  'message.atzen.sonar': ['Echolot: %s m tief · Fangdruck %s %%', 'Echo sounder: %s m deep · fishing pressure %s%%'],
  'message.atzen.sonar_empty': ['Keine Fische in Reichweite.', 'No fish in range.'],
  'message.atzen.bait_box_filled': ['%s Köder eingepackt (%s/%s Stapel).', 'Packed %s baits (%s/%s stacks).'],
  'message.atzen.bait_box_emptied': ['Köderbox ausgeleert.', 'Bait box emptied.'],
  'message.atzen.bait_refilled': ['Köder aus der Köderbox nachgelegt.', 'Bait refilled from the bait box.'],
  'message.atzen.tool_instruction': ['Halte eine eingeholte Atzen-Rute in der anderen Hand.', 'Hold a reeled-in Atzen rod in the other hand.'],
  'message.atzen.performance': ['Leistungsstufe: %s', 'Performance tier: %s'],
  'message.atzen.unknown_species': ['Unbekannte Fischart: %s', 'Unknown species: %s'],
  'performance.atzen.low': ['niedrig (Fische voll aktiv bis 24 Blöcke, Rutenhalter alle 2 s)', 'low (fish fully active within 24 blocks, rod holders every 2 s)'],
  'performance.atzen.normal': ['normal (48 Blöcke, Rutenhalter jede Sekunde)', 'normal (48 blocks, rod holders every second)'],
  'performance.atzen.high': ['hoch (96 Blöcke, Rutenhalter alle 0,5 s)', 'high (96 blocks, rod holders every 0.5 s)'],
  'holding.atzen.problem.volume': ['zu wenig Wasser', 'not enough water'],
  'holding.atzen.problem.oxygen': ['zu wenig Sauerstoff', 'not enough oxygen'],
  'holding.atzen.problem.temperature': ['falsche Wassertemperatur für diese Art', 'wrong water temperature for this species'],
  'holding.atzen.problem.density': ['zu viele Fische', 'too many fish'],
  'holding.atzen.problem.quality': ['schlechte Wasserqualität', 'poor water quality'],
  'holding.atzen.water_good': ['Wasser gut', 'water good'],
  'holding.atzen.add.ok': ['Im Setzkescher: %s/%s Fische, %s/%s kg', 'In the keepnet: %s/%s fish, %s/%s kg'],
  'holding.atzen.add.full': ['Setzkescher voll (%s/%s)', 'Keepnet full (%s/%s)'],
  'holding.atzen.add.too_heavy': ['Zu schwer: %3$s/%4$s kg', 'Too heavy: %3$s/%4$s kg'],
  'holding.atzen.add.too_big': ['Zu groß für den Setzkescher', 'Too big for the keepnet'],
  'holding.atzen.add.duplicate': ['Dieser Fisch ist schon drin.', 'This fish is already in there.'],
  'holding.atzen.add.dead': ['Der Fisch lebt nicht mehr.', 'The fish is no longer alive.'],
  'holding.atzen.status': ['Setzkescher: %s/%s Fische, %s/%s kg – %s', 'Keepnet: %s/%s fish, %s/%s kg – %s'],
  'holding.atzen.taken': ['%s herausgenommen (%s/%s)', 'Took out %s (%s/%s)'],
  'holding.atzen.released_all': ['%s Fische freigelassen.', 'Released %s fish.'],
  'holding.atzen.warning': ['Setzkescher: %s! Zustand der Fische %s %%', 'Keepnet: %s! Fish condition %s%%'],
  'holding.atzen.died': ['Ein Fisch im Setzkescher ist gestorben: %s', 'A fish in the keepnet died: %s'],
  'holding.atzen.pen_warning': ['Unterwasser-Netz: %s!', 'Fish pen: %s!'],
  'holding.atzen.pen_full': ['Netz voll: %s/%s – mehr Wasser drumherum schafft Platz.', 'Pen full: %s/%s – more water around makes room.'],
  'holding.atzen.pen_added': ['%s im Netz (%s/%s)', '%s in the pen (%s/%s)'],
  'holding.atzen.pen_status': ['Netz: %s/%s Fische, Zustand %s %% – %s', 'Pen: %s/%s fish, condition %s%% – %s'],
  'holding.atzen.fed': ['%s Fische gefüttert.', 'Fed %s fish.'],
  'holder.atzen.full': ['Alle Plätze belegt.', 'All rests are taken.'],
  'holder.atzen.no_water': ['Kein Wasser vor dem Halter.', 'No water in front of the holder.'],
  'holder.atzen.placed': ['Rute %s/%s ausgeworfen.', 'Rod %s/%s cast out.'],
  'holder.atzen.hands_full': ['Kein Platz im Inventar für das, was du in der Hand hast.', 'No room in your inventory for what you are holding.'],
  'holder.atzen.missed': ['Zu spät – oder du drillst schon.', 'Too late – or you are already fighting.'],
  'holder.atzen.empty': ['Keine Rute im Halter.', 'No rod in the holder.'],
  'holder.atzen.tangled': ['Die Schnüre haben sich verheddert! Köder verloren.', 'The lines tangled! Bait lost.'],
  'holder.atzen.rod_pulled': ['Ein großer Fisch hat deine Rute ins Wasser gezogen!', 'A big fish pulled your rod into the water!'],
  'holder.atzen.line_taken': ['Ein Fisch hat Schnur genommen und ist mit dem Köder weg.', 'A fish took line and left with the bait.'],
  'holder.atzen.escaped': ['Biss verpasst – der Köder ist weg.', 'Bite missed – the bait is gone.'],
  'market.atzen.sold': ['%s Fisch(e) verkauft: %s Smaragde (Guthaben %s)', 'Sold %s fish: %s emeralds (credit %s)'],
  'market.atzen.help': ['Fischmarkt: Rechtsklick mit einem Fisch verkauft ihn, Schleichen verkauft alle. Seltenheit, Gewicht, Kontrolle und Frische zählen; dieselbe Art am selben Tag bringt weniger.', 'Fish market: use a fish to sell it, sneak to sell all. Rarity, weight, control and freshness count; the same species on the same day pays less.'],
  'workshop.atzen.help': ['Werkstatt: Rute in die Haupthand, in die andere Hand Smaragd (reparieren) oder Perlmutt (verstärken).', 'Workshop: rod in the main hand, in the other an emerald (repair) or mother-of-pearl (reinforce).'],
  'workshop.atzen.not_damaged': ['Die Rute ist heil.', 'The rod is not damaged.'],
  'workshop.atzen.repaired': ['Rute repariert.', 'Rod repaired.'],
  'workshop.atzen.max_upgrades': ['Schon maximal verstärkt (%s/%s).', 'Already fully reinforced (%s/%s).'],
  'workshop.atzen.upgraded': ['Rute verstärkt (%s/%s).', 'Rod reinforced (%s/%s).'],
  'progress.atzen.xp': ['+%s Angler-Erfahrung', '+%s angler experience'],
  'progress.atzen.released': ['Zurückgesetzt: +%s Erfahrung', 'Released: +%s experience'],
  'progress.atzen.level_up': ['Anglerstufe %s erreicht!', 'Angler level %s reached!'],
  'progress.atzen.area_unlocked': ['Neues Angelgebiet: %s – dort beißen jetzt auch seltene Fische.', 'New fishing ground: %s – rare fish bite there now.'],
  'progress.atzen.quest_done': ['Auftrag erfüllt: %s (+%s Smaragde, +%s Erfahrung)', 'Order done: %s (+%s emeralds, +%s experience)'],
  'progress.atzen.record': ['Serverrekord: %s!', 'Server record: %s!'],
  'progress.atzen.any_fish': ['jeder Fisch', 'any fish'],
  'progress.atzen.tournament_start': ['Angelturnier! %s Minuten, gewertet: %s – der schwerste Fisch gewinnt.', 'Fishing tournament! %s minutes, counted: %s – the heaviest fish wins.'],
  'progress.atzen.tournament_end': ['Das Angelturnier ist vorbei!', 'The fishing tournament is over!'],
  'progress.atzen.tournament_place': ['%s. %s – %s kg', '%s. %s – %s kg'],
  'quest.atzen.species': ['Fange %1$s× %2$s', 'Catch %1$s× %2$s'],
  'quest.atzen.weight': ['Fange einen Fisch ab %3$s kg', 'Catch a fish of %3$s kg or more'],
  'quest.atzen.habitat': ['Fange %1$s Fische: %2$s', 'Catch %1$s fish: %2$s'],
  'quest.atzen.rarity': ['Fange einen Fisch der Seltenheit %2$s oder besser', 'Catch a fish of rarity %2$s or better'],
  'quest.atzen.release': ['Setze %1$s Fische zurück', 'Release %1$s fish'],
  'area.atzen.home_lake': ['Heimatsee (Seen)', 'Home Lake (lakes)'],
  'area.atzen.forest_river': ['Waldfluss (Flüsse, kalte Gewässer)', 'Forest River (rivers, cold waters)'],
  'area.atzen.mist_moor': ['Nebelmoor (Sümpfe)', 'Mist Moor (swamps)'],
  'area.atzen.coast': ['Küste (Meer, warmes Meer)', 'Coast (sea, warm sea)'],
  'area.atzen.deep_reserve': ['Tiefenreservat (Nordmeer, Tiefsee)', 'Deep Reserve (northern sea, deep sea)'],
  'area.atzen.caverns': ['Höhlensystem (Wasser unter y 50 ohne Himmel)', 'Caverns (water below y 50 without sky)'],
  'area.atzen.lava_lakes': ['Lavaseen (Lava, nur mit Netheritrute)', 'Lava Lakes (lava, netherite rod only)'],
  'area.atzen.end_waters': ['Endgewässer (Wasser im End)', 'End Waters (water in the End)'],
  'crate.atzen.hooked': ['Eine Angelkiste hängt am Haken!', 'A fishing crate is on the hook!'],
  'crate.atzen.opened': ['Angelkiste geöffnet.', 'Fishing crate opened.'],
  'crate.atzen.from': ['Fundort: %s', 'Found in: %s'],
  'note.atzen.fish': ['Über den %s:', 'About the %s:'],
  'note.atzen.where': ['Lebt in: %s', 'Lives in: %s'],
  'note.atzen.when.twilight': ['Beißt am liebsten in der Dämmerung.', 'Bites best at dusk and dawn.'],
  'note.atzen.when.night': ['Beißt am liebsten nachts.', 'Bites best at night.'],
  'note.atzen.when.day': ['Beißt am liebsten tagsüber.', 'Bites best by day.'],
  'note.atzen.when.always': ['Beißt zu jeder Tageszeit.', 'Bites at any time of day.'],
  'note.atzen.bait': ['Lieblingsköder: %s', 'Favourite bait: %s'],
  'note.atzen.lava': ['Nur mit einer Netheritrute zu fangen.', 'Only to be caught with a netherite rod.'],
  'lexicon.atzen.title': ['Fischlexikon', 'Fish Lexicon'],
  'lexicon.atzen.tab.species': ['Arten', 'Species'],
  'lexicon.atzen.tab.quests': ['Aufträge', 'Orders'],
  'lexicon.atzen.tab.records': ['Rekorde', 'Records'],
  'lexicon.atzen.tab.stats': ['Statistik', 'Statistics'],
  'lexicon.atzen.known': ['Entdeckt: %s von %s Arten', 'Discovered: %s of %s species'],
  'lexicon.atzen.species_line': [' – %s× gefangen, größter %s kg / %s cm', ' – caught %s×, biggest %s kg / %s cm'],
  'lexicon.atzen.daily': ['[Tag] ', '[Day] '],
  'lexicon.atzen.weekly': ['[Woche] ', '[Week] '],
  'lexicon.atzen.quest_progress': [' – %s/%s · %s Smaragde', ' – %s/%s · %s emeralds'],
  'lexicon.atzen.areas': ['Angelgebiete (seltene Fische ab Stufe):', 'Fishing grounds (rare fish from level):'],
  'lexicon.atzen.area_line': ['%s – ab Stufe %s', '%s – from level %s'],
  'lexicon.atzen.tournament_running': ['Turnier läuft noch %s Minuten!', 'Tournament running for %s more minutes!'],
  'lexicon.atzen.ranking': ['Rangliste – die schwersten Fänge', 'Ranking – the heaviest catches'],
  'lexicon.atzen.records': ['Serverrekorde je Art', 'Server records per species'],
  'lexicon.atzen.level': ['Anglerstufe %s – %s Erfahrung (nächste Stufe bei %s)', 'Angler level %s – %s experience (next level at %s)'],
  'lexicon.atzen.stat.catches': ['Gefangene Fische: %s', 'Fish caught: %s'],
  'lexicon.atzen.stat.released': ['Zurückgesetzt: %s', 'Released: %s'],
  'lexicon.atzen.stat.bosses': ['Bossfische: %s', 'Boss fish: %s'],
  'lexicon.atzen.stat.earned': ['Am Fischmarkt verdient: %s Smaragde', 'Earned at the fish market: %s emeralds'],
};
for (const [id, [german, english, germanTip, englishTip]] of Object.entries({ ...extraItems, ...blocks })) {
  de[`item.atzen.${id}`] = german; en[`item.atzen.${id}`] = english;
  de[`tooltip.atzen.${id}`] = germanTip; en[`tooltip.atzen.${id}`] = englishTip;
}
for (const [id, [german, english]] of Object.entries(blocks)) { de[`block.atzen.${id}`] = german; en[`block.atzen.${id}`] = english; }
for (const [key, [german, english]] of Object.entries(progressTranslations)) { de[key] = german; en[key] = english; }
json('assets/atzen/lang/de_de.json', de);
json('assets/atzen/lang/en_us.json', en);
console.log(`Generated ${Object.keys(extraItems).length} items and ${Object.keys(blocks).length} blocks of 0.8–0.14.`);
