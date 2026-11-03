# FrogIsMusicOverlay (Fabric, Minecraft 26.2)

Zeigt die Lyrics des Songs, den du gerade in der **Spotify-Desktop-App** hörst – synchron zur Zeile –
als 2D-HUD **oder als 3D-Text in der Welt**.

## Features
- Automatische Song-Erkennung inkl. **Song-Position** (Windows: Medien-Session via PowerShell, Linux: playerctl). Kein Spotify-Login, kein API-Key.
- Lyrics von **LRCLIB** (kostenlos, zeilengenau synchronisiert).
- Darstellungsarten:
  - 2D-HUD (9 Anker-Positionen + Versatz)
  - 3D: schwebt **vor dir in der Luft**
  - 3D: **am Himmel**
  - 3D: **flach auf Boden / Wasseroberfläche eines Sees** (rastet auf dem Wasser ein)
  - 3D: **fest in der Welt** gespeichert (Taste "Hier festsetzen")
  - 3D: **kreist um dich**
- Einstellungsmenü (Taste **J**): Schriftart, Größe, Zeilenanzahl, Abstand, Ausrichtung, fett/kursiv/Schatten,
  Farben (Farbwähler) für aktuelle/andere Zeilen, Titel, Hintergrund, Umrandung, Deckkraft, Regenbogen-Modus,
  Puls-Animation, Abstand/Höhe/Seite/Drehung/Neigung/Rollen, Folge-Geschwindigkeit, durch Wände sichtbar, Sync-Versatz ...
- **Demo-Modus**: Test-Lyrics ohne Spotify, um alles in Ruhe einzustellen.
- Tasten (im Steuerungs-Menü): Einstellungen, An/Aus, Lyrics ±250 ms, Position festsetzen.

## Bauen
Voraussetzung: **JDK 25**.
```
gradle wrapper --gradle-version 9.5.1      # einmalig (oder Wrapper aus der Fabric-Example-Mod kopieren)
./gradlew build
```
Die Mod liegt danach in `build/libs/`. Zum Spielen: Fabric Loader ≥ 0.19.3, Fabric API und **YetAnotherConfigLib (YACL)** in `mods/`.
Zum Testen: `./gradlew runClient`.

## Eigene Schriften
Minecraft-Schriften kommen aus Resource Packs. Beispiel `assets/meinpack/font/frog.json`:
```json
{ "providers": [ { "type": "ttf", "file": "meinpack:frog.ttf", "shift": [0, 0], "size": 11.0, "oversample": 8.0 } ] }
```
TTF nach `assets/meinpack/font/frog.ttf`, Pack aktivieren, im Menü bei *Schriftart* "Eigene Schrift" wählen und `meinpack:frog` eintragen.

## Hinweise
- Spotify-Desktop-App nötig (nicht der Web-Player). macOS wird aktuell nicht unterstützt.
- Die Stellen mit `26.2-API-RISK` im Code (HudRenderer, WorldRenderer, Client-Entrypoint) sind die einzigen,
  die direkt an Minecraft-Rendering-Klassen hängen. Falls Mojang/Fabric dort in 26.2 etwas umbenannt hat, nur dort anpassen.

## Einzel-Jar (z.B. für NoRisk Client)
YACL ist per Jar-in-Jar eingebettet. Nach `./gradlew build` ist **`build/libs/frogissmusicoverlay-1.0.0.jar`** die einzige Datei,
die du brauchst (NICHT die `-sources.jar`). Fabric API muss im Profil vorhanden sein (bei NoRisk meist schon dabei).
