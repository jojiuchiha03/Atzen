# Entwicklungsanweisung: Realistische Minecraft-Angel-Simulation

Umsetzungsstand je Punkt: siehe [ROADMAP.md](ROADMAP.md).

## 1. Projektziel

Entwickle eine umfangreiche, realistische und anspruchsvolle Angel-Simulation für Minecraft Java. Das Spiel soll deutlich über das klassische Minecraft-Angeln hinausgehen und sich an realistischen Angelsimulationen sowie modernen Fishing-RPGs orientieren.

Der Fokus liegt auf einem physikalisch nachvollziehbaren Angel- und Drillsystem, einer dynamischen Cinematic-Kamera, eigenständigen Fisch-Mobs, einem umfangreichen Ausrüstungssystem, mehreren gleichzeitig verwendbaren Ruten, einem Kescher- und Fischhaltungssystem sowie seltenen Bossfischen.

Das Spiel soll langfristig motivieren, eine steile, aber faire Lernkurve besitzen und auch erfahrene Spieler fordern. Gute Ausrüstung erleichtert den Fang, ersetzt jedoch niemals Geschick, Vorbereitung und die richtige Technik.

Die Umsetzung soll als richtige Minecraft-Java-Mod erfolgen. Der konkrete Modloader und die Minecraft-Version müssen vor Projektbeginn festgelegt werden. Fabric und NeoForge sind mögliche Optionen. Reine Datapacks oder einfache Server-Plugins reichen für die vollständige Umsetzung der individuellen Kamerasteuerung und Animationen nicht aus.

Alle Systeme müssen modular, wartbar, erweiterbar und möglichst multiplayerfähig entwickelt werden.

## 2. Grundlegende Spielmechanik

Der vollständige Angelvorgang besteht aus folgenden Phasen:

1. Ausrüstung auswählen und zusammenstellen.
2. Angelstelle, Wetter, Tageszeit und Gewässerbedingungen beurteilen.
3. Köder vorbereiten und gegebenenfalls eine Montage konfigurieren.
4. Angel auswerfen.
5. Köder abhängig von Gewicht, Bauart und Strömung im Wasser positionieren.
6. Fischverhalten und Bisssignale beobachten.
7. Im richtigen Moment anschlagen.
8. Fisch durch gezielte Kontrolle von Rute, Rolle, Bremse und Schnur drillen.
9. Fisch erschöpfen und zur geeigneten Landungsstelle führen.
10. Fisch mit einem Kescher landen oder mit einer geeigneten Technik sicher entnehmen.
11. Fisch vermessen, wiegen, katalogisieren, verkaufen, halten oder freilassen.
12. Erfahrung sammeln, Ausrüstung verbessern und neue Angelgebiete freischalten.

Jede Phase muss eine eigene Mechanik besitzen. Das Angeln darf nicht auf eine zufällige Wartezeit mit anschließendem automatischem Fang reduziert werden.

## 3. Cinematic-Kamerasystem

Implementiere ein dynamisches Kamerasystem, das den Angelvorgang filmisch inszeniert, ohne die Kontrolle des Spielers unnötig einzuschränken.

### 3.1 Freie Kamera

Im normalen Spiel bewegt sich der Spieler mit der üblichen Minecraft-Kamera. Er kann sich am Ufer bewegen, Angelstellen erkunden, seine Ausrüstung wechseln und mit Gegenständen interagieren.

### 3.2 Wurfkamera

Beim Auswerfen wechselt die Kamera kurz in eine Schulterperspektive hinter dem Spieler. Die Kamera folgt der Bewegung der Rute und anschließend dem fliegenden Köder.

Die Wurfweite hängt unter anderem von Rute, Rolle, Spule, Schnur, Ködergewicht, Wurftechnik und möglichen Hindernissen ab.

Beim Auftreffen auf das Wasser entstehen passende Wasseranimationen und Geräusche. Leichte Köder reagieren anders als schwere Montagen.

### 3.3 Bisskamera

Wenn ein Fisch den Köder untersucht, kann die Kamera abhängig von Situation und Fischart kurz unter die Wasseroberfläche wechseln.

Der Spieler sieht den Fisch beim Annähern, Untersuchen und gegebenenfalls Attackieren des Köders. Ein bestätigter Biss löst eine kurze Spannungssequenz aus.

Nicht jeder Fisch, der sich dem Köder nähert, muss tatsächlich anbeißen.

### 3.4 Drillkamera

Nach einem erfolgreichen Anschlag wechselt die Kamera dynamisch zwischen einer geeigneten Spielerperspektive und einer Perspektive auf den kämpfenden Fisch.

