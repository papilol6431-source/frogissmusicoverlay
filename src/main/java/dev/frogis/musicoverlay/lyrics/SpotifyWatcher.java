package dev.frogis.musicoverlay.lyrics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Liest Titel/Interpret/Position von Spotify.
 * Windows: ueber die Windows-Medien-Session (PowerShell, kein Login / API-Key noetig).
 * Linux:   ueber playerctl.
 */
public final class SpotifyWatcher {
    private static volatile Process proc;
    private static boolean started;

    private SpotifyWatcher() { }

    public static synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(SpotifyWatcher::loop, "FrogIsMusic-Spotify");
        t.setDaemon(true);
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { Process p = proc; if (p != null) p.destroyForcibly(); }));
    }

    private static void loop() {
        String os = System.getProperty("os.name", "").toLowerCase();
        while (true) {
            try {
                ProcessBuilder pb;
                boolean micro = false;
                if (os.contains("win")) {
                    Path script = Files.createTempFile("frogis_spotify", ".ps1");
                    try (InputStream in = SpotifyWatcher.class.getResourceAsStream("/assets/frogissmusicoverlay/spotify.ps1")) {
                        Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
                    }
                    script.toFile().deleteOnExit();
                    pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.toString());
                } else if (os.contains("linux")) {
                    micro = true;
                    pb = new ProcessBuilder("sh", "-c",
                            "while true; do playerctl -p spotify metadata --format '{{status}}\t{{artist}}\t{{title}}\t{{album}}\t{{position}}\t{{mpris:length}}' 2>/dev/null || echo NONE; sleep 0.4; done");
                } else {
                    return; // macOS: nicht unterstuetzt
                }
                pb.redirectErrorStream(true);
                proc = pb.start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) handle(line, micro);
                }
            } catch (Exception ignored) { }
            LyricsService.onState(LyricsService.Status.NONE, "", "", 0, 0);
            try { Thread.sleep(5000); } catch (InterruptedException e) { return; }
        }
    }

    private static void handle(String line, boolean micro) {
        String[] f = line.split("\t", -1);
        if (f.length < 6) { LyricsService.onState(LyricsService.Status.NONE, "", "", 0, 0); return; }
        LyricsService.Status st = switch (f[0].trim()) {
            case "Playing" -> LyricsService.Status.PLAYING;
            case "Paused" -> LyricsService.Status.PAUSED;
            default -> LyricsService.Status.NONE;
        };
        try {
            long pos = (long) Double.parseDouble(f[4]), dur = (long) Double.parseDouble(f[5]);
            if (micro) { pos /= 1000; dur /= 1000; }
            LyricsService.onState(st, f[1].trim(), f[2].trim(), pos, dur);
        } catch (NumberFormatException e) {
            LyricsService.onState(st, f[1].trim(), f[2].trim(), 0, 0);
        }
    }
}
