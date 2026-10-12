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
  river_rod: ['Flussrute', 'River Rod', '10 % weniger Spannung, 5 % schnelleres Einholen.', '10% less strain, 5% faster reeling.'],
  iron_rod: ['Eisenrute', 'Iron Rod', '22 % weniger Spannung. Reparatur: Eisenbarren.', '22% less strain. Repair with iron ingots.'],
  tide_rod: ['Gezeitenrute', 'Tide Rod', '25 % schneller, 10 % weniger Spannung. Reparatur: Prismarin.', '25% faster, 10% less strain. Repair with prismarine shards.'],
  worm_bait: ['Wurmköder', 'Worm Bait', 'Im Köder-Slot: 15 % schneller einholen. Ein Köder pro Duell.', 'In the bait slot: reel 15% faster. One bait per battle.'],
  glow_bait: ['Leuchtköder', 'Glow Bait', 'Im Köder-Slot: 20 % weniger Fluchtdruck. Ein Köder pro Duell.', 'In the bait slot: 20% less surge strain. One bait per battle.'],
  hearty_bait: ['Kraftköder', 'Hearty Bait', 'Im Köder-Slot: 25 % schneller, aber 12 % mehr Spannung.', 'In the bait slot: 25% faster reeling, but 12% more strain.'],
  steady_line_tome: ['Zauberbuch: Schnurmeister', 'Tome of Steady Line', 'Rechtsklick mit Angel in der anderen Hand: Schnurmeister +1.', 'Use with a rod in the other hand: Steady Line +1.'],
  reel_power_tome: ['Zauberbuch: Kurbelkraft', 'Tome of Reel Power', 'Rechtsklick mit Angel in der anderen Hand: Kurbelkraft +1.', 'Use with a rod in the other hand: Reel Power +1.'],
  calm_waters_tome: ['Zauberbuch: Wellenruhe', 'Tome of Calm Waters', 'Rechtsklick mit Angel in der anderen Hand: Wellenruhe +1.', 'Use with a rod in the other hand: Calm Waters +1.'],
};

for (const id of Object.keys(items)) {
  json(`assets/atzen/models/item/${id}.json`, {
    parent: id.endsWith('_rod') ? 'minecraft:item/handheld_rod' : 'minecraft:item/generated',
    textures: { layer0: `atzen:item/${id}` },
  });
  json(`assets/atzen/items/${id}.json`, { model: { type: 'minecraft:model', model: `atzen:item/${id}` } });
}

