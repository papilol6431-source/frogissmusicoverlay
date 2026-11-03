package dev.frogis.musicoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import dev.frogis.musicoverlay.config.ConfigScreen;
import dev.frogis.musicoverlay.config.OverlayConfig;
import dev.frogis.musicoverlay.lyrics.SpotifyWatcher;
import dev.frogis.musicoverlay.render.HudRenderer;
import dev.frogis.musicoverlay.render.WorldRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class FrogisMusicOverlayClient implements ClientModInitializer {
    public static final String MOD_ID = "frogissmusicoverlay";

    private static KeyMapping OPEN, TOGGLE, EARLIER, LATER, PIN;

    @Override
    public void onInitializeClient() {
        OverlayConfig.get();
        SpotifyWatcher.start();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        OPEN    = reg("key.frogissmusicoverlay.open",    GLFW.GLFW_KEY_J, cat);
        TOGGLE  = reg("key.frogissmusicoverlay.toggle",  GLFW.GLFW_KEY_UNKNOWN, cat);
        EARLIER = reg("key.frogissmusicoverlay.earlier", GLFW.GLFW_KEY_UNKNOWN, cat);
        LATER   = reg("key.frogissmusicoverlay.later",   GLFW.GLFW_KEY_UNKNOWN, cat);
        PIN     = reg("key.frogissmusicoverlay.pin",     GLFW.GLFW_KEY_UNKNOWN, cat);

        // 2D-HUD
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"), HudRenderer::extract);
        // 3D-Welt  (26.2-API-RISK: Event-Name)
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> WorldRenderer.render(ctx.poseStack(), ctx.submitNodeCollector()));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            OverlayConfig c = OverlayConfig.get();
            while (OPEN.consumeClick()) mc.gui.setScreen(ConfigScreen.create(mc.screen));
            while (TOGGLE.consumeClick()) { c.enabled = !c.enabled; OverlayConfig.save(); msg(mc, c.enabled ? "Lyrics an" : "Lyrics aus"); }
            while (EARLIER.consumeClick()) { c.syncOffsetMs -= 250; OverlayConfig.save(); msg(mc, "Lyrics-Versatz: " + c.syncOffsetMs + " ms"); }
            while (LATER.consumeClick()) { c.syncOffsetMs += 250; OverlayConfig.save(); msg(mc, "Lyrics-Versatz: " + c.syncOffsetMs + " ms"); }
            while (PIN.consumeClick()) pinHere();
        });
    }

    private static KeyMapping reg(String name, int key, KeyMapping.Category cat) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, key, cat));
    }

    private static void msg(Minecraft mc, String text) {
        mc.gui.setOverlayMessage(Component.literal(text), false);
    }

    /** Speichert die Position ca. vor dem Spieler und schaltet auf "Fest in der Welt". */
    public static void pinHere() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        OverlayConfig c = OverlayConfig.get();
        float yaw = mc.player.getYRot();
        double rad = Math.toRadians(yaw);
        Vec3 eye = mc.player.getEyePosition();
        c.pinX = eye.x - Math.sin(rad) * c.distance;
        c.pinY = eye.y + c.heightOffset;
        c.pinZ = eye.z + Math.cos(rad) * c.distance;
        c.pinYaw = yaw;
        c.pinned = true;
        c.mode = OverlayConfig.Mode.WORLD_PINNED;
        OverlayConfig.save();
        msg(mc, "Lyrics-Position gespeichert");
    }
}