Die Kamera zeigt Fluchten, Richtungswechsel, Bewegungen unter Wasser und die Belastung der Rute. Wichtige Steuerungsinformationen müssen weiterhin sichtbar bleiben.

### 3.5 Sprungkamera

Springt ein Fisch aus dem Wasser, wechselt die Kamera in eine Schulterperspektive hinter den Spieler. Die Rute biegt sich sichtbar, die Schnur bleibt abhängig von der Situation gespannt und der Fisch erzeugt realistische Wasserspritzer.

Eine kurze Zeitlupe kann optional eingesetzt werden. Während des Sprungs bleibt die tatsächliche Kampfmechanik aktiv. Der Spieler muss weiterhin auf Spannung und Fischbewegung reagieren.

### 3.6 Landungskamera

Bei der Landung folgt die Kamera dem Fisch, dem Kescher und der Bewegung des Spielers. Nach einem erfolgreichen Fang kann eine kurze Trophäenansicht erscheinen, die Art, Länge, Gewicht und Seltenheit anzeigt.

### 3.7 Technische Anforderungen

Implementiere einen zentralen Camera Controller mit klar definierten Zuständen. Dazu gehören mindestens IDLE, CASTING, WAITING, BITE, HOOKED, FIGHTING und LANDING.

Sprünge, Fluchten und besondere Bossaktionen werden als Ereignisse innerhalb des bestehenden Drillzustands behandelt.

Kamerasequenzen müssen überspringbar oder abbrechbar sein. Sie dürfen keine wichtigen Spielentscheidungen automatisch treffen.

Im Multiplayer steuert jeder Client seine eigene Kamera. Die maßgeblichen Spielzustände werden serverseitig berechnet und synchronisiert.

## 4. Angelruten

Implementiere verschiedene Rutenarten mit individuellen Eigenschaften, Einsatzgebieten und Nachteilen.

### 4.1 Bambusrute

Einsteiger-Rute mit geringer Belastbarkeit, begrenzter Wurfweite und hoher Flexibilität. Geeignet für kleine Friedfische und erste Angelaufgaben.

### 4.2 Spinnrute

Vielseitige Rute für aktive Köderführung. Geeignet für Barsche, Forellen und Hechte. Wurfgewicht und Aktion bestimmen die optimalen Köder.

### 4.3 Karpfenrute

Für Grundmontagen, größere Friedfische und lange Fluchten. Besitzt hohe Kraftreserven, ist aber weniger flexibel bei der Führung kleiner Kunstköder.

### 4.4 Fliegenrute

Spezialrute mit eigener Wurfmechanik und direkter Schnurkontrolle. Geeignet für Forellen, Äschen und ausgewählte Insektenfresser.

### 4.5 Big-Game-Rute

Schwere Rute für große Welse, Störe und Bossfische. Sie besitzt hohe Belastbarkeit, ist aber schwer und für kleine Fische ungeeignet.

### 4.6 Rutenattribute

Jede Rute besitzt folgende Werte:

- Wurfgewicht
- Belastbarkeit
- Flexibilität
- Rutenaktion
- Sensibilität
- Haltbarkeit
- Gewicht
- Geeigneter Fischgrößenbereich

Eine steife Rute bietet eine gute Kraftübertragung, während eine flexible Rute Fluchten besser abfedern kann. Keine Rute darf in allen Eigenschaften gleichzeitig überlegen sein.

## 5. Angelrollen und Spulen

Implementiere unterschiedliche Rollentypen.

### 5.1 Stationärrolle

Einsteigerfreundliche und vielseitige Rolle für viele Angeltechniken.

### 5.2 Baitcaster

Präzise Rolle für aktive Köderführung. Bei falscher Einstellung der Spulenbremse kann es zu Schnurverwicklungen kommen.

### 5.3 Multirolle

Rolle mit hoher Schnurkapazität und Belastbarkeit für tiefe Gewässer und große Fische.

### 5.4 Fliegenrolle

Dient hauptsächlich der Schnurkontrolle und Bremse. Die Wurfmechanik unterscheidet sich von herkömmlichen Rollen.

### 5.5 Austauschbare Spulen

Implementiere unterschiedliche Spulenausführungen:

- Flachspule für geeignete Wurfkonfigurationen mit dünner Schnur.
- Tiefspule für hohe Schnurkapazität.
- Leichtspule für leichte Köder.
- Verstärkte Spule für hohe Belastungen.
- Schnellübersetzung für höheren Schnureinzug.
- Kraftübersetzung für leichteres Einholen unter Last.

Spulenkapazität, Schnurdurchmesser, Rollentyp und Schnureinzug müssen miteinander abgestimmt sein.

## 6. Schnüre und Vorfächer

