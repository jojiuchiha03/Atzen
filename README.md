# Atzen

Fabric-Mod für Minecraft **26.2** (Java 25 zum Bauen, Fabric Loader ≥ 0.19, Fabric API).

## Bauen

```bash
tools/fetch-libs.sh   # holt Server-/Client-Jar, Bibliotheken und Fabric-API-Module nach libs/
gradle build          # Jar landet in build/libs/
```


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

## Auf den Server bringen

Getestet mit Minecraft 26.2, Fabric Loader 0.19.5 und Fabric API 0.160.0+26.2.

1. Jar aus dem [neuesten Release](https://github.com/jojiuchiha03/Atzen/releases/latest) holen (`atzen-<version>.jar`, **nicht** `-sources.jar`).
2. Server stoppen, altes `atzen-*.jar` aus `mods/` entfernen, neues hineinlegen, Server starten.
3. Im Log muss `Atzen geladen` stehen.
4. **Die Mod muss auch bei allen Spielern im Client installiert sein** (sie bringt eigene Items mit),
   also das Client-Modpack mit demselben Jar aktualisieren. Sonst werden Spieler beim Beitreten abgewiesen.

Die Mod legt keine Konfigurationsdatei an und verändert keine bestehende Welt.
