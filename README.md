# Atzen – Fishing Battles

Fabric-Mod für **Minecraft Java 26.2**: eine Angelsimulation mit echtem Drill – Schnurspannung, Bremse, Pumpen, Gegensteuern, Hakenhalt und Landung –, 50 Fischarten mit eigenen Lebensräumen, wechselbarer Ausrüstung, vier Bossfischen und einer Kinokamera.

**Stand: 0.6.0.** Die vollständige Anforderungsliste steht in [docs/ANFORDERUNGEN.md](docs/ANFORDERUNGEN.md), der Umsetzungsplan in [docs/ROADMAP.md](docs/ROADMAP.md). Deutsch und Englisch sind enthalten.

## Installation

Benötigt werden Minecraft **26.2**, Java **25**, Fabric Loader **0.19.5 oder neuer** und Fabric API **0.161.0+26.2**.

1. Fabric für Minecraft 26.2 installieren.
2. `atzen-0.6.0.jar` und die Fabric-API-JAR in den `mods`-Ordner legen.
3. Im Mehrspielerbetrieb Atzen und Fabric API auf **Server und allen Clients** installieren.

## Der Angelvorgang

1. **Ausrüsten:** Rolle, Schnur und Köder im Inventar auf die Rute ziehen (siehe unten).
2. **Auswerfen:** Die Kamera geht kurz über die Schulter und folgt der Pose.
3. **Warten:** Bleibst du ruhig stehen, zeigt die Kamera die Pose vom Ufer aus. Bewegst du dich oder schaust dich um, hast du sofort wieder die normale Sicht.
4. **Biss:** Atzen-Fische in bis zu 12 Blöcken bemerken die Pose, schwimmen hin und sehen sich den Köder an – die Kamera taucht dafür unter Wasser. Mag der Fisch den Köder nicht, ignoriert er ihn; ist er nur halb interessiert, dreht er manchmal wieder ab.
5. **Anschlag:** Rechtsklick, wenn die Pose abtaucht. Der richtige Moment setzt den Haken fester: Raubfische erst etwas drehen lassen, Forellen sofort anschlagen. Die Anzeige meldet „perfekt“, „gut“ oder „ungünstig“.
6. **Drill:** siehe unten.
7. **Landen:** Ist der Fisch nah (unter 4 m) und erschöpft, steht über der Hotbar „Rechtsklick – Fisch landen!“. Ein großer oder noch nicht ganz müder Fisch kann dabei ausschlagen und noch einmal flüchten.
8. **Trophäe:** Nach dem Fang dreht sich die Kamera um dich, eine Karte zeigt Art, Seltenheit, Länge, Gewicht und deine Kontrolle im Drill.

## Der Drill

Der Server simuliert die Schnur: Der Fisch zieht mit einer Kraft, die von Größe, Ausdauer und seiner aktuellen Aktion abhängt. Die Spannung auf der Schnur kann diese Kraft nie dauerhaft über die **Bremse** hinaus heben – dort rutscht die Rolle und der Fisch nimmt Schnur. Die Schnur **reißt**, wenn die Spannung ihre Tragkraft übersteigt: durch eine Bremse, die fester als die Schnur eingestellt ist, durch Rucke bei steifer Ausrüstung oder durch eine an Hindernissen aufgescheuerte Schnur. Geht die Schnur auf der Spule aus, ist der Fisch weg.

| Taste (einstellbar) | Wirkung |
| --- | --- |
| **R** halten | Einholen – nur gegen einen Fisch, der schwächer zieht als die Rolle. Gegen eine rutschende Bremse zu kurbeln verdrallt die Schnur. |
| **G** halten | Rute heben (Pumpen): zieht den Fisch heran. Danach loslassen und **beim Absenken R** – so kommt die gewonnene Schnur doppelt so schnell herein. Absenken ohne Kurbeln lässt die Schnur durchhängen. |
| **Pfeil links/rechts** | Gegensteuern, wenn der Fisch seitwärts zu Hindernissen zieht. |
| **Pfeil hoch/runter** | Bremse fester/lockerer (10 Stufen). |
| **Rechtsklick** | Fisch landen, wenn er nah und erschöpft ist. |
| **X** | Drill abbrechen bzw. Kameraszene überspringen. |
| **C** | Kinokamera an/aus. |