Implementiere mehrere Schnurtypen mit unterschiedlichen physikalischen Eigenschaften.

### 6.1 Monofile Schnur

Besitzt eine vergleichsweise hohe Dehnung und dämpft plötzliche Fluchten. Sie ist günstig und für Anfänger geeignet.

### 6.2 Geflochtene Schnur

Besitzt geringe Dehnung und hohe Tragkraft bei vergleichsweise kleinem Durchmesser. Sie ermöglicht eine direkte Rückmeldung, kann jedoch bei ungünstigem Kontakt mit Hindernissen beschädigt werden.

### 6.3 Fluorocarbon

Wird vor allem als Vorfachmaterial verwendet. Es besitzt geringe Sichtbarkeit unter Wasser und eine gute Abriebfestigkeit.

### 6.4 Stahlvorfach

Schützt vor scharfen Raubfischzähnen und ist insbesondere beim Hechtangeln sinnvoll.

### 6.5 Schnurattribute

Jede Schnur besitzt Tragkraft, Durchmesser, Dehnung, Abriebfestigkeit und Haltbarkeit.

Ein Schnurbruch darf nicht ausschließlich vom Gewicht des Fisches abhängen. Entscheidend ist die momentane Zugkraft im Verhältnis zur Tragkraft der Schnur und zur Belastung des gesamten Systems.

## 7. Haken

Implementiere unterschiedliche Hakenarten:

- Einzelhaken für vielseitige Anwendungen.
- Drillinge für geeignete Kunstköder.
- Schonhaken ohne Widerhaken für leichteres Lösen.
- Karpfenhaken für Boilies und Grundmontagen.
- Offsethaken für passende Gummiköder.
- Circle Hooks für bestimmte Angeltechniken.

Haken besitzen Größe, Festigkeit, Schärfe und Hakverhalten.

Zu kleine Haken können bei kräftigen Fischen aufbiegen. Zu große Haken können vorsichtige Fische abschrecken. Die Hakengröße muss zur Fischart und zum Köder passen.

## 8. Ködersystem

Implementiere natürliche Köder und Kunstköder mit individuellen Eigenschaften.

### 8.1 Natürliche Köder

- Regenwurm
- Made
- Mais
- Brot
- Boilie
- Köderfisch
- Fischfetzen
- Insektenlarve
- Garnele

### 8.2 Kunstköder

- Spinner
- Blinker
- Wobbler
- Gummifisch
- Popper
- Jig
- Streamer
- Trockenfliege
- Nymphe

Jeder Köder besitzt Gewicht, Größe, Farbe, Laufverhalten, Lockwirkung, Haltbarkeit und eine bevorzugte Führungstechnik.

Die Köderwahl beeinflusst, welche Fischarten sich nähern, wie häufig sie den Köder untersuchen und wie wahrscheinlich ein erfolgreicher Biss ist.

Ein ungeeigneter Köder kann ignoriert werden. Beschädigte Kunstköder können schlechter laufen. Naturköder verlieren mit der Zeit an Wirkung.

Ermögliche außerdem das Herstellen eigener Boilies, Futtermischungen, Fliegen, Leuchtköder und spezieller Bossköder.

Seltene Materialien sollen neue Eigenschaften oder Fangmöglichkeiten eröffnen, aber nicht automatisch jeden Köder verbessern.

## 9. Fischarten und Seltenheit

Implementiere unterschiedliche Fischarten mit eigenem Verhalten.

### 9.1 Friedfische

- Rotauge
- Rotfeder
- Brasse
- Schleie
- Karpfen
- Karausche
- Döbel

Diese Arten suchen Nahrung, reagieren auf Futterplätze und können bei Störungen vorsichtiger werden.

### 9.2 Raubfische

- Barsch
- Hecht
- Zander
- Forelle
- Wels
- Aal

Diese Arten reagieren auf Beutebewegungen, Strömung, Deckung und Sichtverhältnisse. Ihre Jagd- und Fluchtverhalten unterscheiden sich.

### 9.3 Großfische

Kapitale Karpfen, große Hechte, Welse und Störe besitzen höhere Kraftreserven, größere Ausdauer und anspruchsvollere Kampfphasen.

### 9.4 Seltenheitsstufen

Implementiere die Kategorien Gewöhnlich, Ungewöhnlich, Selten, Episch, Legendär und Mythisch.

Die Seltenheit beeinflusst Verfügbarkeit, Fangbedingungen und Belohnungen. Seltene Fische benötigen eigene Eigenschaften und sollen nicht lediglich größere Versionen gewöhnlicher Fische sein.

## 10. Realistisches Drill-System

Der Drill ist das zentrale Spielsystem und muss anspruchsvoll, nachvollziehbar und fair sein.

