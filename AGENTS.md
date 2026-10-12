# Hinweise für KI-Agenten

- **Minecraft 26.2 ist unverschleiert.** Es gibt weder Yarn noch Mojang-Mappings.
  Der Build nutzt Fabric Loom 1.17.21 ohne Mapping-Abhängigkeit und den Gradle-Wrapper 9.5.1.
  Klassennamen sind die echten Mojang-Namen (`net.minecraft.world.item.Item`, `Identifier` statt `ResourceLocation`).
- Loom lädt Minecraft und Fabric-Abhängigkeiten selbst. `tools/fetch-libs.sh` bleibt ein Legacy-Hilfswerkzeug; `libs/` wird nie committet.
- Bauen: `./gradlew build` mit **JDK 25**, unter Windows `scripts/build.ps1 build`. Ziel-Bytecode ist Java 25.
- Client und Server sind getrennte Source-Sets: `src/main` darf keine Klassen aus `net.minecraft.client` benutzen,
  sonst bricht der Build (und der Server würde abstürzen). Client-Code gehört nach `src/client`.
- Items: Angelzubehör wird in `AtzenItems`, Fische in `AtzenFishRegistry` registriert (`Properties.setId(key)` ist Pflicht). Die Reihenfolge im Kreativtab legt `ModItems.ordered()` fest.
  Dazu `assets/atzen/items/<name>.json`, `models/item/<name>.json`, PNG-Textur (64×64, in `scripts/draw-textures.py`) und Einträge in beiden Sprachdateien ergänzen.
  Generierte Ressourcen und Übersetzungen auch in `scripts/generate-resources.mjs` pflegen.
- Texturen (Items, Eimer, Fischhäute, alle 64×64) zeichnet `python3 scripts/draw-textures.py` neu; Farben und Formen dort ändern, nicht in den PNGs.
  `scripts/import-textures.ps1` (alte Bildvorlagen aus `art/`) nicht mehr ausführen, es würde die gezeichneten Texturen überschreiben.
- Blöcke analog: `BlockBehaviour.Properties...setId(blockKey)`, plus `BlockItem` mit `useBlockDescriptionPrefix()`, `blockstates/`.
- Mixins nach `io.github.jojiuchiha03.atzen.mixin` und in `atzen.mixins.json` eintragen (`mixins` = beide Seiten, `client` = nur Client).
- 26.2-Falle: Grasblöcke sind nicht mehr in `#minecraft:dirt`, sondern in `#minecraft:grass_blocks` / `#minecraft:substrate_overworld`.
- Mod-ID und Namensraum: `atzen`. Paket: `io.github.jojiuchiha03.atzen`.
- Jede Änderung muss `./gradlew build` bestehen (CI prüft das bei jedem Push). Die JUnit-Tests starten keine Minecraft-Welt.