Der Fisch wechselt zwischen **Dagegenhalten**, **Flucht**, **Abtauchen**, **Seitwärts zu Hindernissen**, **Kopfstößen**, **Sprüngen** und **Erschöpfung**. Jede Aktion außer Dagegenhalten und Erschöpfung kündigt er vorher an: er schlägt an der Oberfläche, die Anzeige warnt, ein Brodeln ist zu hören.

- **Flucht/Abtauchen:** nicht kurbeln, die Bremse arbeiten lassen; eine haltende Bremse ermüdet den Fisch schneller, ist aber riskanter.
- **Seitwärts:** in die Gegenrichtung steuern, sonst scheuert die Schnur an Hindernissen (geflochtene Schnur besonders).
- **Kopfstöße:** Spannung mittel halten – zu straff oder zu locker reißt den Haken aus.
- **Sprung:** Rute unten lassen und leicht kurbeln. Gehobene Rute oder harte Spannung schütteln den Haken ab.
- **Schlaffe Schnur** kostet Hakenhalt. Fällt der Hakenhalt auf 0, ist der Fisch weg.

**Anzeige über der Hotbar:** Die Leiste zeigt die Spannung als Anteil der Tragkraft (rot ab 85 %), der türkise Strich die Bremse, darunter die Kraft des Fisches und den Hakenhalt. Eine Zeile sagt, was gerade zu tun ist; darunter Schnur draußen/Spulenkapazität, Bremsstufe und Hakenhalt. Bei Seitwärtsfluchten zeigt ein oranger Strich, wie nah das Hindernis ist. Bosse zeigen oben Namen, Phase und Kraft.

Typische Drilldauer mit passender Ausrüstung: Rotauge ~15 s, Barsch ~20 s, Hecht 45–60 s, Karpfen 60–90 s, Wels 2–3 min, Bosse 4–7 min. Wer nur R gedrückt hält, fängt kleine Fische, verliert große aber meist.

Der Server berechnet alles; häufigeres Senden von Eingaben beschleunigt nichts. Tod, Verbindungsabbruch, Rutenwechsel oder zu großer Abstand beenden den Versuch. Bei Vanilla-Bissen bleiben Schätze, Müll und Glück erhalten.

## Kamera

Eine Zustandsmaschine für den ganzen Wurf: **Wurf** (Schulter, folgt der Pose), **Warten** (vom Ufer auf die Pose, nur solange du still stehst), **Biss** (unter Wasser neben der Pose), **Anschlag**, **Drill** (über die Schulter, wenn der Fisch hält; beim Fisch, wenn er flüchtet oder abtaucht; weit und hoch bei Seitwärtsfluchten; dicht hinter dir bei Sprüngen; wackelt kurz vorm Reißen), **Landung** (tief am Wasser) und **Trophäe**. Die Kamera trifft keine Entscheidungen; **C** schaltet sie ab, **X** überspringt Szenen.

## Ausrüstung

Jede Atzen-Rute hat drei Slots: **Rolle, Schnur, Köder**. Teil im Inventar auf die Rute ziehen – ein vorhandenes Teil wird getauscht. Rechtsklick auf die Rute mit leerem Mauszeiger nimmt erst den Köder, dann die Schnur, dann die Rolle heraus. Der Tooltip zeigt die Slots, Tragkraft, Schnurmenge, Einzug, Kraft und die Fähigkeit der Rolle. Ohne Schnur angelt die Rute mit einfachem Faden (3,5 kg).

| Rute | Hubkraft | Flex | Besonderheit |
| --- | --- | --- | --- |
| Angel (Vanilla) | 3 kg | mittel | – |
| Flussrute | 4 kg | sehr weich | verzeiht Rucke, +5 % Einzug |
| Eisenrute | 7 kg | steif | pumpt hart, gibt Rucke weiter |
| Gezeitenrute | 6 kg | mittel | +15 % Einzug |
| **Carbonrute** | **9 kg** | weich | die beste: kräftig und geschmeidig, +10 % Einzug |

| Schnur | Tragkraft | Dehnung | Abrieb |
| --- | --- | --- | --- |
| Faden (ohne Schnur) | 3,5 kg | mittel | schwach |
| Monofile Schnur | 7 kg | hoch – schluckt Rucke | normal |
| Geflochtene Schnur | 14 kg | kaum – direkt | scheuert schnell |
| **Carbonschnur** | **20 kg** | etwas | sehr fest – die beste |

