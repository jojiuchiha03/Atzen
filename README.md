# Atzen – Fishing Battles

Fabric-Mod für **Minecraft Java 26.2**: ein Angelspiel mit echtem Drill, aber einfacher Bedienung – **R** holt ein, **F** gibt Schnur –, 64 Fischarten mit eigenen Lebensräumen (auch in Höhlen, in Lava und im End), Angelkisten, drei Ruten, drei Rollen, einer Schnur, drei Ködern und vier Bossfischen.

**Stand: 0.15.0.** Die vollständige Anforderungsliste steht in [docs/ANFORDERUNGEN.md](docs/ANFORDERUNGEN.md), der Umsetzungsplan in [docs/ROADMAP.md](docs/ROADMAP.md). Deutsch und Englisch sind enthalten.

## Installation

Benötigt werden Minecraft **26.2**, Java **25**, Fabric Loader **0.19.5 oder neuer** und Fabric API **0.161.0+26.2**.

1. Fabric für Minecraft 26.2 installieren.
2. `atzen-0.15.0.jar` und die Fabric-API-JAR in den `mods`-Ordner legen.
3. Im Mehrspielerbetrieb Atzen und Fabric API auf **Server und allen Clients** installieren.

## Der Angelvorgang

1. **Ausrüsten:** Rolle, Schnur und Köder im Inventar auf die Rute ziehen (siehe unten).
2. **Auswerfen** und **warten**. Die Sicht bleibt die ganze Zeit deine eigene.
3. **Biss:** Atzen-Fische in bis zu 12 Blöcken bemerken die Pose, schwimmen hin und sehen sich den Köder an. Mag der Fisch den Köder nicht, ignoriert er ihn.
4. **Anschlag:** Rechtsklick, wenn die Pose abtaucht. Der richtige Moment setzt den Haken fester; die Anzeige meldet „perfekt“, „gut“ oder „zu spät“.
5. **Drill:** siehe unten.
6. **Landen:** Ist der Fisch nah (unter 4 m) und müde, Rechtsklick. Fische bis 2 kg hebst du per Hand heraus, größere brauchen einen **Kescher in der anderen Hand** (Rechtsklick, solange die Marke im grünen Feld ist). Bosse brauchen den Großfischkescher.
7. **Fangkarte:** Oben erscheint eine Karte mit Art, Seltenheit, Länge, Gewicht und deiner Kontrolle im Drill.

## Der Drill

Zwei Tasten, beide einstellbar:

| Taste | Wirkung |
| --- | --- |
| **R** halten | Einholen |
| **F** halten | Schnur geben (öffnet die Bremse). Während eines Drills tauscht F nicht die Hände. |

Die Bremse stellt sich selbst ein. Die Regeln sind einfach:

- **Der Fisch hält still oder ist müde:** R halten. Ohne Einholen hängt die Schnur durch, und der Haken fällt heraus.
- **Er kündigt etwas an** (er schlägt an der Oberfläche, ein Brodeln ist zu hören, die Anzeige blinkt „Achtung“) **oder zieht ab:** loslassen, nicht kurbeln. Wer gegen eine rutschende Bremse kurbelt, verdrallt die Schnur, bis sie reißt.
- **Er springt oder schüttelt den Kopf:** F halten. Eine straffe Schnur reißt beim Sprung den Haken heraus.
- **Die Schnur ist kurz vorm Reißen** (Leiste rot): F.

**Anzeige über der Hotbar:** der Fisch mit Name und Entfernung, die Schnurspannung (grün bis zur Bremse, rot darüber) und die Kraft des Fisches. Darüber steht immer genau eine Anweisung mit der passenden Taste. Bosse zeigen oben Namen, Phase und Kraft.

Kleine Fische landest du schon mit R allein, große nur, wenn du im richtigen Moment loslässt und F drückst. Typische Drilldauer: Rotauge ~15 s, Barsch ~25 s, Hecht ~1 min, Karpfen 1–1,5 min, Wels ~2 min, Bosse 3–7 min. `/atzen cancel` bricht ab.

## Ausrüstung

Jede Atzen-Rute hat drei Plätze: **Rolle, Schnur, Köder**. Teil im Inventar auf die Rute ziehen – ein vorhandenes wird getauscht; Rechtsklick auf die Rute mit leerem Mauszeiger nimmt die Teile wieder heraus. Der Tooltip zeigt die drei Plätze nebeneinander, was die Rute hält, wie schnell sie einholt und wie oft sie verstärkt ist. Ohne Schnur angelt die Rute mit Faden (3,5 kg).