const rods = ['river_rod', 'iron_rod', 'tide_rod'].map(id => `atzen:${id}`);
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
  worm_bait: ['minecraft:rotten_flesh', 'minecraft:wheat_seeds'],
  glow_bait: ['atzen:worm_bait', 'minecraft:glow_ink_sac'],
  hearty_bait: ['minecraft:salmon', 'minecraft:kelp', 'minecraft:wheat'],
  steady_line_tome: ['minecraft:book', 'minecraft:lapis_lazuli', 'minecraft:string', 'minecraft:iron_ingot'],
  reel_power_tome: ['minecraft:book', 'minecraft:lapis_lazuli', 'minecraft:redstone', 'minecraft:amethyst_shard'],
  calm_waters_tome: ['minecraft:book', 'minecraft:lapis_lazuli', 'minecraft:prismarine_crystals', 'minecraft:glow_ink_sac'],
};
for (const [id, ingredients] of Object.entries(recipes)) {
  const count = id === 'worm_bait' ? 6 : id.endsWith('_bait') ? 4 : 1;
  json(`data/atzen/recipe/${id}.json`, {
    type: 'minecraft:crafting_shapeless', category: id.endsWith('_rod') ? 'equipment' : 'misc',
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

const translations = {
  'key.category.atzen.fishing': ['Atzen: Angeln', 'Atzen: Fishing'],
  'key.atzen.reel': ['Schnur einholen (halten)', 'Reel in (hold)'],
  'key.atzen.cancel': ['Angelduell abbrechen', 'Cancel fishing battle'],
  'fish.atzen.darter': ['Flitzer', 'Darter'],
  'fish.atzen.fighter': ['Kämpfer', 'Fighter'],
  'fish.atzen.bruiser': ['Brocken', 'Bruiser'],
  'phase.atzen.steady': ['Jetzt einholen', 'Reel it in'],
  'phase.atzen.warning': ['Flucht steht bevor!', 'Surge incoming!'],
  'phase.atzen.surge': ['Der Fisch flüchtet!', 'The fish is surging!'],
  'phase.atzen.recovery': ['Der Fisch wird müde', 'The fish is tiring'],
  'result.atzen.caught': ['FANG GELANDET!', 'CATCH LANDED!'],
  'result.atzen.snapped': ['SCHNUR GERISSEN!', 'LINE SNAPPED!'],
  'result.atzen.escaped': ['FISCH ENTKOMMEN!', 'FISH ESCAPED!'],
  'result.atzen.timeout': ['DER FISCH WAR ZU AUSDAUERND.', 'THE FISH OUTLASTED YOU.'],
  'result.atzen.cancelled': ['DUELL ABGEBROCHEN.', 'BATTLE CANCELLED.'],
  'enchantment.atzen.steady_line': ['Schnurmeister', 'Steady Line'],
  'enchantment.atzen.reel_power': ['Kurbelkraft', 'Reel Power'],
  'enchantment.atzen.calm_waters': ['Wellenruhe', 'Calm Waters'],
  'message.atzen.tome_instruction': ['Halte eine eingeholte Angel in der anderen Hand und benutze das Buch.', 'Hold a reeled-in rod in the other hand, then use the tome.'],
  'message.atzen.tome_max': ['Diese Verzauberung hat bereits ihre höchste Stufe.', 'This enchantment is already at its maximum level.'],
  'message.atzen.enchanted': ['Angel verzaubert: %s %s', 'Rod enchanted: %s %s'],
  'message.atzen.client_required': ['Installiere Atzen auch auf deinem Client, um Angelduelle zu spielen.', 'Install Atzen on your client to play fishing battles.'],
  'message.atzen.already_fishing': ['Beende zuerst deinen aktuellen Wurf oder dein Duell.', 'Finish your current cast or battle first.'],
  'message.atzen.help': ['Atzen: normal auswerfen und beim Biss rechtsklicken. Im Duell R halten, bei knarzender Schnur und Fluchten loslassen. Köder im Inventar auf die Angel ziehen (Rechtsklick nimmt ihn heraus). /atzen practice startet ein Training ohne Beute, /atzen cancel bricht ab. Tasten sind einstellbar.', 'Atzen: cast normally, then right-click at a bite. Hold R to reel; ease off when the line creaks or the fish surges. Drop bait onto the rod in your inventory (right-click takes it out). /atzen practice starts a no-loot training battle; /atzen cancel stops it. Keys are configurable.'],
  'tooltip.atzen.bait_slot': ['Köder: %s ×%s', 'Bait: %s ×%s'],
  'tooltip.atzen.bait_slot_empty': ['Köder-Slot leer – Köder auf die Angel ziehen', 'Bait slot empty – drop bait onto the rod'],
  'message.atzen.controls_hint': ['Erster Biss! Halte %s zum Einholen – hör auf die Schnur: knarzt sie, lass locker. %s bricht ab.', 'First bite! Hold %s to reel – listen to the line: when it creaks, ease off. %s cancels.'],
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
  river_trout: ['Flussforelle', 'River Trout', 0xB9A2A0],
  perch: ['Barsch', 'Perch', 0x86A848],
  pike: ['Hecht', 'Pike', 0x566E3A],
  carp: ['Karpfen', 'Carp', 0xBE8540],
  catfish: ['Wels', 'Catfish', 0x73638A],
  golden_koi: ['Goldkoi', 'Golden Koi', 0xFFD95B],
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
  'rarity.atzen.rare': ['Selten', 'Rare'],
  'rarity.atzen.legendary': ['Legendär', 'Legendary'],
  'size.atzen.small': ['Kleines Exemplar', 'Small specimen'],
  'size.atzen.normal': ['Ausgewachsen', 'Mature'],
  'size.atzen.large': ['Großes Exemplar', 'Large specimen'],
  'size.atzen.trophy': ['Trophäenfisch', 'Trophy fish'],
  'catch.atzen.measurements': ['Länge: %s cm | Gewicht: %s kg', 'Length: %s cm | Weight: %s kg'],
  'catch.atzen.location': ['Fangort: %s (%s, %s)', 'Caught in: %s (%s, %s)'],
  'catch.atzen.day': ['Welt-Tag: %s', 'World day: %s'],
  'catch.atzen.quality': ['Kontrolle: %s %% | Angler: %s', 'Control: %s%% | Angler: %s'],
  'catch.atzen.wild': ['Wildfang', 'Wild catch'],
  'habitat.atzen.swamp': ['Sumpfgewässer', 'Swamp waters'],
  'habitat.atzen.freshwater': ['Gewässer', 'Waters'],
};
for (const [key, [german, english]] of Object.entries(fishTranslations)) { de[key] = german; en[key] = english; }
json('assets/atzen/lang/de_de.json', de);
json('assets/atzen/lang/en_us.json', en);
console.log('Generated six fish species with buckets, spawn eggs, cooking recipes and catch translations.');
