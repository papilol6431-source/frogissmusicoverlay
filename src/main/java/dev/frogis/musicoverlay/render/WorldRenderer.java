package dev.frogis.musicoverlay.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.frogis.musicoverlay.config.OverlayConfig;
import dev.frogis.musicoverlay.lyrics.LyricsService;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Zeichnet die Lyrics als echten 3D-Text in der Welt. */
public final class WorldRenderer {
    private static float smoothYaw;
    private static boolean init;
    private static long lastNs;

    private WorldRenderer() { }

    private static final int FULL_BRIGHT = 0xF000F0;

    // 26.2-API-RISK: Camera#position()/rotation() und SubmitNodeCollector#submitText(...) -> nur hier verwendet.
    public static void render(PoseStack ps, SubmitNodeCollector collector) {
        OverlayConfig c = OverlayConfig.get();
        if (!c.enabled || c.mode == OverlayConfig.Mode.HUD) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (mc.options.hideGui && c.hideWithGui) return;
        LyricsService.View v = LyricsService.view(c.linesBefore, c.linesAfter);
        if (v == null) return;

        Font font = mc.font;
        List<LyricsStyle.Row> rows = LyricsStyle.rows(v, c, font);
        if (rows.isEmpty()) return;
        LyricsStyle.Block block = LyricsStyle.layout(rows, font, c);

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 cp = cam.position();

        smooth(p.getViewYRot(pt) + c.yawOffset, c.followSpeed);
        double rad = Math.toRadians(smoothYaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);      // Blickrichtung (Minecraft-Yaw)
        double rx = -Math.cos(rad), rz = -Math.sin(rad);     // rechts

        double px, py, pz;
        float faceYaw;
        boolean flat = false;
        boolean billboard = c.faceViewer;

        switch (c.mode) {
            case WORLD_SKY -> {
                double el = Math.toRadians(c.skyElevation);
                px = cp.x + fx * Math.cos(el) * c.skyDistance + rx * c.sideOffset;
                py = cp.y + Math.sin(el) * c.skyDistance + c.heightOffset;
                pz = cp.z + fz * Math.cos(el) * c.skyDistance + rz * c.sideOffset;
                faceYaw = yawFor(px - cp.x, pz - cp.z);
            }
            case WORLD_GROUND -> {
                px = cp.x + fx * c.distance + rx * c.sideOffset;
                pz = cp.z + fz * c.distance + rz * c.sideOffset;
                double feet = Mth.lerp(pt, p.yo, p.getY()) + c.groundHeight;
                py = c.snapToWater ? waterSurface(mc.level, px, pz, feet) + c.groundHeight : feet;
                faceYaw = yawFor(px - cp.x, pz - cp.z);
                flat = true;
            }
            case WORLD_ORBIT -> {
                double a = Math.toRadians((Util.getMillis() / 1000.0) * c.orbitSpeed);
                px = cp.x + Math.cos(a) * c.orbitRadius;
                pz = cp.z + Math.sin(a) * c.orbitRadius;
                py = cp.y + c.heightOffset;
                faceYaw = yawFor(px - cp.x, pz - cp.z);
            }
            case WORLD_PINNED -> {
                if (!c.pinned) return;
                px = c.pinX; py = c.pinY; pz = c.pinZ;
                faceYaw = billboard ? yawFor(px - cp.x, pz - cp.z) : c.pinYaw;
            }
            default -> { // WORLD_FRONT
                px = cp.x + fx * c.distance + rx * c.sideOffset;
                py = cp.y + c.heightOffset;
                pz = cp.z + fz * c.distance + rz * c.sideOffset;
                faceYaw = yawFor(px - cp.x, pz - cp.z);
            }
        }

        ps.pushPose();
        ps.translate(px - cp.x, py - cp.y, pz - cp.z);
        if (flat) {
            ps.mulPose(Axis.YP.rotationDegrees(180f - faceYaw));
            ps.mulPose(Axis.XP.rotationDegrees(-90f + c.tilt));   // flach hinlegen, Textoberkante zeigt vom Spieler weg
        } else if (billboard) {
            ps.mulPose(cam.rotation());
        } else {
            ps.mulPose(Axis.YP.rotationDegrees(180f - faceYaw));
            ps.mulPose(Axis.XP.rotationDegrees(c.tilt));
        }
        ps.mulPose(Axis.ZP.rotationDegrees(c.roll));
        float s = 0.03f * c.worldScale;
        ps.scale(s, -s, s);
        ps.translate(-block.w() / 2f, -block.h() / 2f, 0f);

        int bg = LyricsStyle.bgArgb(c);
        int outline = c.worldOutline ? LyricsStyle.argb(c.outlineColor, 1f) : 0;
        Font.DisplayMode mode = c.seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

        for (LyricsStyle.Placed pl : block.items()) {
            ps.pushPose();
            ps.translate(pl.x(), pl.y(), 0f);
            ps.scale(pl.k(), pl.k(), 1f);
            collector.submitText(ps, 0f, 0f, pl.row().comp().getVisualOrderText(), c.shadow, mode, FULL_BRIGHT,
                    pl.row().color(), bg, outline);
            ps.popPose();
        }
        ps.popPose();
    }

    private static float yawFor(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static void smooth(float target, float speed) {
        long n = System.nanoTime();
        float dt = init ? (n - lastNs) / 1e9f : 1f;
        lastNs = n;
        if (!init || speed <= 0f) { smoothYaw = target; init = true; return; }
        smoothYaw += Mth.wrapDegrees(target - smoothYaw) * Math.min(1f, dt * speed);
    }

    /** Sucht (nahe der Spielerhoehe) die oberste Wasseroberflaeche an dieser XZ-Position. */
    private static double waterSurface(Level level, double x, double z, double fallback) {
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        int start = Mth.floor(fallback) + 2;
        for (int y = start; y > start - 8; y--) {
            bp.set(Mth.floor(x), y, Mth.floor(z));
            FluidState fs = level.getFluidState(bp);
            if (fs.is(FluidTags.WATER) && level.getFluidState(bp.above()).isEmpty()) {
                return y + fs.getOwnHeight();
            }
        }
        return fallback;
    }
}