| Rute | Hält | Besonderheit |
| --- | --- | --- |
| Angel (Vanilla) | 5 kg | – |
| **Bambusrute** | 7 kg | leicht und feinfühlig, für den Anfang |
| **Carbonrute** | 24 kg | wirft am weitesten, stark genug für die Bosse |
| **Netheritrute** | 30 kg | die stärkste und die einzige, die in Lava angelt |

| Rolle | Einzug | Kraft | Schnur |
| --- | --- | --- | --- |
| **Kupferrolle** | 2,2 m/s | 5 kg | 120 m |
| **Eisenrolle** | 2,6 m/s | 8 kg | 180 m |
| **Diamantrolle** | 3,2 m/s | 12 kg | 300 m |

**Angelschnur:** trägt 20 kg.

| Köder | Für |
| --- | --- |
| **Regenwurm** | alle friedlichen Fische: Karpfen, Forellen, Grundfische, Schwärme |
| **Köderfisch** | Raubfische in Süß- und Salzwasser, eher große |
| **Leuchtköder** | alle Fische, nachts besonders; Fluchten ziehen schwächer |

Jeder Biss verbraucht einen Köder. Dazu kommen vier Bossköder (siehe unten). Die Tageszeit zählt: Raubfische beißen in der Dämmerung, Wels, Aal und Zander nachts, Friedfische tagsüber. Regen belebt Süßwasser.

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
| Höhlenwasser | Höhlensalmler, Grottenolm, Amethystbarsch, Tropfsteinaal, Leuchtkarpfen |
| Lava | Glutkarpfen, Magmaaal, Lohenhecht, Basaltwels, Obsidianstör |
| Wasser im End | Chorusfisch, Enderaal, Leererochen, Shulker-Kugelfisch |

### Höhlen, Lava und End

Wie in Tide gibt es Fische abseits der Oberfläche. Sie schwimmen nicht frei herum, sie beißen nur an der Pose; Tageszeit und Regen spielen dort keine Rolle, und es kommt kein Vanilla-Müll mit hoch.

- **Höhlenwasser:** jedes Wasser unter y 50, über dem kein Himmel zu sehen ist.
- **Lava:** in jeder Lava, im Nether und in der Oberwelt – aber nur mit der **Netheritrute**. Mit ihr schwimmt die Pose auf Lava wie auf Wasser und verbrennt nicht. Lavafische brennen auch selbst nicht.
- **End:** jedes Wasser im End (dort muss man es selbst hinbringen).

Der Tiefenwächter bleibt ein Boss des verborgenen Wassers unter y 0.

### Angelkisten und Fundzettel

Etwa jeder sechzehnte Biss ist kein Fisch, sondern eine **Angelkiste** (Glück des Meeres macht sie häufiger); sie kommt ohne Drill hoch. Rechtsklick öffnet sie: zwei bis vier Fundstücke aus genau diesem Gewässer (Amethyst und Leuchtbeeren aus Höhlen, Magmacreme und Quarz aus Lava, Chorusfrüchte und Enderperlen aus dem End, Prismarin und Nautilusschalen aus dem Meer, Ton und Seerosen aus Süßwasser), dazu Köder oder Haken, manchmal Smaragde, Perlmutt, Netheritschrott oder eine Shulkerschale.

Oft liegt ein **Fundzettel** darin. Er verrät über eine Fischart – seltene häufiger –, wo sie lebt, wann sie beißt, welchen Köder sie am liebsten nimmt und ob sie eine Netheritrute braucht. Das **Fischlexikon** zeigt außerdem bei jeder Art, auch bei noch unbekannten, in welchen Gewässern sie lebt.

Größere Exemplare ziehen stärker und halten länger durch (ein 1-kg-Barsch zieht etwa 1,5 kg, ein 12-kg-Karpfen etwa 7 kg, ein 50-kg-Wels etwa 15 kg). Fänge speichern Art, Länge, Gewicht, Größenklasse, Fangort, Welt-Tag, Angler und die Kontrolle im Drill; individuelle Fänge sind nicht stapelbar. Lebende Fische lassen sich im Eimer tragen.

## Bossfische

