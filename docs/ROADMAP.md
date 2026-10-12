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

## Stufen

| Version | Inhalt | Anforderungen |
| --- | --- | --- |
| **0.6.0** | Kamera-Controller mit Zuständen (Wurf, Warten am Köder, Biss, Anschlag, Drill, Sprung, Landung), Bremse einstellbar, Schnurkapazität, Hakenhalt, Pumpen, längere Drills, Schnurbruch nach Zugkraft/Tragkraft; Rollentypen mit Spezialfähigkeiten; 18 Köder mit Artvorlieben; Seltenheit in 6 Stufen; erster Bossfisch | 3, 5.1–5.4, 6.5, 8, 9.4, 10, 18.1 |
| 0.7.0 | Rutentypen (Bambus, Spinn, Karpfen, Fliege, Big-Game) mit Attributen, Spulen, Vorfächer (Fluo, Stahl), Haken | 4, 5.5, 6, 7 |
| 0.8.0 | Kescher (klein/Karpfen/Großfisch) mit aktiver Landung, Trophäenansicht, Setzkescher | 3.6, 10.6, 14, 15.1 |
| 0.9.0 | Rutenhalter, Doppelständer, Rod Pod, Bissanzeiger, mehrere Ruten gleichzeitig | 13 |
| 0.10.0 | Unterwasser-Netz, Haltungsbedingungen, einfache Zucht | 15.2–15.4 |
| 0.11.0 | Fisch-Individuen (Alter, Hunger, Scheu), Schwarmtypen, Raubfischjagd, Fangdruck/Regeneration | 16, 17 |
| 0.12.0 | Werkstatt, Händler, Geld/Erfahrung, Lexikon, Aufträge, Trophäen, Ranglisten, Angelgebiete | 19, 20 |
| 0.13.0 | Restliche Bosse (Schilfkönig, Schwarzer Stör, Tiefenwächter), Verzauberungen, Module, Hilfsmittel (Echolot, Futterkorb …) | 11, 12, 18 |
| 0.14.0 | Balancing, Multiplayer- und Dauertests, Performance-Stufen | 22, 24 |
