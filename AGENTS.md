# Hinweise für KI-Agenten

- **Minecraft 26.2 ist unverschleiert.** Es gibt weder Yarn noch Mojang-Mappings, Fabric Loom funktioniert nicht.
  Gebaut wird mit reinem Gradle direkt gegen `libs/server.jar` / `libs/client.jar` (siehe `build.gradle`).
  Klassennamen sind die echten Mojang-Namen (`net.minecraft.world.item.Item`, `Identifier` statt `ResourceLocation`).
- Vor dem ersten Build `tools/fetch-libs.sh` ausführen. `libs/` wird nie committet.
- Bauen: `gradle build` mit **JDK 25**. Ziel-Bytecode ist Java 21.
- Client und Server sind getrennte Source-Sets: `src/main` darf keine Klassen aus `net.minecraft.client` benutzen,
  sonst bricht der Build (und der Server würde abstürzen). Client-Code gehört nach `src/client`.
- Neue Items: in `ModItems` registrieren (`Properties.setId(key)` ist Pflicht), dazu `assets/atzen/items/<name>.json`,
  `models/item/<name>.json`, Textur `textures/item/<name>.png` (16×16) und Einträge in `lang/de_de.json` + `en_us.json`.
- Blöcke analog: `BlockBehaviour.Properties...setId(blockKey)`, plus `BlockItem` mit `useBlockDescriptionPrefix()`, `blockstates/`.
- Mixins nach `io.github.jojiuchiha03.atzen.mixin` und in `atzen.mixins.json` eintragen (`mixins` = beide Seiten, `client` = nur Client).
- 26.2-Falle: Grasblöcke sind nicht mehr in `#minecraft:dirt`, sondern in `#minecraft:grass_blocks` / `#minecraft:substrate_overworld`.
- Mod-ID und Namensraum: `atzen`. Paket: `io.github.jojiuchiha03.atzen`.
- Jede Änderung muss `gradle build` bestehen (CI prüft das bei jedem Push).