Jeder Boss braucht seinen eigenen Bossköder, sein Gewässer und seine Zeit. Auch dann ist nur etwa jeder dritte Biss der Boss, und jeder Angler trifft jeden Boss einmal pro Tag. Bosse haben drei Phasen mit eigenem Verhalten und eine Eigenheit; ihr Fang bringt eine Trophäe.

| Boss | Wo und wann | Köder | Eigenheit | Beute |
| --- | --- | --- | --- | --- |
| **Der Alte vom Grund** (Wels) | nachts, See oder Sumpf, ≥ 5 Blöcke tief | Uralter Grundköder | flüchtet ins Totholz, täuscht kurz vor der Landung Erschöpfung vor und explodiert | Uralte Welsbarte |
| **Der Schilfkönig** (Hecht) | Sumpf bei Regen oder in der Dämmerung | Königswobbler | Jagd ins Schilf, dann ein Tanz aus Sprung auf Sprung | Schilfkrone |
| **Der Schwarze Stör** | tagsüber, Fluss, ≥ 4 Blöcke tief | Strömungsköder | die Strömung zieht immer mit; lange Ruhe, dann Ausbrüche | Schwarze Knochenplatte |
| **Der Tiefenwächter** (mythisch) | verborgenes Wasser unter y 0, ohne Himmel | Tiefenlicht | lernt: wer ständig Schnur gibt, bekommt Kopfstöße, wer nur kurbelt, Fluchten | Herz der Tiefe |

Bosse sind nur mit starker Ausrüstung zu schaffen (Carbon- oder Netheritrute, Diamantrolle, Angelschnur).

## Kescher und Zurücksetzen

| Kescher | Passt für | Haltbarkeit |
| --- | --- | --- |
| Kleiner Kescher | bis 60 cm, 3 kg | 96 |
| Karpfenkescher | bis 120 cm, 25 kg | 256 |
| Großfischkescher | bis 3,2 m, alle Bosse | 512 |

Je kleiner der Fisch im Verhältnis zum Netz, desto breiter das grüne Feld. Jeder gefangene Fisch ist ein **Individuum** mit eigener Kennung, Maßen und **Zustand** (0–100 %): lange Drills und das Herausheben per Hand stressen ihn. Fisch in der Hand über Wasser benutzen setzt ihn als genau diesen Fisch zurück – das bringt Erfahrung, zählt für Rücksetz-Aufträge und senkt den Fangdruck.

## Setzkescher und Unterwasser-Netz

**Setzkescher** ins Wasser stellen (der Block muss im Wasser stehen): bis 8 Fische, 40 kg, je höchstens 120 cm. Rechtsklick mit Fisch legt ihn hinein, leere Hand nimmt den letzten heraus, Schleichen lässt alle frei. Jede Minute prüft er das Wasser: **Volumen, Sauerstoff (Strömung, Pflanzen), Temperatur (passt das Gewässer zur Art?), Besatzdichte, Wasserqualität** (sinkt mit der Last, erholt sich in Strömung und Regen). Schlechtes Wasser stresst zuerst und wird angezeigt („zu wenig Sauerstoff“ …), erst Vernachlässigung tötet. Ein Fisch kann nie doppelt im Setzkescher liegen.

Das **Unterwasser-Netz** unter Wasser setzen: hineingesetzte Fische leben als echte Fische weiter, im Umkreis des Netzes. Platz gibt es für einen Fisch je 24 Wasserblöcke ringsum. Füttern mit Köder oder Futtermischung; satte, gesunde Paare derselben Art (mindestens einen Tag alt) bekommen alle zehn Minuten höchstens ein Junges. Leere Hand zeigt Zustand und Probleme, Schleichen öffnet das Netz.

## Rutenhalter

| Halter | Ruten | Bissanzeige |
| --- | --- | --- |
| Rutenhalter | 1 | Klicken + Spritzer an der Stelle |
| Doppelständer | 2 | wie oben |
| Rod Pod | 3 | elektronischer Alarm (laut), große Fische ziehen keine Rute hinein |

Rute auf den Halter klicken: sie wirft 6–12 Blöcke vor den Halter aus (Wurfweite der Rute zählt). Die Wartezeit hängt von Köder, Fangdruck, Futterplatz, Köder-Verzauberung und Posenmodul ab. Beißt einer, hast du 8 Sekunden (mit Bisssensor 12): Halter mit leerer Hand anklicken nimmt die Rute auf und der Drill beginnt ohne Pose – Rechtsklick mit der Rute landet. Verpasste Bisse kosten den Köder, manchmal nimmt der Fisch Schnur und Kunstköder mit, und ein großer Fisch kann eine Rute aus einem einfachen Halter ins Wasser ziehen. Liegen zwei Köder näher als 2 Blöcke beieinander, verheddern sich die Schnüre.

