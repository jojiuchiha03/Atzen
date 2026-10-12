# Atzen – Fishing Battles

Fabric-Mod für **Minecraft Java 26.2**: Fische im Wasser beobachten, den Biss anschlagen und den Fang in einem Duell mit Schnurspannung, Fluchten und Ausdauer landen.

**Stand: 0.5.0.** Enthält 48 Süß- und Salzwasserfische mit eigenen animierten 3D-Körperformen und Lebensräumen, vier Angeln, wechselbare Rollen, Schnüre und Köder sowie drei Verzauberungen. Deutsch und Englisch sind enthalten. Build-, Daten- und Logiktests sind eingerichtet. Ein Test in einer laufenden Minecraft-Welt steht noch aus; die EULA wurde für eine Testinstanz nicht bestätigt.

## Installation

Benötigt werden Minecraft **26.2**, Java **25**, Fabric Loader **0.19.5 oder neuer** und eine zu 26.2 passende Fabric API. Gebaut wird gegen **Fabric API 0.161.0+26.2**.

1. Fabric für Minecraft 26.2 installieren.
2. `build/libs/atzen-0.2.0.jar` und die Fabric-API-JAR in den `mods`-Ordner legen.
3. Im Mehrspielerbetrieb Atzen und Fabric API auf **Server und allen Clients** installieren.

Die Datei mit `-sources.jar` im Namen ist Quellcode und gehört nicht in den `mods`-Ordner.

## Angeln und Duelle

Mit einer normalen Angel oder einer Atzen-Rute auswerfen. Beim Biss rechtsklicken, um das Duell zu starten. Atzen-Fische in der Nähe (bis 12 Blöcke) bemerken eine ausgeworfene Pose, schwimmen hin und beißen an: Die Pose taucht ab wie bei einem normalen Biss. Wer jetzt rechtsklickt, kämpft gegen genau diesen Fisch, mit seiner Länge und seinem Gewicht. Mit Köder beißen sie sicher, ohne Köder nur manchmal; zieht man nicht an, schwimmt der Fisch weiter. Auch ein direkt mit dem Haken getroffener Atzen-Fisch kann gefangen werden; dann entsprechen Maße und Beute genau diesem Tier.

| Eingabe | Wirkung |
| --- | --- |
| **R halten** | Einholen: Fisch-Ausdauer sinkt, Schnurspannung steigt |
| **R loslassen** | Nachgeben und Spannung abbauen |
| **X** | Aktuelles Duell abbrechen |

Über der Hotbar zeigt eine schmale **Spannungsleiste**, was zu tun ist: grüner Bereich = R halten und einholen, rot = nachgeben; darunter die Ausdauer des Fisches. Ein kurzer Hinweis (Einholen / Nachgeben / Loslassen / Festhalten / Straffen) blinkt, wenn es eilt. Während des Duells kreist die Kamera langsam um dich und die Pose, der Fisch zappelt sichtbar an der Schnur, springt bei Fluchten aus dem Wasser und wird beim Einholen näher gezogen. Die Spannung hörst du: Je lauter und schneller die Schnur knarzt, desto näher ist sie am Reißen; bei hoher Spannung und während Fluchten wackelt die Kamera. Danach geht es weich zurück in deine eigene Ansicht.

Die Tasten lassen sich unter Steuerung → Atzen: Angeln ändern. Zwischen 30 und 65 % Spannung holst du besonders effizient ein. Bei 100 % reißt die Schnur. Bleibt sie drei Sekunden unter 8 %, entkommt der Fisch. Vor jeder Flucht gibt es eine Warnphase; während der Flucht solltest du nachgeben. Jedes Duell hat maximal 60 Sekunden Zeit.

Größere Exemplare derselben Art ziehen stärker und brauchen länger. Bessere Ausrüstung erleichtert den Fang. Der Server berechnet das Ergebnis; häufigeres Senden von Tasteneingaben beschleunigt das Duell nicht. Tod, Verbindungsabbruch, Rutenwechsel oder zu großer Abstand beenden den Versuch.