| Rolle | Einzug | Kraft | Schnur | Fähigkeit |
| --- | --- | --- | --- | --- |
| Kupfer-Stationärrolle | 2,2 m/s | 5 kg | 120 m | Verzeihend: schlaffe Schnur kostet weniger Hakenhalt |
| Eisen-Stationärrolle | 2,6 m/s | 7 kg | 160 m | Verzeihend |
| Diamant-Stationärrolle | 3,0 m/s | 9 kg | 200 m | Feinbremse: Rucke kommen viel schwächer an |
| Baitcaster | 4,0 m/s | 8 kg | 110 m | sehr schnell; Flucht gegen lockere Bremse (≤ 3) gibt eine Perücke |
| Multirolle | 2,0 m/s | 15 kg | 350 m | Kraftübersetzung für Großfische |
| Fliegenrolle | 1,6 m/s | 4 kg | 90 m | Pumpen +60 %, Fische unter 3 kg schwächer; für Fliegen nötig |
| Echorolle | 2,6 m/s | 8 kg | 180 m | kündigt jede Aktion eine Sekunde früher an |
| Gezeitenrolle | 2,8 m/s | 9 kg | 220 m | Meeresfische erholen sich nicht |
| Netheritrolle | 2,4 m/s | 14 kg | 300 m | Bremse hält nie mehr als 90 % der Tragkraft |
| Rolle des Alten | 2,4 m/s | 13 kg | 320 m | Bossbeute: Fluchten nehmen 35 % weniger Schnur |
| Schilfkrone | 3,4 m/s | 9 kg | 180 m | Bossbeute: Sprünge reißen nie aus, solange die Rute unten ist |
| Strömungsrolle | 2,6 m/s | 12 kg | 280 m | Bossbeute: holt in Ruhephasen selbst Schnur ein |
| Tiefenherz | 3,2 m/s | 12 kg | 300 m | Bossbeute: Fisch ermüdet 25 % schneller, warnt früher |

### Köder

Jede Art hat eine Ernährungsweise (Friedfisch, Raubfisch, Salmonide, Meeresräuber, Meeresgrund, Schwarmfisch); der Köder entscheidet, wer sich nähert und wer anbeißt. Ein unpassender Köder wird ignoriert. Große Köder (Boilie, Köderfisch, Wobbler) bringen größere Fische. **Naturköder** werden pro Drill verbraucht, **Kunstköder** nutzen sich ab (ein abgenutzter läuft schlechter) und gehen bei Schnurbruch verloren.

| Köder | Für |
| --- | --- |
| Regenwurm, Made | fast alle Friedfische, Forellen, Aal, Grundfische |
| Mais, Brotflocke, Boilie | Friedfische (Karpfen/Karausche, Döbel/Rotfeder); Boilie selektiv auf große |
| Köderfisch, Fischfetzen | Raubfische und Meeresräuber; Wels und Hecht besonders |
| Insektenlarve | Forellen, Äschen, Friedfische |
| Garnele | Meeresgrund- und Raubfische |
| Leuchtköder | alle, nachts besonders; Fluchten 20 % schwächer |
| Kraftköder | fast alle; Fisch ermüdet schneller, aber mehr Spannung |
| Spinner, Blinker, Wobbler | Raubfische (Barsch und Rapfen lieben Spinner) |
| Gummifisch | Raubfische, Zander besonders; verschleißt schnell |
| Popper | Meeresräuber an der Oberfläche; gehakte Fische springen mehr |
| Jig | Meeresgrund, Kabeljau und Seelachs |
| Streamer, Trockenfliege, Nymphe | Forellen und Äschen – mit Fliegenrolle |

Die Tageszeit zählt: Raubfische beißen in der Dämmerung, Wels, Aal und Zander nachts, Friedfische tagsüber, Forellen in der Dämmerung. Regen belebt Süßwasser.

## Fische

50 Arten schwimmen als Tiere im Wasser, jede mit eigener Körperform, Textur und Drillverhalten (Springer, Grundflüchter, Hindernissucher, Kopfschüttler). Die sichtbare Größe richtet sich nach der Länge. Seltenheit in sechs Stufen: Gewöhnlich, Ungewöhnlich, Selten, Episch, Legendär, Mythisch.