Verwende mindestens folgende Werte:

- Schnurspannung
- Fisch-Ausdauer
- Fischkontrolle
- Verbleibende Schnurkapazität
- Hakenhalt
- Rutenbelastung
- Zustand von Schnur und Montage

### 10.1 Anschlag

Der Spieler muss einen Biss erkennen und im richtigen Moment anschlagen. Der optimale Zeitpunkt hängt von Fischart, Montage und Köder ab.

### 10.2 Flucht

Der Fisch versucht, Abstand zu gewinnen. Die Rolle gibt abhängig von der Bremse Schnur frei.

### 10.3 Gegensteuern

Der Spieler muss den Fisch mit kontrolliertem seitlichem Rutendruck von Hindernissen fernhalten.

### 10.4 Pumpen

Der Spieler hebt die Rute kontrolliert an und holt beim Absenken Schnur ein. Falsches Timing führt zu Spannungsverlust oder Überlastung.

### 10.5 Spezialaktionen

Fische können springen, Kopfstöße ausführen, abrupt die Richtung wechseln, zum Grund flüchten oder in Hindernisse ziehen.

### 10.6 Landung

Ein Fisch gilt erst als erfolgreich gefangen, wenn er mit einem geeigneten Kescher oder einer zulässigen Landungstechnik gesichert wurde.

### 10.7 Bremse

Die Rollenbremse muss einstellbar sein.

- Eine zu lockere Bremse erlaubt lange Fluchten.
- Eine zu feste Bremse erhöht die Bruchgefahr.
- Eine geeignete Einstellung ermöglicht kontrolliertes Nachgeben und erneuten Druckaufbau.

Gute Ausrüstung reduziert Risiken, beseitigt sie aber nicht vollständig.

## 11. Verzauberungen und Angelmodule

Implementiere Verzauberungen und zusätzliche technische Module als getrennte Systeme.

### 11.1 Verzauberungen

- Köderglück III: Erhöht die Chance auf einen Biss.
- Angelglück III: Verbessert die Chance auf seltene Beute oder hochwertige Fänge.
- Haltbarkeit III: Reduziert den Verschleiß.
- Reparatur I: Ermöglicht Reparaturen über Erfahrung.
- Seelenruhe II: Reduziert Störungen scheuer Fische.
- Präzisionswurf III: Erleichtert präzise Würfe.
- Drillkontrolle III: Verringert Spannungsspitzen bei gutem Timing.

Diese Verzauberungen sind projektspezifische Vorschläge. Ihre Effekte müssen mit vorhandenen Minecraft-Verzauberungen abgeglichen werden.

### 11.2 Angelmodule

- Feinbremse
- Verstärkter Rutenblank
- Bisssensor
- Leichtlaufspule
- Keramik-Schnurlaufringe
- Verbesserte Rollenlager
- Verstärkte Hakenhalterung
- Spezielle Vorfachsysteme

Module besitzen klare Vor- und Nachteile. Begrenze die Zahl gleichzeitig aktiver Module, damit die Ausrüstung nicht unbegrenzt optimiert werden kann.

## 12. Erweiterungen und Hilfsmittel

Implementiere folgende Gegenstände:

- Rutenhalter
- Pose
- Futterkorb
- Echolot
- Schnurzähler
- Kescher
- Boot
- Sonar-Modul
- Spezialvorfach
- Boss-Montage
- Köderbox
- Angelwerkzeug
- Reparaturstation

Das Echolot kann Tiefenstrukturen und Fischschwärme anzeigen, darf aber nicht automatisch jeden Fisch exakt lokalisieren.

## 13. Abgestellte Ruten und Rutensystem

Der Spieler muss Angelruten am Ufer abstellen und mehrere Montagen gleichzeitig verwenden können.

### 13.1 Einzelner Rutenhalter

Eine Rute kann in einem einfachen Halter befestigt werden. Die Rute wird als sichtbares Objekt mit Rolle, Schnur und Köder dargestellt.

### 13.2 Doppel-Rutenständer

Ermöglicht zwei gleichzeitig ausgelegte Ruten mit unterschiedlichen Ködern und Montagen.

### 13.3 Professionelles Rod Pod

Ermöglicht mehrere Ruten, elektronische Bissanzeiger und erweiterte Halterungen.

### 13.4 Verhalten abgestellter Ruten

Die Ruten müssen unabhängig vom Spieler funktionieren.

Bei einem Biss bewegt sich die Rutenspitze. Ein Bissanzeiger kann Licht und Geräusche ausgeben.

Der Spieler interagiert mit der Rute, um sie aufzunehmen und den Drill zu übernehmen.