Bei normalen Bissen bleiben die Vanilla-Regeln für Schätze, Müll, Glück und offenes Wasser erhalten. Nur Fischbeute wird zu einem detaillierten Atzen-Fang. Ein gewonnenes Duell kann deshalb auch einen Vanilla-Schatz oder Müll liefern. Direkt gehakte Tiere liefern das tatsächlich gehakte Exemplar.

## Fische und 3D-Modelle

48 Arten schwimmen als Tiere im Wasser, jede mit eigener Körperform und Textur; Schwanz und Flossen sind animiert. Plattfische liegen flach mit beiden Augen oben, der Schwertfisch trägt sein Schwert, Welse, Barben und Dorsche haben Barteln. Die sichtbare Größe richtet sich nach der individuellen Länge.

| Lebensraum | Arten |
| --- | --- |
| Flüsse | Bachforelle, Regenbogenforelle, Äsche, Lachs, Meerforelle, Hecht, Zander, Flussbarsch, Aal, Rapfen, Barbe, Rotauge, Döbel |
| Seen | Hecht, Zander, Flussbarsch, Karpfen, Brasse, Rotfeder, Schleie |
| Sümpfe | Wels, Karpfen, Schleie, Aal, Goldkoi |
| Kalte Gewässer | Bachforelle, Äsche, Lachs, Meerforelle |
| Ozean | Kabeljau, Seelachs, Schellfisch, Seehecht, Wittling, Hering, Sprotte, Wolfsbarsch, Makrele, Scholle, Flunder, Seezunge, Steinbutt, Knurrhahn |
| Warmer Ozean | Sardine, Sardelle, Dorade, Roter Schnapper, Rotbarbe, Zackenbarsch, Barracuda, Thunfisch, Bonito, Schwertfisch |
| Kalter Ozean | Alaska-Seelachs, Heilbutt, Rotbarsch, Seewolf, Lachs |
| Tiefer Ozean | Seeteufel, Heilbutt, Thunfisch, Schwertfisch, Rotbarsch |

Seltene Arten (Zander, Aal, Wels, Zackenbarsch, Heilbutt, Seeteufel u. a.) sind lila markiert, legendäre (Schwertfisch, Goldkoi) golden. Die genaue Gewichtung je Lebensraum steht in `FishSpecies.java`.

Fische spawnen in jedem Wasser mit freiem Himmel darüber, auch oberhalb des Meeresspiegels (Seen, Bergflüsse), aber nicht in Höhlen. Welche Arten auftauchen und anbeißen, hängt vom Lebensraum des Bioms ab.

Gewichte folgen einer artspezifischen Länge³-Kurve mit ±12 % Variation. Kleinere Exemplare sind häufiger; die obersten 15 % des Längenbereichs gelten als Trophäenfische. Die Werte sind Spielbalance, keine biologische Vermessungsdatenbank. Bei normalen Angelbissen wird die Art nach dem Lebensraum am Haken gewichtet ausgewählt.

Fänge speichern Art, Länge, Gewicht, Größenklasse, Fangbiom, Koordinaten, Welt-Tag, Angler und die Kontrolle während des Duells. Die Angaben stehen im Tooltip; die Rohdaten liegen in `minecraft:custom_data` unter `atzen_fish`. Individuelle Fänge sind nicht stapelbar.

Lebende Fische lassen sich mit einem Wassereimer aufnehmen und wieder freilassen. Länge, Gewicht und Gesundheit bleiben erhalten. Raubfische, Plattfische und seltene Arten sind Einzelgänger; Heringsartige ziehen in großen Schwärmen. Getötete Fische liefern ebenfalls einen vermessenen Wildfang. Alle Fisch-Items sind essbar und können zu gebratenem Kabeljau gekocht werden; dabei gehen die Fangdaten verloren.

## Angeln, Köder und Verzauberungen

