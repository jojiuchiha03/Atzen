# Atzen

Fabric-Mod für Minecraft **26.2** (Java 25 zum Bauen, Fabric Loader ≥ 0.19, Fabric API).

## Bauen

```bash
tools/fetch-libs.sh   # holt Server-/Client-Jar, Bibliotheken und Fabric-API-Module nach libs/
gradle build          # Jar landet in build/libs/
```

Das Jar kommt so wie es ist in den `mods`-Ordner (Server und Client, dazu Fabric API).

## Aufbau

| Pfad | Inhalt |
|------|--------|
| `src/main/java/.../atzen/Atzen.java` | Einstieg, registriert alles |
| `src/main/java/.../atzen/ModItems.java` | Items + Kreativ-Tab (Beispiel: Atzen-Münze) |
| `src/main/java/.../atzen/mixin/` | Mixins, jeweils in `atzen.mixins.json` eintragen |
| `src/client/java/.../atzen/client/` | Nur-Client-Code (Renderer, HUD, Tasten) |
| `src/main/resources/assets/atzen/` | Texturen, Modelle, Übersetzungen |
| `src/main/resources/data/atzen/` | Rezepte, Loot-Tabellen, Tags |

Version in `gradle.properties` (`mod_version`) hochzählen, Tag `v<version>` pushen → GitHub Actions baut und veröffentlicht das Release.
