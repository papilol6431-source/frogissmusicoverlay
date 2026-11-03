package dev.frogis.musicoverlay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

/** Alle Einstellungen. Wird als JSON in config/frogissmusicoverlay.json gespeichert. */
public class OverlayConfig {

    public interface Labeled { String label(); }

    public enum Mode implements Labeled {
        HUD("2D-HUD (Bildschirm)"),
        WORLD_FRONT("3D: Schwebt vor dir in der Luft"),
        WORLD_SKY("3D: Am Himmel"),
        WORLD_GROUND("3D: Flach auf Boden / Wasser (See)"),
        WORLD_PINNED("3D: Fest an gespeicherter Position"),
        WORLD_ORBIT("3D: Kreist um dich herum");
        private final String l; Mode(String l) { this.l = l; }
        public String label() { return l; }
    }

    public enum Anchor implements Labeled {
        TOP_LEFT("Oben links", 0f, 0f), TOP_CENTER("Oben Mitte", .5f, 0f), TOP_RIGHT("Oben rechts", 1f, 0f),
        CENTER_LEFT("Links Mitte", 0f, .5f), CENTER("Mitte", .5f, .5f), CENTER_RIGHT("Rechts Mitte", 1f, .5f),
        BOTTOM_LEFT("Unten links", 0f, 1f), BOTTOM_CENTER("Unten Mitte", .5f, 1f), BOTTOM_RIGHT("Unten rechts", 1f, 1f);
        private final String l; public final float ax, ay;
        Anchor(String l, float ax, float ay) { this.l = l; this.ax = ax; this.ay = ay; }
        public String label() { return l; }
    }

    public enum Align implements Labeled {
        LEFT("Links"), CENTER("Zentriert"), RIGHT("Rechts");
        private final String l; Align(String l) { this.l = l; }
        public String label() { return l; }
    }

    public enum FontPreset implements Labeled {
        DEFAULT("Minecraft Standard", null),
        UNIFORM("Minecraft Uniform (Unicode)", "minecraft:uniform"),
        GALACTIC("Zaubertisch (Galaktisch)", "minecraft:alt"),
        ILLAGER("Illager-Runen", "minecraft:illageralt"),
        CUSTOM("Eigene Schrift (siehe Feld darunter)", null);
        private final String l; public final String id;
        FontPreset(String l, String id) { this.l = l; this.id = id; }
        public String label() { return l; }
    }

    // ---------- Allgemein ----------
    public boolean enabled = true;
    public Mode mode = Mode.HUD;
    public boolean demoMode = false;
    public boolean hideWhenPaused = false;
    public boolean hideWithGui = true;
    public boolean showHeader = true;

    // ---------- Sync ----------
    public int syncOffsetMs = 0;
    public boolean estimateUnsynced = true;

    // ---------- Text ----------
    public FontPreset fontPreset = FontPreset.DEFAULT;
    public String customFont = "";
    public float scale = 1.0f;
    public int linesBefore = 1;
    public int linesAfter = 2;
    public int lineSpacing = 4;
    public Align align = Align.CENTER;
    public int maxWidth = 240;
    public boolean boldCurrent = true;
    public boolean italic = false;
    public boolean shadow = true;
    public boolean worldOutline = false;

    // ---------- Farben ----------
    public int currentColor = 0x7CFF6B;
    public int otherColor = 0xBBBBBB;
    public int headerColor = 0xFFFFFF;
    public int outlineColor = 0x000000;
    public int backgroundColor = 0x000000;
    public float backgroundAlpha = 0.30f;
    public float currentOpacity = 1.0f;
    public float otherOpacity = 0.60f;
    public boolean rainbow = false;
    public float rainbowSpeed = 1.0f;
    public boolean pulse = true;

    // ---------- HUD ----------
    public Anchor anchor = Anchor.TOP_CENTER;
    public int hudOffsetX = 0;
    public int hudOffsetY = 30;

    // ---------- 3D ----------
    public float worldScale = 1.6f;
    public float distance = 3.5f;
    public float heightOffset = 0.3f;
    public float sideOffset = 0f;
    public float yawOffset = 0f;
    public float tilt = 0f;
    public float roll = 0f;
    public boolean faceViewer = true;
    public float followSpeed = 6f;
    public boolean seeThrough = false;
    public float skyElevation = 55f;
    public float skyDistance = 18f;
    public float groundHeight = 0.05f;
    public boolean snapToWater = true;
    public float orbitRadius = 4f;
    public float orbitSpeed = 20f;
    public boolean pinned = false;
    public double pinX, pinY, pinZ;
    public float pinYaw;

    /** null = Standardschrift */
    public Identifier fontId() {
        if (fontPreset == FontPreset.CUSTOM) return Identifier.tryParse(customFont == null ? "" : customFont.trim());
        return fontPreset.id == null ? null : Identifier.tryParse(fontPreset.id);
    }

    // ---------- Laden / Speichern ----------
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("frogissmusicoverlay.json"); }
    private static OverlayConfig instance;

    public static OverlayConfig get() {
        if (instance == null) {
            try {
                Path p = path();
                instance = Files.exists(p) ? GSON.fromJson(Files.readString(p), OverlayConfig.class) : new OverlayConfig();
            } catch (Exception e) { instance = new OverlayConfig(); }
            if (instance == null) instance = new OverlayConfig();
        }
        return instance;
    }

    public static void save() {
        try { Files.writeString(path(), GSON.toJson(get())); } catch (Exception ignored) { }
    }
}