## Fangdruck und Verhalten der Fische

Jedes Chunk merkt sich den **Fangdruck**: jeder entnommene Fisch erhöht ihn, zurückgesetzte Fische senken ihn um die Hälfte, mit der Zeit erholt sich das Wasser (etwa 2 Spieltage von „leergefischt“ bis „unberührt“). Bei hohem Druck beißen seltene Fische weniger, die Fische sind kleiner und es wachsen langsamer neue nach. Das **Echolot** zeigt Tiefe, Fangdruck und die Fische ringsum.

Fische haben Alter, Hunger und Scheu. Raubfische jagen kleinere Fische (die fliehen vor ihnen), Schwärme fressen an Pflanzen und Futterplätzen, ziehen tagsüber im Meer, laichen nachts bei Vollmond, Raubfische halten Reviere, scheue Fische meiden Spieler, jede Art hält ihre bevorzugte Tiefe. Weit weg von Spielern ruht dieses Verhalten.

## Fortschritt, Markt und Werkstatt

Jeder Fang bringt **Angler-Erfahrung** nach Seltenheit, Größe und Kontrolle im Drill; dieselbe Art am selben Tag bringt jedes Mal 20 % weniger. Stufe = √(Erfahrung / 40). Die Angelgebiete:

| Gebiet | Gewässer | seltene Fische ab Stufe |
| --- | --- | --- |
| Heimatsee | Seen | 0 |
| Waldfluss | Flüsse, kalte Gewässer | 3 |
| Nebelmoor | Sümpfe | 5 |
| Küste | Meer, warmes Meer | 7 |
| Höhlensystem | Höhlenwasser | 4 |
| Lavaseen | Lava (Netheritrute) | 8 |
| Tiefenreservat | Nordmeer, Tiefsee | 10 |
| Endgewässer | Wasser im End | 12 |

Vor der Stufe beißen dort nur gewöhnliche und ungewöhnliche Fische. Täglich gibt es drei, wöchentlich zwei **Aufträge** (Art, Gewicht, Gewässer, Seltenheit, Zurücksetzen) – für alle Spieler gleich, Belohnung Smaragde und Erfahrung. Das **Fischlexikon** zeigt entdeckte Arten mit Anzahl und größtem Fang, Aufträge, Gebiete, die Serverrekorde je Art, die Rangliste der zehn schwersten Fänge und die eigene Statistik. Epische und bessere Fänge bringen manchmal **Perlmutt**.

**Fischmarkt:** Rechtsklick mit Fisch verkauft ihn, Schleichen verkauft alle. Preis nach Seltenheit, Gewicht, Kontrolle und Frische (Zustand); Wechselgeld bleibt als Guthaben.

**Angelwerkstatt:** Rute in die Haupthand, in die andere Hand Smaragd (repariert ein Viertel) oder Perlmutt (verstärkt, bis 3×, je +6 % Last und Kraft).

## Hilfsmittel

**Futtermischung** (aufs Wasser werfen: 5 Minuten Futterplatz, füttert auch Fische im Netz), **Echolot**, **Köderbox** (packt alle Köder ein, legt leere Köderplätze automatisch nach). Vanilla **Köder** verkürzt die Wartezeit am Rutenhalter, **Glück des Meeres** macht seltene Fische und Angelkisten wahrscheinlicher. Aus dem Boot angeln funktioniert wie vom Ufer.

## Rezepte

Alle Werkbankrezepte sind formlos.

