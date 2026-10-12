# Roadmap

Arbeitsplan zu [ANFORDERUNGEN.md](ANFORDERUNGEN.md). Grundlage: **Minecraft Java 26.2 + Fabric** (Loom 1.17.21, Fabric API 0.161.0). Jede Stufe ist eine eigene, kompilierbare und im Spiel prüfbare Version.

Legende: ✅ fertig · 🔶 teilweise · ⬜ offen

## Bestand bis 0.5.0

| Punkt | Stand |
| --- | --- |
| 1 Modloader/Version | ✅ Fabric 26.2 |
| 9 Fischarten | 🔶 48 Arten mit Lebensräumen; Karausche, Stör fehlen; 3 statt 6 Seltenheitsstufen |
| 10 Drill | 🔶 Spannung, Ausdauer, Fluchten, Warnphase; keine Bremse, kein Pumpen, kein Hakenhalt |
| 3 Kamera | 🔶 Schulterkamera im Drill; keine Zustandsmaschine, keine Biss-/Wurf-/Landungskamera |
| 4–6 Ausrüstung | 🔶 Rolle/Schnur/Köder-Slots; Rutentypen, Spulen, Vorfächer und Haken fehlen |
| 16 Fisch-Mobs | 🔶 eigene Entities mit Länge/Gewicht, schwimmen zum Köder; Hunger/Scheu/Alter fehlen |

## Stand nach 0.6.0

| Punkt | Stand |
| --- | --- |
| 2 Angelvorgang | 🔶 Auswerfen, Biss mit Inspektion, Anschlag-Timing, Drill, Landung per Hand; Wurfweite/Montage offen |
| 3 Kamera | ✅ Zustände IDLE, CASTING, WAITING, BITE, HOOKED, FIGHTING, LANDING (+ TROPHY), abschaltbar/überspringbar; Rutenbiegung offen |
| 5 Rollen | 🔶 13 Rollen (Stationär, Baitcaster mit Perücke, Multi, Fliege + Spezialrollen); Spulen offen |
| 6 Schnüre | 🔶 Mono/Geflecht/Carbon mit Tragkraft, Dehnung, Abrieb; Vorfächer offen |
| 8 Köder | 🔶 alle 18 aus der Liste + Leucht-/Kraft-/Bossköder, Artvorlieben, Verschleiß; Futtermischungen, Alterung offen |
| 9 Fische | ✅ 50 Arten inkl. Karausche und Stör, 6 Seltenheitsstufen, artspezifisches Drillverhalten |
| 10 Drill | ✅ Spannung, Ausdauer, Hakenhalt, Schnurkapazität, Schnurzustand, Bremse, Pumpen, Gegensteuern, Spezialaktionen; Kescher-Landung kommt in 0.8.0 |
| 4 Ruten | ✅ (0.7.0) Bambus, Spinn, Karpfen, Fliege, Big-Game + 4 ältere, alle Attribute aus 4.6, Rutenbelastung und Bruch |
| 5.5 Spulen | ✅ (0.7.0) alle sechs; Schnurdurchmesser bestimmt die Kapazität |
| 6 Vorfächer | ✅ (0.7.0) Fluorocarbon, Stahl; Durchbeißen ohne Stahl |
| 7 Haken | ✅ (0.7.0) alle sechs Arten (Einzelhaken in drei Größen), Größe, Festigkeit, Hakverhalten |
| 3.2 Wurfweite | 🔶 (0.7.0) Rute, Spule, Ködergewicht; Hindernisse beim Wurf offen |
| 18 Bosse | ✅ alle vier mit Bedingungen, Phasen, Eigenheiten, Trophäen; im Lexikon und in den Rekorden |

## Stand nach 0.14.0