| Angel | Haltbarkeit | Eigenschaft | Reparaturmaterial |
| --- | --- | --- | --- |
| Flussrute | 128 | etwas weniger Spannung, etwas schneller | Kupferbarren |
| Eisenrute | 384 | deutlich weniger Spannung | Eisenbarren |
| Gezeitenrute | 512 | weniger Spannung, schnell | Prismarinscherben |
| **Carbonrute** | 1024 | **am besten:** 34 % weniger Spannung, 20 % schneller, ruhigere Fluchten | Carbonfaser |

Jede Atzen-Rute hat drei **Ausrüstungs-Slots: Rolle, Schnur, Köder.** Teil im Inventar auf die Rute ziehen – ein vorhandenes Teil wird getauscht und landet auf dem Mauszeiger. Rechtsklick auf die Rute mit leerem Mauszeiger nimmt erst den Köder, dann die Schnur, dann die Rolle heraus. Alternativ die Rute auf dem Mauszeiger halten und per Rechtsklick auf ein Teil klicken. Der Tooltip zeigt die drei Slots und die Gesamtwirkung. Ohne geladenen Köder zählt weiterhin ein Köder in der anderen Hand. Pro begonnenem Duell wird ein Köder verbraucht. Im Kreativmodus wird nichts verbraucht.

| Rolle | Wirkung |
| --- | --- |
| Kupferrolle | 8 % schneller einholen |
| Eisenrolle | 15 % schneller, Fluchten 5 % schwächer |
| Diamantrolle | 28 % schneller, Fluchten 12 % schwächer |

| Schnur | Wirkung |
| --- | --- |
| Nylonschnur | 10 % weniger Spannung |
| Geflochtene Schnur | 20 % weniger Spannung, 5 % schneller |
| **Carbonschnur** | **am besten:** 34 % weniger Spannung, Fluchten 15 % schwächer |

| Köder | Wirkung |
| --- | --- |
| Wurmköder | 15 % schneller einholen |
| Leuchtköder | 20 % weniger Spannungsanstieg während Fluchten |
| Kraftköder | 25 % schneller einholen, aber 12 % mehr Spannung |

| Verzauberung | Höchststufe | Wirkung je Stufe |
| --- | --- | --- |
| Schnurmeister | III | 8 % weniger Spannungsanstieg |
| Kurbelkraft | III | 8 % schneller einholen |
| Wellenruhe | II | 15 % weniger Spannungsanstieg während Fluchten |

Die Verzauberungen unterstützen normale und Atzen-Angeln. Sie sind für den Verzauberungstisch registriert. Alternativ das passende Atzen-Zauberbuch in die **Haupthand**, die eingeholte Angel in die **Nebenhand** nehmen und rechtsklicken: Ein Buch erhöht die Verzauberung um eine Stufe. Auf der Höchststufe wird kein Buch verbraucht. Boni verschiedener Ausrüstungsteile werden miteinander multipliziert.

## Rezepte

Alle folgenden Werkbankrezepte sind formlos. Die Rutenrezepte erzeugen eine neue Rute; sie übernehmen keine Verzauberungen, Namen oder anderen Daten der eingesetzten Rute. Baue deshalb zuerst die gewünschte Rute und verzaubere sie danach.

