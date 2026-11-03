package dev.frogis.musicoverlay.render;

import dev.frogis.musicoverlay.config.OverlayConfig;
import dev.frogis.musicoverlay.lyrics.LyricsService;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Gemeinsame Logik fuer HUD und 3D: Zeilen bauen, einfaerben, Layout berechnen. */
public final class LyricsStyle {
    private LyricsStyle() { }

    public record Row(Component comp, int width, int color, float scale, float bump) { }
    public record Placed(Row row, float x, float y, float k) { }
    public record Block(List<Placed> items, float w, float h) { }

    public static int argb(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255f), 5, 255); // Alpha <= 3 wuerde von MC als "voll sichtbar" gelesen
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int bgArgb(OverlayConfig c) {
        if (c.backgroundAlpha <= 0.01f) return 0;
        return argb(c.backgroundColor, c.backgroundAlpha);
    }

    private static Component comp(String text, boolean current, OverlayConfig c) {
        Style st = Style.EMPTY;
        Identifier f = c.fontId();
        if (f != null) st = st.withFont(new FontDescription.Resource(f));
        if (current && c.boldCurrent) st = st.withBold(true);
        if (c.italic) st = st.withItalic(true);
        return Component.literal(text).withStyle(st);
    }

    public static List<Row> rows(LyricsService.View v, OverlayConfig c, Font font) {
        List<Row> out = new ArrayList<>();
        long now = Util.getMillis();
        float pauseDim = v.paused() ? 0.6f : 1f;

        if (c.showHeader && !v.header().isBlank()) {
            Component h = comp(v.header(), false, c);
            out.add(mk(h, font, c, argb(c.headerColor, 0.9f * pauseDim), 0.8f, 1f));
        }
        for (LyricsService.Entry e : v.entries()) {
            boolean cur = e.rel() == 0;
            int rgb;
            float alpha;
            if (cur) {
                rgb = c.rainbow ? Mth.hsvToRgb((now * 0.0002f * c.rainbowSpeed) % 1f, 0.55f, 1f) : c.currentColor;
                alpha = c.currentOpacity;
            } else {
                rgb = c.otherColor;
                alpha = c.otherOpacity * Math.max(0.25f, 1f - 0.25f * (Math.abs(e.rel()) - 1));
            }
            float bump = 1f;
            if (cur && c.pulse) {
                float t = Mth.clamp(v.sinceLineMs() / 350f, 0f, 1f);
                bump = 1f + 0.15f * (1f - t) * (1f - t);
            }
            out.add(mk(comp(e.text(), cur, c), font, c, argb(rgb, alpha * pauseDim), 1f, bump));
        }
        return out;
    }

    private static Row mk(Component comp, Font font, OverlayConfig c, int color, float base, float bump) {
        int w = Math.max(1, font.width(comp));
        float k = Math.min(1f, c.maxWidth / (float) w) * base;
        return new Row(comp, w, color, k, bump);
    }

    /** Positioniert alle Zeilen in "Font-Einheiten", Ursprung links oben. */
    public static Block layout(List<Row> rows, Font font, OverlayConfig c) {
        float lineH = font.lineHeight + c.lineSpacing;
        float bw = 1f;
        for (Row r : rows) bw = Math.max(bw, r.width() * r.scale());
        List<Placed> items = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            float w = r.width() * r.scale();
            float x = switch (c.align) { case LEFT -> 0f; case CENTER -> (bw - w) / 2f; case RIGHT -> bw - w; };
            float y = i * lineH;
            float tx = x - w * (r.bump() - 1f) / 2f;
            float ty = y - font.lineHeight * r.scale() * (r.bump() - 1f) / 2f;
            items.add(new Placed(r, tx, ty, r.scale() * r.bump()));
        }
        return new Block(items, bw, rows.size() * lineH - c.lineSpacing);
    }
}