| Punkt | Stand |
| --- | --- |
| 3.6 / 10.6 / 14 Landung, Kescher | ✅ drei Kescher, Zielleiste, Handlandung bis 2 kg, Bosse nur mit Großfischkescher |
| 15.1 Setzkescher | ✅ Kapazität, Gewicht, Größe, Bedingungen, Warnungen, Tod bei Vernachlässigung, keine Duplikate |
| 15.2–15.4 Netz, Haltung, Zucht | ✅ Platz nach Wasservolumen, Füttern, Zucht gesunder Paare |
| 13 Rutenhalter | ✅ Halter, Doppelständer, Rod Pod mit Alarm, unbeaufsichtigte Bisse, Verheddern |
| 16 Fisch-Mobs | ✅ Individuen mit Alter, Hunger, Scheu, Zustand; Schwarmverhalten, Raubfischjagd, Tiefe |
| 17 Ökologie | ✅ Fangdruck pro Chunk mit Erholung, Futterplätze, Einfluss auf Bisse, Größen und Spawns |
| 19 Angelgebiete | ✅ fünf Gebiete nach Gewässer, seltene Fische ab Anglerstufe |
| 20 Fortschritt | ✅ Erfahrung/Stufen, Markt, Werkstatt, Lexikon, Aufträge, Rekorde, Rangliste, Turniere |
| 11 Hilfsmittel | ✅ Echolot, Futtermischung, Köderbox, Angelwerkzeug, Kescher; Bootangeln wie vom Ufer |
| 12 Module, Verzauberungen | ✅ 13 Module, Ruhige Hand, Präzisionswurf, Drillkontrolle, Vanilla Köder/Glück des Meeres |
| 22 Performance | ✅ Verhalten nur nahe Spielern, Leistungsstufen, Fangdruck nur pro Chunk |
| 24 Tests | 🔶 Regeln als JUnit, Server-Rauchtest; Mehrspieler- und Dauertests im Spiel stehen aus |

## Stufen

| Version | Inhalt | Anforderungen |
| --- | --- | --- |
| ✅ **0.6.0** | Kamera-Controller mit Zuständen (Wurf, Warten am Köder, Biss, Anschlag, Drill, Sprung, Landung), Bremse einstellbar, Schnurkapazität, Hakenhalt, Pumpen, längere Drills, Schnurbruch nach Zugkraft/Tragkraft; Rollentypen mit Spezialfähigkeiten; 25 Köder mit Artvorlieben; Seltenheit in 6 Stufen; alle vier Bossfische | 3, 5.1–5.4, 6.5, 8, 9.4, 10, 18 |
| ✅ 0.7.0 | Rutentypen (Bambus, Spinn, Karpfen, Fliege, Big-Game) mit Attributen, Spulen, Vorfächer (Fluo, Stahl), Haken | 4, 5.5, 6, 7 |
| ✅ 0.8.0 | Kescher (klein/Karpfen/Großfisch) mit aktiver Landung, Trophäenansicht, Setzkescher | 3.6, 10.6, 14, 15.1 |
| ✅ 0.9.0 | Rutenhalter, Doppelständer, Rod Pod, Bissanzeiger, mehrere Ruten gleichzeitig | 13 |
| ✅ 0.10.0 | Unterwasser-Netz, Haltungsbedingungen, einfache Zucht | 15.2–15.4 |
| ✅ 0.11.0 | Fisch-Individuen (Alter, Hunger, Scheu), Schwarmtypen, Raubfischjagd, Fangdruck/Regeneration | 16, 17 |
| ✅ 0.12.0 | Werkstatt, Händler, Geld/Erfahrung, Lexikon, Aufträge, Trophäen, Ranglisten, Angelgebiete | 19, 20 |
| ✅ 0.13.0 | Verzauberungen nach Liste, Angelmodule, Hilfsmittel (Echolot, Futterkorb, Köderbox …), Bootangeln | 11, 12 |
| ✅ 0.14.0 | Balancing, Multiplayer- und Dauertests, Performance-Stufen | 22, 24 |