| Lebensraum | Arten |
| --- | --- |
| Flüsse | Bachforelle, Regenbogenforelle, Äsche, Lachs, Meerforelle, Hecht, Zander, Flussbarsch, Aal, Rapfen, Barbe, Rotauge, Döbel, Stör |
| Seen | Hecht, Zander, Flussbarsch, Karpfen, Karausche, Brasse, Rotfeder, Schleie |
| Sümpfe | Wels, Karpfen, Karausche, Schleie, Aal, Goldkoi |
| Kalte Gewässer | Bachforelle, Äsche, Lachs, Meerforelle, Stör |
| Ozean | Kabeljau, Seelachs, Schellfisch, Seehecht, Wittling, Hering, Sprotte, Wolfsbarsch, Makrele, Scholle, Flunder, Seezunge, Steinbutt, Knurrhahn |
| Warmer Ozean | Sardine, Sardelle, Dorade, Roter Schnapper, Rotbarbe, Zackenbarsch, Barrakuda, Thunfisch, Bonito, Schwertfisch |
| Kalter Ozean | Alaska-Seelachs, Heilbutt, Rotbarsch, Seewolf, Lachs |
| Tiefer Ozean | Seeteufel, Heilbutt, Thunfisch, Schwertfisch, Rotbarsch |

Größere Exemplare ziehen stärker und halten länger durch (ein 1-kg-Barsch zieht etwa 1,5 kg, ein 12-kg-Karpfen etwa 7 kg, ein 50-kg-Wels etwa 15 kg). Fänge speichern Art, Länge, Gewicht, Größenklasse, Fangort, Welt-Tag, Angler und die Kontrolle im Drill; individuelle Fänge sind nicht stapelbar. Lebende Fische lassen sich im Eimer tragen.

## Bossfische

Jeder Boss braucht seinen eigenen Bossköder, sein Gewässer und seine Zeit. Auch dann ist nur etwa jeder dritte Biss der Boss, und jeder Angler trifft jeden Boss einmal pro Tag. Bosse haben drei Phasen mit eigenem Verhalten und eine Eigenheit; ihr Fang bringt eine Trophäe, aus der die Bossrolle gebaut wird.

| Boss | Wo und wann | Köder | Eigenheit | Beute |
| --- | --- | --- | --- | --- |
| **Der Alte vom Grund** (Wels) | nachts, See oder Sumpf, ≥ 5 Blöcke tief | Uralter Grundköder | flüchtet ins Totholz, täuscht kurz vor der Landung Erschöpfung vor und explodiert | Uralte Welsbarte → Rolle des Alten |
| **Der Schilfkönig** (Hecht) | Sumpf bei Regen oder in der Dämmerung | Königswobbler | Jagd ins Schilf, dann ein Tanz aus Sprung auf Sprung | Schilfkrone → Schilfkronen-Rolle |
| **Der Schwarze Stör** | tagsüber, Fluss, ≥ 4 Blöcke tief | Strömungsköder | die Strömung zieht immer mit; lange Ruhe, dann Ausbrüche | Schwarze Knochenplatte → Strömungsrolle |
| **Der Tiefenwächter** (mythisch) | verborgenes Wasser unter y 0, ohne Himmel | Tiefenlicht | lernt: wer ständig pumpt, bekommt Kopfstöße, wer nur kurbelt, Fluchten | Herz der Tiefe → Tiefenherz |

Bosse sind nur mit starker Ausrüstung zu schaffen (Carbonschnur, kräftige Rolle).

## Rezepte

Alle Werkbankrezepte sind formlos.

