# Changelog

## [0.3.0] - 2026-10-12

### Neu
- Kein HUD mehr: Beim Drill kreist die Kamera langsam um Angler und Pose, rückt beim Einholen näher und wackelt, wenn die Schnur kurz vorm Reißen ist oder der Fisch flüchtet. Danach weich zurück in die eigene Ansicht.
- Der Fisch hängt sichtbar an der Schnur (für alle Spieler): kreist unter der Pose, zappelt vor der Flucht, springt bei der Flucht aus dem Wasser und wird beim Einholen zum Angler gezogen. Der Spieler kurbelt sichtbar.
- Schnurspannung als Ton: Knarzen wird mit der Spannung lauter und schneller, die Rolle klickt, eine schlaffe Schnur platscht. Beim ersten Biss ein kurzer Steuerungshinweis im Chat.
- Köder-Slot an jeder Atzen-Rute: Köder im Inventar auf die Rute ziehen, Rechtsklick nimmt ihn heraus; der Tooltip zeigt den Slot. Köder in der anderen Hand funktionieren weiterhin.
- Eigener Kreativ-Tab „Atzen – Angeln“, sortiert: Angeln, Köder, Bücher, Fische, Eimer, Spawn-Eier.

### Entfernt
- Atzen-Münze samt Rezept.

- Fische aus der Welt schwimmen zur Pose und beißen an (die Pose taucht ab wie bei Vanilla); wer anschlägt, kämpft gegen genau diesen Fisch. Mit Köder beißen sie sicher, ohne Köder nur manchmal. Zieht man nicht an, schwimmt der Fisch weiter.
- Alle Texturen neu gezeichnet (64×64 Pixelkunst mit Kontur und Lichtschattierung): Fische, Angeln, Köder, Bücher, eigene Fischeimer mit Fisch im Wasser, Fischhäute. Erzeugt von `scripts/draw-textures.py`.

### Behoben
- Fische spawnen jetzt: Die Vanilla-Regel ließ sie nur zwischen Meeresspiegel −13 und Meeresspiegel zu, Seen und Flüsse darüber blieben leer. Jetzt zählt jedes Wasser mit freiem Himmel darüber (keine Höhlen). Zusätzlich dünner besetzt in Seen und Teichen aller Oberwelt-Biome außer Ozean und Strand.

## [0.2.0] - 2026-10-11

### Neu
- Angelduelle mit Schnurspannung, Fluchten, Ausdauer und Trainingsmodus.
- Sechs Fischarten als animierte Tiere, mit individuellen Längen, Gewichten und Fangdaten; Transport im Eimer.
- Drei Angeln, drei Köder und drei Verzauberungen mit eigenen Texturen und Rezepten.
- 14 automatisierte Build-/Logik-/Ressourcenprüfungen; kein Ingame-Test durchgeführt.

### Geändert
- Reproduzierbarer Build mit Gradle-Wrapper, Fabric Loom 1.17.21, Java 25 und Fabric API 0.161.0+26.2.
- Atzen-Münze, Kreativ-Tab und Autorenangaben aus 0.1.0 bleiben erhalten.

## [0.1.0] - 2026-10-11

### Neu
- Grundgerüst für Minecraft 26.2 (Fabric Loader 0.19.5, Fabric API 0.160.0+26.2).
- Atzen-Münze (2 Goldklumpen formlos) und eigener Kreativ-Tab „Atzen“.
