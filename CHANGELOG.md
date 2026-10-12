# Changelog

## [0.7.0] - 2026-10-12

### Neu
- Fünf Rutentypen: Bambus-, Spinn-, Karpfen-, Fliegen- und Big-Game-Rute. Jede Rute hat Wurfgewicht, Belastbarkeit, Flex (Aktion), Sensibilität, Gewicht, Haltbarkeit und Fischgrößenbereich; keine ist überall die beste.
- Rutenbelastung: Über der Belastbarkeit nutzt sich die Rute schneller ab, beim 1,3-Fachen knackt sie (ein Viertel Haltbarkeit weg, Fisch weg).
- Sechs Wechselspulen (Flach-, Tief-, Leicht-, verstärkte Spule, Schnell- und Kraftübersetzung).
- Vorfächer: Fluorocarbon (fast unsichtbar, abriebfest) und Stahl (gegen Zähne). Hecht, Zander, Barrakuda, Seewolf, Seeteufel, Schilfkönig und Tiefenwächter können eine Schnur ohne Stahlvorfach durchbeißen.
- Acht Haken (klein/normal/groß, Drilling, Schonhaken, Karpfen-, Offset-, Circle-Haken) mit Größe und Festigkeit: zu große Haken schrecken ab, zu schwache biegen auf.
- Die Rute hat jetzt sechs Slots: Rolle, Spule, Schnur, Vorfach, Haken, Köder. Ältere Ruten behalten ihre Teile.
- Wurfweite hängt von Rute, Spule und Ködergewicht ab; Fliegen brauchen Fliegenrute und Fliegenrolle.
- Schnurdurchmesser: dünne Geflochtene passt in größerer Menge auf die Spule.
- Sensible Ruten vergrößern das Zeitfenster für einen guten Anschlag; zu kleine Fische spürt eine schwere Rute kaum.
- Nach dem Anschlag flüchtet der Fisch sofort ins Freiwasser; schon beim Biss zieht er die Pose vom Ufer weg.
- Ergebnis des Drills als Einblendung (Fang, Schnurbruch, durchgebissen, Rute überlastet …).

## [0.6.1] - 2026-10-12

### Behoben
- Ruhigere Kamera im Drill: Einstellungen wechseln erst nach zwei Sekunden statt bei jeder Fischaktion (Sprünge schneiden sofort), weicheres Nachführen, die Richtung zum Fisch wird geglättet (kein Herumschwenken, wenn der Fisch nah vorbeischwimmt), kein Sprung beim Übergang vom Biss in den Drill, Fischeinstellung prallt nicht mehr am Ufer ab, Blickwinkel begrenzt, Wackeln nur noch kurz vor dem Reißen.

## [0.6.0] - 2026-10-12

### Neu
- Anforderungsliste und Roadmap unter `docs/`.
- Neuer Drill als Schnursimulation: Fischkraft nach Größe und Ausdauer, einstellbare Bremse (10 Stufen), Schnurbruch nach Spannung gegen Tragkraft, Spulenkapazität, Hakenhalt, Pumpen (Rute heben, beim Absenken kurbeln), Gegensteuern bei Seitwärtsfluchten, Abrieb an Hindernissen, Schnurdrall beim Kurbeln gegen die Bremse.
- Fischaktionen mit Vorwarnung: Flucht, Abtauchen, Seitwärts, Kopfstöße, Sprünge, Erschöpfung; artspezifisches Verhalten.
- Anschlag-Timing setzt den Haken; aktive Landung per Rechtsklick, wenn der Fisch nah und müde ist.
- Längere Drills: Hecht ~1 min, Wels 2–3 min, Bosse 4–7 min.
- Kamera-Zustandsmaschine: Wurf, Warten am Köder, Biss unter Wasser, Anschlag, Drill mit Einstellungen je Fischaktion, Sprung, Landung, Trophäenansicht. C schaltet sie ab, X überspringt.
- Neue Anzeige über der Hotbar: Spannung, Bremsmarke, Fischkraft, Hakenhalt, Hindernisnähe, Handlungshinweis, Schnur/Bremse/Haken in Zahlen, Bossleiste.
- 13 Rollen mit Spezialfähigkeiten (Stationär, Baitcaster, Multi, Fliege, Echo, Gezeiten, Netherit und vier Bossrollen).
- 25 Köder: Regenwurm, Made, Mais, Brot, Boilie, Köderfisch, Fischfetzen, Insektenlarve, Garnele, Spinner, Blinker, Wobbler, Gummifisch, Popper, Jig, Streamer, Trockenfliege, Nymphe u. a.; Fische ignorieren unpassende Köder, Kunstköder nutzen sich ab.
- Vier Bossfische mit Bedingungen, drei Phasen und Eigenheiten: Der Alte vom Grund, Der Schilfkönig, Der Schwarze Stör, Der Tiefenwächter; Bosstrophäen.
- Karausche und Stör; sechs Seltenheitsstufen; Tageszeit und Regen beeinflussen die Beißlust.
- Training mit beliebiger Art oder Boss: `/atzen practice <art>`.

### Geändert
- Rute, Schnur und Rolle wirken jetzt über physikalische Werte (Hubkraft, Flex, Tragkraft, Dehnung, Abrieb, Einzug, Kraft, Kapazität).

## [0.5.0] - 2026-10-12

### Neu
- Wechselbare Ausrüstung: Jede Atzen-Rute hat Slots für Rolle, Schnur und Köder (auf die Rute ziehen zum Einsetzen/Tauschen, Rechtsklick zum Herausnehmen). Der Tooltip zeigt alle drei Slots und die Gesamtwirkung.
- Neue Rollen (Kupfer, Eisen, Diamant) und Schnüre (Nylon, Geflochten, Carbon); neue Carbonrute. Carbon ist bei Rute und Schnur das Beste. Carbonfaser aus Kohleblöcken im Hochofen.
- 48 statt 6 Fischarten: heimische Süßwasserfische (Raubfische, Friedfische, Salmoniden) und Meeresfische (Dorschartige, Heringsartige, Barschartige, Makrelen/Thun, Plattfische u. a.), mit eigenen Formen und Texturen.
- Lebensräume: Fluss, See, Sumpf, kalte Gewässer, Ozean, warmer, kalter und tiefer Ozean bestimmen, welche Arten spawnen und anbeißen.
- Spannungsleiste über der Hotbar mit grünem Bereich, Ausdauer des Fisches und Hinweis, ob R gehalten oder losgelassen werden soll.

### Geändert
- Neue Kameraführung im Drill: über die Schulter des Anglers auf den Fisch, langsamer Schulterwechsel, rückt bei Fluchten näher, gleitet ruckelfrei und geht nicht durch Wände.
- Plattfische liegen flach mit beiden Augen oben; Schwertfisch mit Schwert; Barteln bei Wels, Barbe, Kabeljau und Schellfisch.

## [0.4.0] - 2026-10-12

### Neu
- Eigene Angel-Sounds, synthetisiert von `scripts/sounds.py` (keine fremden Aufnahmen): Anbiss-Plopp, Anschlag, Warn-Brodeln vor der Flucht, Ratschen der Rolle, knarzende Schnur unter Spannung, schlaffe Schnur, Fischsprung, Schnurriss, Fang-Fanfare, entkommender Fisch und Köder-Einlegen. Mit Untertiteln. Fang, Riss und Entkommen hören jetzt auch die Spieler in der Nähe.

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