Während der Spieler nicht eingreift, entwickelt sich der Kampf begrenzt automatisch weiter. Der Fisch kann Schnur nehmen, sich lösen oder die Montage überlasten.

Bei mehreren Ruten können mehrere Bisse gleichzeitig auftreten. Der Spieler muss priorisieren. Fische können in andere Schnüre geraten.

Eine schlecht gesicherte Rute kann bei starken Fluchten aus dem Halter gezogen werden. Gute Halterungen reduzieren dieses Risiko.

## 14. Kescher-System

Der Kescher ist ein eigenes Werkzeug und muss aktiv zur Landung eingesetzt werden.

### 14.1 Kleine Kescher

Für kleine Friedfische, Barsche und Forellen.

### 14.2 Karpfenkescher

Große Netzfläche und längerer Griff für größere Friedfische.

### 14.3 Großfischkescher

Verstärkter Rahmen und tieferes Netz für große Welse, Störe und bestimmte Bossfische.

### 14.4 Landungsmechanik

Der Spieler muss den Fisch in einen geeigneten Bereich führen, den Kescher ausrüsten, seine Öffnung passend ausrichten und den Fisch kontrolliert aufnehmen.

Die Erfolgschance hängt von Fischgröße, Keschergröße, Ausrichtung, Abstand und Fischverhalten ab.

Ein Fisch kann im letzten Moment ausschlagen. Ein zu kleiner Kescher kann nicht für jeden Fisch verwendet werden.

Nach erfolgreicher Landung wird der Zustand des Fisches erhalten. Der Fisch kann vermessen, gewogen, gehalten, freigelassen oder anderweitig verarbeitet werden.

Bossfische benötigen gegebenenfalls spezielle Landungsmechaniken.

## 15. Setzkescher und Unterwasser-Netze

Implementiere zwei voneinander getrennte Haltungssysteme.

### 15.1 Setzkescher

Ein am Ufer befestigter, teilweise oder vollständig im Wasser liegender Kescher für die vorübergehende Haltung gefangener Fische.

Eigenschaften:

- Begrenzte Kapazität.
- Begrenztes Gesamtgewicht.
- Größenabhängige Eignung.
- Eingeschränkte Bewegungsfreiheit.
- Erhalt individueller Fischdaten.
- Entnahme und Freilassung einzelner Fische.
- Beeinflussung des Fischzustands durch Haltungsbedingungen.

### 15.2 Unterwasser-Fischnetz

Ein platzierbares Gehege für längere Haltung, Beobachtung und gegebenenfalls kontrollierte Fischzucht.

Eigenschaften:

- Definierter Innenraum.
- Begrenzte Kapazität abhängig vom Volumen.
- Bewegungsraum für die enthaltenen Fische.
- Fischartenabhängige Anforderungen.
- Speicherung der Fischidentitäten und ihrer Zustände.
- Interaktion zum Einsetzen, Entnehmen und Freilassen.

### 15.3 Haltungsbedingungen

Jeder Behälter besitzt Werte für Wasservolumen, Sauerstoffversorgung, Temperatur, Besatzdichte und Wasserqualität.

Schlechte Bedingungen führen zunächst zu Stress und Konditionsverlust. Schwere Vernachlässigung kann zum Tod führen.

Der Spieler muss frühzeitig erkennen können, wenn Bedingungen problematisch werden.

Fische dürfen nicht dupliziert werden. Beim Einsetzen in ein Netz wird ihre Identität aus der bisherigen Weltrepräsentation in das Haltungssystem übertragen. Beim Entnehmen wird derselbe Fisch mit seinen Daten wieder als Entity erzeugt.

### 15.4 Fischzucht

Später können geeignete Fischarten unter passenden Bedingungen gehalten und gezüchtet werden.

Die Zucht benötigt Platz, Nahrung, geeignete Wasserbedingungen und gegebenenfalls saisonale Voraussetzungen. Sie darf nicht zu einer kostenlosen, unbegrenzten Ressourcenquelle werden.

## 16. Eigenständige Fisch-Mobs

Jeder Fisch muss eine eigenständige Minecraft-Entity sein und unabhängig vom Angeln in der Welt existieren.

### 16.1 Individuelle Eigenschaften

Jeder Fisch besitzt mindestens:

- Fischart
- Individuelle Identität
- Größe
- Gewicht
- Alter
- Hunger
- Ausdauer
- Scheu
- Gesundheitszustand
- bevorzugte Tiefe
- bevorzugten Lebensraum
- individuelle Bewegungsmuster

Die Daten müssen dauerhaft gespeichert werden, soweit sie für die jeweilige Fischart und Spielmechanik relevant sind.

### 16.2 Bewegungsverhalten