| Ergebnis | Zutaten |
| --- | --- |
| Flussrute | 1 Angel, 2 Kupferbarren, 1 Faden |
| Eisenrute | 1 Flussrute, 3 Eisenbarren, 1 Leder |
| Gezeitenrute | 1 Eisenrute, 3 Prismarinscherben, 1 Herz des Meeres, 1 Diamant |
| Carbonfaser | Kohleblock im Hochofen |
| Carbonrute | Eisenrute, 3 Carbonfaser, 1 Diamant |
| Kupferrolle | 2 Kupferbarren, 1 Eisennugget, 1 Stock |
| Eisenrolle | 2 Eisenbarren, 1 Eisennugget, 1 Redstone |
| Diamantrolle | Eisenrolle, Diamant, Gold, Redstone |
| Nylonschnur | 3 Faden, 1 Schleimball |
| Geflochtene Schnur | Nylonschnur, 3 Faden |
| Carbonschnur | Geflochtene Schnur, 2 Carbonfaser |
| 6 Wurmköder | 1 Verrottetes Fleisch, 1 Weizensamen |
| 4 Leuchtköder | 1 Wurmköder, 1 Leuchttintenbeutel |
| 4 Kraftköder | 1 Roher Lachs, 1 Seetang, 1 Weizen |
| Schnurmeister-Zauberbuch | 1 Buch, 1 Lapislazuli, 1 Faden, 1 Eisenbarren |
| Kurbelkraft-Zauberbuch | 1 Buch, 1 Lapislazuli, 1 Redstone, 1 Amethystscherbe |
| Wellenruhe-Zauberbuch | 1 Buch, 1 Lapislazuli, 1 Prismarinkristalle, 1 Leuchttintenbeutel |

Alle Items stehen sortiert im Kreativtab „Atzen – Angeln“: Angeln, Rollen, Schnüre, Köder, Carbonfaser, Bücher, Fische, Eimer und Spawn-Eier. Die Spawn-Eier erscheinen zusätzlich im Vanilla-Tab Spawn-Eier.

## Befehle

```text
/atzen
/atzen practice
/atzen practice darter
/atzen practice fighter
/atzen practice bruiser
/atzen cancel
```

Das Training simuliert nur ein Duell: keine Beute, kein Köderverbrauch und kein Rutenverschleiß. Es lässt sich nur starten, wenn gerade kein Haken ausgeworfen ist.

## Entwicklung und Prüfungen

Java 25 installieren und `JAVA_HOME` setzen. Unter Windows:

```powershell
.\scripts\build.ps1 build
```

Das Skript verwendet einen vorhandenen lokalen JDK unter `.tools/jdk`, sonst `JAVA_HOME`. Es legt Gradle-Cache und temporäre Dateien im Projekt ab. Alternativ mit Java 25: `./gradlew build` beziehungsweise `gradlew.bat build`.

`build` kompiliert Server- und Clientcode, baut die JAR und führt 14 JUnit-Prüfungen aus. Diese prüfen erfolgreiche und fehlgeschlagene Duelle, Warnphasen, Abbruch, große Trophäenfische, Größen-/Gewichtsgrenzen, Artenverteilung, JSON-Dateien, eigene Modell-/Texturreferenzen und Übersetzungen. Ein Bytecode-Test prüft außerdem die verwendeten Angel-Mixin-Ziele gegen Minecraft 26.2. Es wird keine Welt gestartet. Der HTML-Testbericht liegt unter `build/reports/tests/test/index.html`.

Ressourcen neu erzeugen:

```powershell
node scripts/generate-resources.mjs
.\scripts\import-textures.ps1
```

Die 21 eigenen PNG-Texturen wurden mit dem integrierten ImageGen-Werkzeug erzeugt. Originale liegen unter `art/source`; genaue Prompts und Zielpfade stehen in `art/prompts.json`. Der Import verkleinert auf 64×64 Pixel und erhält bei Item-Sprites Seitenverhältnis und Transparenz. Die sechs blickdichten Fischatlanten enthalten Haut-, Flossen- und Augenbereiche. Vanilla-Ressourcen werden über Minecraft-Verweise genutzt und nicht als Originaldateien mitgeliefert.

Die 3D-Modelle sind direkt in `AtzenFishModel.java` definiert und benötigen keine zusätzliche Animationsbibliothek. Spielbalance: `FishingBattle.java`, `FishSpecies.java` und `BattleEquipment.java`. Git ist eingerichtet; das Remote verweist auf `https://github.com/jojiuchiha03/Atzen`. Zugangsdaten gehören nicht in das Repository.

Lizenz: MIT, siehe `LICENSE`.