| Ergebnis | Zutaten |
| --- | --- |
| Flussrute | Angel, 2 Kupferbarren, Faden |
| Eisenrute | Flussrute, 3 Eisenbarren, Leder |
| Gezeitenrute | Eisenrute, 3 Prismarinscherben, Herz des Meeres, Diamant |
| Carbonrute | Eisenrute, 3 Kohlefaser, Diamant |
| Kohlefaser | Kohleblock im Hochofen |
| Kupfer-Stationärrolle | 2 Kupferbarren, Eisennugget, Stock |
| Eisen-Stationärrolle | 2 Eisenbarren, Eisennugget, Redstone |
| Diamant-Stationärrolle | Eisenrolle, Diamant, Goldbarren, Redstone |
| Baitcaster | 2 Eisenbarren, Goldbarren, Redstone, Quarz |
| Multirolle | Eisenrolle, 2 Eisenbarren, Kupferblock |
| Fliegenrolle | Eisenbarren, Leder, Faden, Stock |
| Echorolle | Eisenrolle, Echosplitter, Amethystscherbe, Redstone |
| Gezeitenrolle | Diamantrolle, Herz des Meeres, 2 Prismarinscherben |
| Netheritrolle | Diamantrolle, Netheritbarren |
| Rolle des Alten / Schilfkrone / Strömungsrolle / Tiefenherz | Multirolle / Baitcaster / Multirolle / Echorolle + Bosstrophäe + Gold / Smaragd / Prismarinkristalle / Diamant |
| Monofile Schnur | 3 Faden, Schleimball |
| Geflochtene Schnur | Monofile Schnur, 3 Faden |
| Carbonschnur | Geflochtene Schnur, 2 Kohlefaser |
| 6 Regenwurm | Verrottetes Fleisch, Weizensamen |
| 8 Made | 2 Verrottetes Fleisch |
| 8 Mais | Weizen, Gelber Farbstoff |
| 8 Brotflocke | Brot |
| 6 Boilie | Ei, Weizen, Zucker, Knochenmehl |
| 2 Köderfisch | Roher Kabeljau |
| 4 Fischfetzen | Roher Lachs |
| 6 Insektenlarve | Spinnenauge, Weizensamen |
| 4 Garnele | Seetang, Roher Kabeljau, Knochenmehl |
| 4 Leuchtköder | Regenwurm, Leuchttintenbeutel |
| 4 Kraftköder | Roher Lachs, Seetang, Weizen |
| Spinner | 2 Eisennuggets, Kupferbarren, Faden |
| Blinker | Eisenbarren, Goldnugget |
| Wobbler | Eichenbretter, Roter Farbstoff, 2 Eisennuggets |
| Gummifisch | Schleimball, Hellgrüner Farbstoff, Eisennugget |
| Popper | Eichenbretter, Weißer Farbstoff, Eisennugget, Feder |
| Jig | Eisenbarren, Faden, Schleimball |
| Streamer | Feder, Faden, Eisennugget, Grüner Farbstoff |
| Trockenfliege | Feder, Faden, Eisennugget |
| Nymphe | Feder, Faden, Eisennugget, Goldnugget |
| 2 Uralter Grundköder | Rohes Rindfleisch, Spinnenauge, Schlamm, Leuchttintenbeutel |
| Königswobbler | Wobbler, Smaragd, Goldbarren, Feder |
| 2 Strömungsköder | Boilie, Prismarinkristalle, Ton, Knochenmehl |
| 2 Tiefenlicht | Leuchtköder, Echosplitter, Sculk |
| Zauberbücher | Buch, Lapislazuli + Faden & Eisen (Schnurmeister) / Redstone & Amethyst (Kurbelkraft) / Prismarinkristalle & Leuchttinte (Wellenruhe) |

Verzauberungen: **Schnurmeister** +8 % Tragkraft je Stufe, **Kurbelkraft** +8 % Einzug und Kraft, **Wellenruhe** Fluchten −12 %. Zauberbuch in die Haupthand, eingeholte Angel in die Nebenhand, rechtsklicken.

Alle Items stehen sortiert im Kreativtab „Atzen – Angeln“.

## Befehle

```text
/atzen                          Hilfe
/atzen practice [Art|Boss]      Training ohne Beute mit der Rute in der Hand
                                (roach, river_trout, pike, carp, catfish, tuna,
                                 ancient_catfish, reed_king, black_sturgeon, deep_warden)
/atzen cancel                   Drill abbrechen
```

## Entwicklung und Prüfungen

`./gradlew build` mit JDK 25 kompiliert Server- und Clientcode, baut die JAR und führt die JUnit-Prüfungen aus. Sie spielen den Drill ohne Minecraft mit einem Test-Angler durch: jede gewöhnliche Art mit passender Ausrüstung fangbar, große Fische zerreißen schwache Ausrüstung, Dauer-Kurbeln reicht bei großen Fischen nicht, jeder Boss ist mit bester Ausrüstung schaffbar und mit einfacher nicht, alle Aktionen werden angekündigt, eine zu feste Bremse reißt die Schnur. Dazu Köderauswahl, Bossbedingungen, Maße, JSON-Dateien, Texturen, Übersetzungen und die Mixin-Ziele gegen Minecraft 26.2.

Ressourcen neu erzeugen: `node scripts/generate-resources.mjs` (Modelle, Rezepte, Übersetzungen), `python3 scripts/draw-textures.py` (alle Texturen, 64×64), `python3 scripts/sounds.py OUT` (Klänge).

Spielbalance: `FishingBattle.java` (Drill), `Boss.java`, `FishSpecimen.fighter()` (Fischkraft), `ReelType`/`LineType`/`RodType`/`BaitType` (Ausrüstung).

Lizenz: MIT, siehe `LICENSE`.