Fische bewegen sich abhängig von Art, Strömung, Tiefe, Umgebung, Nahrung und Gefahren.

Sie sollen nicht lediglich zufällig durch das Wasser schwimmen.

Implementiere Nahrungssuche, Ruhephasen, Fluchtverhalten, Revierverhalten, Schwarmbildung und artspezifische Bewegungsmuster.

### 16.3 Schwarmverhalten

Schwärme bestehen aus mehreren eigenständigen Fisch-Mobs, die sich koordiniert bewegen.

Ein Schwarm besitzt einen Mittelpunkt, eine Bewegungsrichtung, eine bevorzugte Größe und einen Zustand.

Einzelne Fische orientieren sich am Schwarm, können aber ausscheren, Nahrung verfolgen oder vor Raubfischen fliehen.

Implementiere mindestens folgende Schwarmtypen:

- Futterschwarm
- Scheuer Schwarm
- Wandernder Schwarm
- Laichgruppe
- Räuber-Beute-Gruppe
- Tiefenschwarm

Die Verteilung der Fische muss abhängig von Tageszeit, Wetter, Temperatur, Nahrung und Raubfischaktivität variieren.

### 16.4 Raubfischverhalten

Raubfische müssen kleinere Fische erkennen, verfolgen und unter geeigneten Bedingungen angreifen können.

Die Beute reagiert mit Fluchtbewegungen. Schwärme können auseinanderbrechen. Raubfische müssen nicht ständig erfolgreich jagen.

Die Jagd soll natürliche Verhaltensweisen erzeugen und gleichzeitig die Performance des Spiels berücksichtigen.

## 17. Fischökologie

Implementiere ein vereinfachtes Ökosystem.

Pflanzen, Algen und Kleinstlebewesen bilden die Nahrungsgrundlage für geeignete Fischarten. Kleine Fische können wiederum Nahrung für Raubfische sein.

Die Fischbestände reagieren auf Nahrung, Wasserbedingungen, Fortpflanzung, Wanderung, Raubfische und Fangdruck.

Häufiges Angeln kann Fische vorsichtiger machen oder die Bestandszusammensetzung eines Gewässerabschnitts verändern.

Fischbestände müssen sich langfristig regenerieren können. Verwende dafür Nachwuchs, Zuwanderung und zeitabhängige Wiederbesiedlung.

Nicht jede Fischart muss vollständig simulierte Fortpflanzung besitzen. Die Ökologie soll nachvollziehbar sein, ohne unnötig komplex zu werden.

## 18. Bossfische

Bossfische sind einzigartige Begegnungen mit besonderen Verhaltensweisen, Voraussetzungen und Belohnungen.

### 18.1 Der Alte vom Grund

Ein riesiger Wels in einem tiefen See.

Er flüchtet in versunkene Baumstämme, führt lange Fluchten aus und täuscht kurz vor der Landung Erschöpfung vor.

### 18.2 Der Schilfkönig

Ein außergewöhnlich großer Hecht im Nebelmoor.

Er reagiert auf schnelle Köderbewegungen, flüchtet in Pflanzenbestände und springt mehrfach, um den Haken abzuschütteln.

### 18.3 Der Schwarze Stör

Ein massiver Stör in einem tiefen Fluss.

Die Strömung erhöht die Belastung der Schnur. Der Fisch wechselt zwischen ruhigen Bewegungen und explosiven Fluchten.

### 18.4 Der Tiefenwächter

Ein mythischer Fisch in einem verborgenen Gewässer.

Er besitzt mehrere Kampfphasen, reagiert auf wiederholte Drilltechniken und verändert seine Bewegungsmuster.

### 18.5 Bossmechaniken

Jeder Boss besitzt:

- Eigene Voraussetzungen für die Begegnung.
- Eine besondere Erscheinung oder Silhouette.
- Individuelle Bewegungsmuster.
- Mehrere Kampfphasen.
- Spezielle Fluchten und Reaktionen.
- Besondere Landungsanforderungen.
- Einzigartige Belohnungen.
- Eintrag im Fischlexikon.

Die Schwierigkeit soll durch nachvollziehbare Mechaniken entstehen und nicht nur durch überhöhte Lebenspunkte.

## 19. Biome und Fortschritt

Implementiere mehrere Angelgebiete mit unterschiedlichen Fischarten und Bedingungen.

### Heimatsee

Einsteigergebiet mit kleinen Fischen, einfachen Montagen und ersten Aufgaben.

### Waldfluss

Strömung, tiefe Pools, Forellen und Döbel. Präzise Würfe und passende Köderführung werden wichtig.

### Nebelmoor

Dichte Vegetation, scheue Fische, Hindernisse und anspruchsvolle Raubfischtechniken.