| Ergebnis | Zutaten |
| --- | --- |
| Bambusrute | 3 Bambus, 2 Faden |
| Carbonrute | Bambusrute, 3 Kohlefaser, Diamant |
| Netheritrute | Carbonrute, Netheritbarren, Lohenrute |
| Kohlefaser | Kohleblock im Hochofen |
| Kupferrolle | 2 Kupferbarren, Eisennugget, Stock |
| Eisenrolle | Kupferrolle, 2 Eisenbarren, Redstone |
| Diamantrolle | Eisenrolle, Diamant, Goldbarren, Redstone |
| Angelschnur | 3 Faden, Schleimball |
| 6 Regenwurm | Verrottetes Fleisch, Weizensamen |
| 2 Köderfisch | Roher Kabeljau |
| 4 Leuchtköder | Regenwurm, Leuchttintenbeutel |
| 2 Uralter Grundköder | Rohes Rindfleisch, Spinnenauge, Schlamm, Leuchttintenbeutel |
| Königswobbler | Köderfisch, Smaragd, Goldbarren, Feder |
| 2 Strömungsköder | Regenwurm, Prismarinkristalle, Ton, Knochenmehl |
| 2 Tiefenlicht | Leuchtköder, Echosplitter, Sculk |
| Kescher | klein: 2 Stöcke, 3 Faden · Karpfen: kleiner Kescher, Eisen, 3 Faden · Großfisch: Karpfenkescher, Eisenblock, Angelschnur, Leder |
| 4 Futtermischung | Weizen, Brot, Knochenmehl, Erde |
| Echolot | Kupferbarren, Redstone, Amethystscherbe, Glasscheibe |
| Köderbox | Truhe, Kupferbarren |
| Fischlexikon | Buch, Roher Kabeljau, Tintenbeutel |
| Setzkescher | 4 Faden, 2 Stöcke |
| Rutenhalter / Doppelständer / Rod Pod | 3 Stöcke + Eichenstufe / 2 Rutenhalter + Stock / 3 Eisen, Redstone, Notenblock |
| Unterwasser-Netz | 2 Setzkescher, Eisengitter, Faden |
| Fischmarkt | Fass, Smaragd, Eichenschild, Roher Kabeljau |
| Angelwerkstatt | Schmiedetisch, Angel, Eisenbarren |

Alle Items stehen sortiert im Kreativtab „Atzen – Angeln“.

## Befehle

```text
/atzen                          Hilfe
/atzen practice [Art|Boss]      Training ohne Beute mit der Rute in der Hand
                                (roach, river_trout, pike, carp, catfish, tuna,
                                 ancient_catfish, reed_king, black_sturgeon, deep_warden)
/atzen cancel                   Drill abbrechen
/atzen lexikon                  Fischlexikon öffnen
/atzen turnier start <min> [art]  Turnier: schwerster Fisch (der Art) gewinnt, Plätze 1–3: 16/8/4 Smaragde (OP)
/atzen turnier ende             Turnier sofort auswerten (OP)
/atzen leistung [niedrig|normal|hoch]
                                Leistungsstufe: wie weit Fische voll aktiv sind (24/48/96 Blöcke)
                                und wie oft Rutenhalter prüfen (OP)
```

## Entwicklung und Prüfungen

`./gradlew build` mit JDK 25 kompiliert Server- und Clientcode, baut die JAR und führt die JUnit-Prüfungen aus. Sie spielen den Drill ohne Minecraft mit einem Test-Angler (nur R und F) durch: jede gewöhnliche Art mit passender Ausrüstung fangbar, Dauer-Kurbeln reicht bei großen Fischen nicht, Kurbeln gegen Fluchten reißt die Schnur, jeder Boss ist mit bester Ausrüstung schaffbar und mit einfacher kaum, alle Aktionen werden angekündigt, F öffnet die Bremse. Dazu Setzkescher (keine Duplikate, Grenzen, schlechtes Wasser stresst und tötet erst bei Vernachlässigung), Kescher-Zielleiste, Fangdruck, Aufträge (für alle gleich), Erfahrung und Preise (Wiederholung lohnt nicht), Angelgebiete, Köderauswahl, Bossbedingungen, Maße, JSON-Dateien, Texturen, Übersetzungen und die Mixin-Ziele gegen Minecraft 26.2.

Ressourcen neu erzeugen: `node scripts/generate-resources.mjs` (Modelle, Rezepte, Übersetzungen), `python3 scripts/draw-textures.py` (alle Texturen, 64×64), `python3 scripts/sounds.py OUT` (Klänge).

Spielbalance: `FishingBattle.java` (Drill), `Landing.java` (Kescher), `Valuation.java` (Erfahrung, Preise), `Pressure.java` (Fangdruck), `WaterConditions.java` (Haltung), `Boss.java`, `FishSpecimen.fighter()` (Fischkraft), `ReelType`/`LineType`/`RodType`/`BaitType` (Ausrüstung), `FightHud.prompt()` (Anweisung im Drill).

Lizenz: MIT, siehe `LICENSE`.
