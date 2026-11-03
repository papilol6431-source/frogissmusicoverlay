package dev.frogis.musicoverlay.config;

import dev.frogis.musicoverlay.FrogisMusicOverlayClient;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Das Einstellungsmenue (YetAnotherConfigLib): Tabs, Slider, Farbwaehler, Dropdowns. */
public final class ConfigScreen {
    private ConfigScreen() { }

    public static Screen create(Screen parent) {
        OverlayConfig c = OverlayConfig.get(), d = new OverlayConfig();

        ConfigCategory general = ConfigCategory.createBuilder().name(Component.literal("Allgemein"))
                .option(bool("Overlay aktiv", "Schaltet das ganze Overlay an/aus.", d.enabled, () -> c.enabled, v -> c.enabled = v))
                .option(enumOpt("Darstellung", "Wo und wie die Lyrics erscheinen: HUD, vor dir, Himmel, See ...", OverlayConfig.Mode.class, d.mode, () -> c.mode, v -> c.mode = v))
                .option(bool("Demo-Modus", "Zeigt Test-Lyrics, auch ohne Spotify. Perfekt zum Einstellen!", d.demoMode, () -> c.demoMode, v -> c.demoMode = v))
                .option(bool("Titel/Interpret anzeigen", "Kleine Kopfzeile ueber den Lyrics.", d.showHeader, () -> c.showHeader, v -> c.showHeader = v))
                .option(bool("Bei Pause ausblenden", "Versteckt das Overlay, wenn Spotify pausiert ist.", d.hideWhenPaused, () -> c.hideWhenPaused, v -> c.hideWhenPaused = v))
                .option(bool("Mit F1 (GUI aus) ausblenden", "Overlay verschwindet bei versteckter Oberflaeche.", d.hideWithGui, () -> c.hideWithGui, v -> c.hideWithGui = v))
                .build();

        ConfigCategory text = ConfigCategory.createBuilder().name(Component.literal("Text & Schrift"))
                .option(enumOpt("Schriftart", "Vorgegebene Schriften. 'Eigene Schrift' nutzt das Feld darunter.", OverlayConfig.FontPreset.class, d.fontPreset, () -> c.fontPreset, v -> c.fontPreset = v))
                .option(string("Eigene Schrift (ID)", "z.B. meinpack:frog  -> aus einem Resource Pack (assets/meinpack/font/frog.json, TTF-Provider).", d.customFont, () -> c.customFont, v -> c.customFont = v))
                .option(flt("Textgroesse (HUD)", "Skalierung im HUD-Modus.", d.scale, 0.3f, 6f, 0.05f, "x", () -> c.scale, v -> c.scale = v))
                .option(intg("Zeilen davor", "Wie viele vergangene Zeilen sichtbar sind.", d.linesBefore, 0, 6, "", () -> c.linesBefore, v -> c.linesBefore = v))
                .option(intg("Zeilen danach", "Wie viele kommende Zeilen sichtbar sind.", d.linesAfter, 0, 8, "", () -> c.linesAfter, v -> c.linesAfter = v))
                .option(intg("Zeilenabstand", "Abstand zwischen den Zeilen.", d.lineSpacing, 0, 20, "", () -> c.lineSpacing, v -> c.lineSpacing = v))
                .option(enumOpt("Ausrichtung", "Links, zentriert oder rechts.", OverlayConfig.Align.class, d.align, () -> c.align, v -> c.align = v))
                .option(intg("Max. Zeilenbreite", "Laengere Zeilen werden automatisch verkleinert.", d.maxWidth, 60, 600, "", () -> c.maxWidth, v -> c.maxWidth = v))
                .option(bool("Aktuelle Zeile fett", "", d.boldCurrent, () -> c.boldCurrent, v -> c.boldCurrent = v))
                .option(bool("Kursiv", "", d.italic, () -> c.italic, v -> c.italic = v))
                .option(bool("Schatten", "", d.shadow, () -> c.shadow, v -> c.shadow = v))
                .option(bool("Puls-Animation", "Aktuelle Zeile 'ploppt' beim Wechsel kurz auf.", d.pulse, () -> c.pulse, v -> c.pulse = v))
                .build();

        ConfigCategory colors = ConfigCategory.createBuilder().name(Component.literal("Farben"))
                .option(color("Aktuelle Zeile", d.currentColor, () -> c.currentColor, v -> c.currentColor = v))
                .option(color("Andere Zeilen", d.otherColor, () -> c.otherColor, v -> c.otherColor = v))
                .option(color("Titel/Interpret", d.headerColor, () -> c.headerColor, v -> c.headerColor = v))
                .option(color("Hintergrund", d.backgroundColor, () -> c.backgroundColor, v -> c.backgroundColor = v))
                .option(flt("Hintergrund-Deckkraft", "0 = kein Hintergrund.", d.backgroundAlpha, 0f, 1f, 0.01f, "", () -> c.backgroundAlpha, v -> c.backgroundAlpha = v))
                .option(flt("Deckkraft aktuelle Zeile", "", d.currentOpacity, 0.1f, 1f, 0.01f, "", () -> c.currentOpacity, v -> c.currentOpacity = v))
                .option(flt("Deckkraft andere Zeilen", "", d.otherOpacity, 0.1f, 1f, 0.01f, "", () -> c.otherOpacity, v -> c.otherOpacity = v))
                .option(bool("Regenbogen-Modus", "Aktuelle Zeile wechselt die Farbe.", d.rainbow, () -> c.rainbow, v -> c.rainbow = v))
                .option(flt("Regenbogen-Tempo", "", d.rainbowSpeed, 0.1f, 10f, 0.1f, "x", () -> c.rainbowSpeed, v -> c.rainbowSpeed = v))
                .option(bool("3D: Umrandung", "Farbige Kontur um den 3D-Text.", d.worldOutline, () -> c.worldOutline, v -> c.worldOutline = v))
                .option(color("3D: Umrandungsfarbe", d.outlineColor, () -> c.outlineColor, v -> c.outlineColor = v))
                .build();

        ConfigCategory hud = ConfigCategory.createBuilder().name(Component.literal("HUD (2D)"))
                .option(enumOpt("Position", "Anker am Bildschirmrand.", OverlayConfig.Anchor.class, d.anchor, () -> c.anchor, v -> c.anchor = v))
                .option(intg("Versatz X", "", d.hudOffsetX, -600, 600, " px", () -> c.hudOffsetX, v -> c.hudOffsetX = v))
                .option(intg("Versatz Y", "", d.hudOffsetY, -600, 600, " px", () -> c.hudOffsetY, v -> c.hudOffsetY = v))
                .build();

        ConfigCategory world = ConfigCategory.createBuilder().name(Component.literal("3D-Welt"))
                .option(flt("3D-Groesse", "Groesse des Textes in der Welt.", d.worldScale, 0.2f, 12f, 0.05f, "x", () -> c.worldScale, v -> c.worldScale = v))
                .option(bool("Dem Spieler zugewandt", "Text dreht sich immer zu dir (Billboard).", d.faceViewer, () -> c.faceViewer, v -> c.faceViewer = v))
                .option(flt("Folge-Geschwindigkeit", "Wie schnell der Text deiner Blickrichtung folgt. 0 = starr an deinen Blick gekoppelt.", d.followSpeed, 0f, 20f, 0.5f, "", () -> c.followSpeed, v -> c.followSpeed = v))
                .option(bool("Durch Waende sichtbar", "", d.seeThrough, () -> c.seeThrough, v -> c.seeThrough = v))
                .group(OptionGroup.createBuilder().name(Component.literal("Vor dir / allgemein"))
                        .option(flt("Abstand", "Abstand vor dir (Luft- und Boden-Modus).", d.distance, 0.5f, 30f, 0.1f, " Bl.", () -> c.distance, v -> c.distance = v))
                        .option(flt("Hoehe", "Hoehe relativ zu deinen Augen.", d.heightOffset, -5f, 20f, 0.05f, " Bl.", () -> c.heightOffset, v -> c.heightOffset = v))
                        .option(flt("Seitlich", "Nach links (-) / rechts (+) verschieben.", d.sideOffset, -15f, 15f, 0.1f, " Bl.", () -> c.sideOffset, v -> c.sideOffset = v))
                        .option(flt("Drehung (Yaw)", "Winkel relativ zu deiner Blickrichtung.", d.yawOffset, -180f, 180f, 1f, "\u00B0", () -> c.yawOffset, v -> c.yawOffset = v))
                        .option(flt("Neigung (Tilt)", "Nach vorn/hinten kippen (wenn nicht zugewandt).", d.tilt, -90f, 90f, 1f, "\u00B0", () -> c.tilt, v -> c.tilt = v))
                        .option(flt("Rollen", "Seitlich verdrehen.", d.roll, -180f, 180f, 1f, "\u00B0", () -> c.roll, v -> c.roll = v))
                        .build())
                .group(OptionGroup.createBuilder().name(Component.literal("Himmel"))
                        .option(flt("Himmel-Winkel", "Wie hoch ueber dem Horizont.", d.skyElevation, 5f, 89f, 1f, "\u00B0", () -> c.skyElevation, v -> c.skyElevation = v))
                        .option(flt("Himmel-Entfernung", "", d.skyDistance, 3f, 80f, 0.5f, " Bl.", () -> c.skyDistance, v -> c.skyDistance = v))
                        .build())
                .group(OptionGroup.createBuilder().name(Component.literal("Boden / See"))
                        .option(flt("Hoehe ueber Boden", "Wie weit der Text ueber Boden/Wasser schwebt.", d.groundHeight, -0.5f, 3f, 0.01f, " Bl.", () -> c.groundHeight, v -> c.groundHeight = v))
                        .option(bool("Auf Wasseroberflaeche einrasten", "Liegt der Text ueber einem See, wird er auf die Wasserflaeche gelegt.", d.snapToWater, () -> c.snapToWater, v -> c.snapToWater = v))
                        .build())
                .group(OptionGroup.createBuilder().name(Component.literal("Kreisen"))
                        .option(flt("Radius", "", d.orbitRadius, 1f, 20f, 0.1f, " Bl.", () -> c.orbitRadius, v -> c.orbitRadius = v))
                        .option(flt("Tempo", "Grad pro Sekunde (negativ = andere Richtung).", d.orbitSpeed, -180f, 180f, 1f, "\u00B0/s", () -> c.orbitSpeed, v -> c.orbitSpeed = v))
                        .build())
                .group(OptionGroup.createBuilder().name(Component.literal("Feste Position"))
                        .option(ButtonOption.createBuilder().name(Component.literal("Position hier speichern"))
                                .description(OptionDescription.of(Component.literal("Setzt die Lyrics fest in die Welt, genau vor dich. Schaltet auf 'Fest an gespeicherter Position'.")))
                                .text(Component.literal("Jetzt speichern"))
                                .action((screen, opt) -> FrogisMusicOverlayClient.pinHere()).build())
                        .build())
                .build();

        ConfigCategory sync = ConfigCategory.createBuilder().name(Component.literal("Sync"))
                .option(intg("Zeit-Versatz", "Lyrics frueher (-) oder spaeter (+) anzeigen. Auch per Taste einstellbar.", d.syncOffsetMs, -5000, 5000, " ms", () -> c.syncOffsetMs, v -> c.syncOffsetMs = v))
                .option(bool("Unsynchronisierte Lyrics schaetzen", "Wenn nur Text ohne Zeitstempel existiert, gleichmaessig ueber die Songlaenge verteilen.", d.estimateUnsynced, () -> c.estimateUnsynced, v -> c.estimateUnsynced = v))
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("FrogIsMusicOverlay"))
                .category(general).category(text).category(colors).category(hud).category(world).category(sync)
                .save(OverlayConfig::save)
                .build().generateScreen(parent);
    }

    // ---------- kleine Helfer ----------
    private static Option<Boolean> bool(String n, String desc, boolean def, Supplier<Boolean> g, Consumer<Boolean> s) {
        return Option.<Boolean>createBuilder().name(Component.literal(n)).description(OptionDescription.of(Component.literal(desc)))
                .binding(def, g, s).controller(TickBoxControllerBuilder::create).build();
    }

    private static Option<Float> flt(String n, String desc, float def, float min, float max, float step, String unit, Supplier<Float> g, Consumer<Float> s) {
        return Option.<Float>createBuilder().name(Component.literal(n)).description(OptionDescription.of(Component.literal(desc)))
                .binding(def, g, s)
                .controller(o -> FloatSliderControllerBuilder.create(o).range(min, max).step(step)
                        .formatValue(v -> Component.literal(String.format("%.2f%s", v, unit))))
                .build();
    }

    private static Option<Integer> intg(String n, String desc, int def, int min, int max, String unit, Supplier<Integer> g, Consumer<Integer> s) {
        return Option.<Integer>createBuilder().name(Component.literal(n)).description(OptionDescription.of(Component.literal(desc)))
                .binding(def, g, s)
                .controller(o -> IntegerSliderControllerBuilder.create(o).range(min, max).step(1)
                        .formatValue(v -> Component.literal(v + unit)))
                .build();
    }

    private static <E extends Enum<E> & OverlayConfig.Labeled> Option<E> enumOpt(String n, String desc, Class<E> cls, E def, Supplier<E> g, Consumer<E> s) {
        return Option.<E>createBuilder().name(Component.literal(n)).description(OptionDescription.of(Component.literal(desc)))
                .binding(def, g, s)
                .controller(o -> EnumControllerBuilder.create(o).enumClass(cls).formatValue(v -> Component.literal(v.label())))
                .build();
    }

    private static Option<String> string(String n, String desc, String def, Supplier<String> g, Consumer<String> s) {
        return Option.<String>createBuilder().name(Component.literal(n)).description(OptionDescription.of(Component.literal(desc)))
                .binding(def, g, s).controller(StringControllerBuilder::create).build();
    }

    private static Option<Color> color(String n, int def, Supplier<Integer> g, Consumer<Integer> s) {
        return Option.<Color>createBuilder().name(Component.literal(n))
                .binding(new Color(def), () -> new Color(g.get()), col -> s.accept(col.getRGB() & 0xFFFFFF))
                .controller(ColorControllerBuilder::create).build();
    }
}