### Tiefenreservat

Tiefe Gewässer, Großfische, seltene Arten und anspruchsvolle Drills.

Später können weitere Gebiete mit besonderen Fischarten, Wetterbedingungen und Bossbegegnungen ergänzt werden.

## 20. Wirtschaft und Langzeitmotivation

Der Spieler kann durch Fischverkäufe, Aufgaben, Turniere, Forschungsaufträge und seltene Fänge Geld und Erfahrung verdienen.

Die Fischbewertung berücksichtigt Art, Gewicht, Länge, Qualität, Seltenheit und Zustand.

Implementiere:

- Tages- und Wochenaufträge.
- Angelturniere.
- Fischlexikon.
- Fangstatistik.
- Trophäensammlung.
- Händler und Werkstatt.
- Ausrüstungs-Upgrades.
- Seltene Herstellungsmaterialien.
- Freischaltung neuer Gebiete.
- Ranglisten für kapitale Fänge.

Wiederholtes Farmen derselben kleinen Fischart darf nicht automatisch die beste Fortschrittsstrategie darstellen.

## 21. Technische Architektur

Entwickle das Projekt modular mit klar getrennten Verantwortlichkeiten.

| Baustein | Verantwortung |
| --- | --- |
| FishEntity | Verwaltet einzelne Fisch-Mobs, Identität, Größe, Zustand und Bewegungsverhalten. |
| SchoolManager | Verwaltet Schwärme, koordinierte Bewegungen und vereinfachte Schwarmsteuerung. |
| PredatorAI | Verwaltet Beuteerkennung, Jagdverhalten und Raubfischreaktionen. |
| FishingRodEntity | Stellt abgestellte Ruten und sichtbare Angelmontagen dar. |
| FishingRig | Verwaltet Rolle, Schnur, Haken, Köder und Bremse. |
| FishingLineSimulation | Berechnet vereinfachte Schnurphysik, Spannung, Durchhang und Bruchrisiko. |
| LandingNet | Verwaltet Kescher, Landungsbereich und sichere Fischübertragung. |
| KeepnetBlockEntity | Verwaltet Setzkescher, enthaltene Fische und Haltungsbedingungen. |
| FishPenBlockEntity | Verwaltet Unterwasser-Netze, Kapazität, Fischbewegung und Haltungsbedingungen. |
| CameraController | Steuert Wurf-, Biss-, Drill-, Sprung- und Landungskamera. |
| AquaticSimulationManager | Verwaltet Gewässerbedingungen, vereinfachte Ökologie und Bestandsveränderungen. |
| ProgressionManager | Verwaltet Erfahrung, Geld, Aufgaben, Freischaltungen und Fischlexikon. |

Die maßgeblichen Spielzustände müssen serverseitig berechnet werden. Kamera, visuelle Effekte und lokale Animationen können clientseitig verarbeitet werden.

## 22. Performance und Speicherung

Verwende eine abgestufte Simulation.

- **Nahbereich:** Vollständige KI, Animationen, Kollisionen und individuelle Reaktionen.
- **Mittelbereich:** Vereinfachte Bewegung, Schwarmsteuerung und reduzierte Aktualisierungsrate.
- **Fernbereich und ungeladene Chunks:** Gespeicherte Bestandsdaten und zeitabhängige Veränderungen ohne permanente Vollsimulation.

Vermeide unnötig häufige Berechnungen und übermäßige Entity-Anzahlen.

Fische müssen beim Speichern und Laden ihre relevanten Eigenschaften behalten. Beim Wechsel zwischen Welt, Kescher und Unterwasser-Netz dürfen keine Duplikate entstehen.

Synchronisiere wichtige Angelzustände serverseitig, insbesondere Fischposition, Schnurspannung, Ausdauer, Bissstatus und Kampfphase.

## 23. Entwicklungsreihenfolge

Entwickle das Projekt in klaren, testbaren Phasen.

