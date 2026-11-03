package dev.frogis.musicoverlay.render;

import dev.frogis.musicoverlay.config.OverlayConfig;
import dev.frogis.musicoverlay.lyrics.LyricsService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public final class HudRenderer {
    private HudRenderer() { }

    // 26.2-API-RISK: GuiGraphicsExtractor#text / #fill / #pose koennen leicht anders heissen -> hier ist die einzige Stelle.
    public static void extract(GuiGraphicsExtractor g, DeltaTracker dt) {
        OverlayConfig c = OverlayConfig.get();
        if (!c.enabled || c.mode != OverlayConfig.Mode.HUD) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui && c.hideWithGui) return;
        LyricsService.View v = LyricsService.view(c.linesBefore, c.linesAfter);
        if (v == null) return;

        Font font = mc.font;
        List<LyricsStyle.Row> rows = LyricsStyle.rows(v, c, font);
        if (rows.isEmpty()) return;
        LyricsStyle.Block block = LyricsStyle.layout(rows, font, c);

        float s = c.scale;
        float sw = block.w() * s, sh = block.h() * s;
        int screenW = mc.getWindow().getGuiScaledWidth(), screenH = mc.getWindow().getGuiScaledHeight();
        float x0 = c.anchor.ax * (screenW - sw) + c.hudOffsetX;
        float y0 = c.anchor.ay * (screenH - sh) + c.hudOffsetY;

        int bg = LyricsStyle.bgArgb(c);
        if (bg != 0) {
            float padX = 5 * s, padY = 3 * s;
            g.fill((int) (x0 - padX), (int) (y0 - padY), (int) (x0 + sw + padX), (int) (y0 + sh + padY), bg);
        }

        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        g.pose().scale(s, s);
        for (LyricsStyle.Placed p : block.items()) {
            g.pose().pushMatrix();
            g.pose().translate(p.x(), p.y());
            g.pose().scale(p.k(), p.k());
            g.text(font, p.row().comp(), 0, 0, p.row().color(), c.shadow);
            g.pose().popMatrix();
        }
        g.pose().popMatrix();
    }
}