1. **Grundgerüst:** Minecraft-Version und Modloader festlegen, Projektstruktur erstellen und grundlegende Registrierung testen.
2. **Fisch-Mobs:** Drei Fischarten mit Größenunterschieden, eigener Bewegung und ersten Schwarmverhaltensweisen implementieren.
3. **Angelgrundsystem:** Auswerfen, Köder, Biss, Anschlag, Schnurspannung, Bremse und Drill implementieren.
4. **Ruten und Rollen:** Ausrüstungssystem, austauschbare Komponenten, Schnurtypen und Haken ergänzen.
5. **Cinematic-Kamera:** Wurfkamera, Bisskamera, Fischkamera, Schulterkamera bei Sprüngen und Landungssequenz integrieren.
6. **Abgestellte Ruten:** Einzelhalter, Doppelständer, Rod Pod und Bissanzeiger implementieren.
7. **Kescher und Setzkescher:** Fische sicher landen, übertragen, halten und freilassen können.
8. **Unterwasser-Netze:** Platzierbare Fischgehege mit Kapazität, Bewegung und Haltungsbedingungen ergänzen.
9. **Ökologie:** Schwarmverhalten, Raubfischjagd, Nahrungsbeziehungen und vereinfachte Bestandsregeneration entwickeln.
10. **Fortschritt:** Werkstatt, Geld, Erfahrung, Fischlexikon, Aufgaben und neue Angelgebiete ergänzen.
11. **Bossfische:** Mehrphasige Bosskämpfe mit einzigartigen Verhaltensweisen und Belohnungen entwickeln.
12. **Balancing und Multiplayer:** Performance, Speicherung, Synchronisierung, Schwierigkeitsgrad und Exploit-Schutz testen.

## 24. Anforderungen an die Umsetzung

Arbeite in kleinen, überprüfbaren Entwicklungsschritten.

Erstelle nicht sofort sämtliche Fischarten, Gegenstände und Bossmechaniken gleichzeitig.

Beginne mit einer stabilen Grundlage aus wenigen Fischarten, einer Rute und einem vollständigen Drill. Ergänze anschließend die weiteren Systeme.

Jede Entwicklungsphase muss kompilierbar und testbar sein. Prüfe APIs und Abhängigkeiten anhand der tatsächlich gewählten Minecraft-Version und des Modloaders. Verwende keine erfundenen Methoden oder nicht existierenden Bibliotheken.

Trenne Spielregeln, Darstellung, Physik und Speicherung sauber voneinander. Vermeide unnötig große Klassen und hart codierte Werte, die später schwer zu ändern sind.

Konfiguriere wichtige Balancing-Werte möglichst zentral.

Teste insbesondere:

- Fischfang ohne Duplikate.
- Ruten und Fische über Chunk-Grenzen hinweg.
- Mehrere gleichzeitig aktive Ruten.
- Schnurbruch und Hakenverlust.
- Kescherfang und Freilassung.
- Speicherung von Fischen in Netzen.
- Wiederherstellung nach Weltneustarts.
- Synchronisierung im Multiplayer.
- Performance bei großen Schwärmen.
- Kamerawechsel während kritischer Spielsituationen.

## 25. Definition of Done

Das Projekt gilt erst dann als funktionsfähiger erster Prototyp, wenn folgende Funktionen tatsächlich umgesetzt und getestet wurden:

- Mindestens fünf eigenständige Fischarten.
- Individuelle Fischbewegungen und erste Schwarmmechaniken.
- Ein funktionierender Wurf- und Bissablauf.
- Ein anspruchsvoller Drill mit Bremse und Schnurbruch.
- Sichtbare Rutenbiegung und dynamische Kamerawechsel.
- Fischsprünge mit Schulterkamera.
- Aktive Landung mit einem Kescher.
- Abstellbare Angelruten mit funktionierenden Bissanzeigen.
- Ein Setzkescher, in dem Fische erhalten bleiben und weiterleben.
- Ein Unterwasser-Netz mit begrenzter Kapazität.
- Persistente Fischdaten ohne Duplikate.
- Eine grundlegende Werkstatt und ein Fortschrittssystem.
- Ein Bossfisch mit eigenständigen Kampfphasen.

Alle Funktionen müssen im Spiel überprüfbar sein. Bloße Platzhalter, unverbundene Menüs oder Gegenstände ohne tatsächliche Wirkung gelten nicht als vollständige Implementierung.

## 26. Abschließende Entwicklungspriorität

Die wichtigste Priorität ist ein überzeugender, anspruchsvoller und stabiler Angel-Drill. Danach folgen eigenständige Fisch-Mobs, das Rutensystem, Kescher und Fischhaltung. Cinematics, umfangreiche Ökologie und Bossfische werden auf dieser Grundlage ergänzt.

Das Ziel ist keine Sammlung neuer Angelgegenstände, sondern eine zusammenhängende Angelsimulation, in der Fischverhalten, Ausrüstung, Kamera, Gewässer und Spielerentscheidungen tatsächlich miteinander interagieren.

Beginne mit der Auswahl einer konkreten Minecraft-Java-Version und eines passenden Modloaders. Erstelle anschließend das Projektgerüst und implementiere den ersten vertikalen Prototypen: einen Fisch-Mob, eine Rute, einen vollständigen Drill und eine erfolgreiche Landung. Erst wenn dieser Ablauf zuverlässig funktioniert, werden die weiteren Systeme ergänzt.
