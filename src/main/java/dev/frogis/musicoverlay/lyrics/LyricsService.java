package dev.frogis.musicoverlay.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.frogis.musicoverlay.config.OverlayConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Haelt den aktuellen Spotify-Zustand, holt Lyrics von LRCLIB (kostenlos, ohne API-Key) und liefert die Anzeige-Zeilen. */
public final class LyricsService {
    public enum Status { NONE, PLAYING, PAUSED }
    public record Line(long timeMs, String text) { }
    public record Entry(String text, int rel) { }
    public record View(String header, List<Entry> entries, long sinceLineMs, boolean paused) { }

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final Pattern TS = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");

    private static volatile Status status = Status.NONE;
    private static volatile String artist = "", title = "", trackKey = "";
    private static volatile long durationMs, basePosMs, baseNano;
    private static volatile List<Line> lines = List.of();
    private static volatile String message = null;
    private static volatile int generation;

    private LyricsService() { }

    // ----- vom Watcher-Thread aufgerufen -----
    public static synchronized void onState(Status st, String ar, String ti, long pos, long dur) {
        if (st == Status.NONE || ti == null || ti.isBlank()) { status = Status.NONE; return; }
        String key = ar + "\u0000" + ti;
        boolean changed = !key.equals(trackKey);
        long expected = rawPosition();
        if (changed || st != status || Math.abs(pos - expected) > 250) { basePosMs = pos; baseNano = System.nanoTime(); }
        status = st;
        durationMs = dur;
        if (changed) {
            trackKey = key; artist = ar; title = ti;
            lines = List.of();
            message = "Lade Lyrics ...";
            fetch(ar, ti, dur, ++generation);
        }
    }

    private static long rawPosition() {
        return status == Status.PLAYING ? basePosMs + (System.nanoTime() - baseNano) / 1_000_000L : basePosMs;
    }

    // ----- Anzeige -----
    public static View view(int before, int after) {
        OverlayConfig c = OverlayConfig.get();
        if (c.demoMode) return demo(before, after);
        Status st = status;
        if (st == Status.NONE) return null;
        if (st == Status.PAUSED && c.hideWhenPaused) return null;
        String header = artist + " \u2013 " + title;
        List<Line> ls = lines;
        if (ls.isEmpty()) {
            String m = message;
            return new View(header, m == null ? List.of() : List.of(new Entry(m, 0)), 0, st == Status.PAUSED);
        }
        return build(header, ls, rawPosition() + c.syncOffsetMs, before, after, st == Status.PAUSED);
    }

    private static View build(String header, List<Line> ls, long pos, int before, int after, boolean paused) {
        int lo = 0, hi = ls.size() - 1, idx = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (ls.get(mid).timeMs() <= pos) { idx = mid; lo = mid + 1; } else hi = mid - 1;
        }
        List<Entry> out = new ArrayList<>();
        for (int rel = -before; rel <= after; rel++) {
            int i = idx + rel;
            if (i >= ls.size()) continue;
            if (i < 0) { if (rel == 0) out.add(new Entry("\u266A", 0)); continue; }
            String t = ls.get(i).text();
            out.add(new Entry(t.isBlank() ? "\u266A" : t, rel));
        }
        long since = idx >= 0 ? pos - ls.get(idx).timeMs() : pos;
        return new View(header, out, since, paused);
    }

    private static final List<Line> DEMO = new ArrayList<>();
    static {
        String[] t = {"Hoppel durch die Nacht", "Der Frosch hat den Beat", "Quak quak im Mondlicht", "Teich-Disko ist an",
                "Alle Seerosen tanzen", "FrogIsMusicOverlay laeuft!"};
        for (int i = 0; i < t.length; i++) DEMO.add(new Line(i * 3000L, t[i]));
    }

    private static View demo(int before, int after) {
        long pos = System.currentTimeMillis() % (DEMO.size() * 3000L);
        return build("FrogIs \u2013 Demo Song", DEMO, pos, before, after, false);
    }

    // ----- Lyrics holen (LRCLIB) -----
    private static void fetch(String ar, String ti, long dur, int gen) {
        CompletableFuture.runAsync(() -> {
            List<Line> result = List.of();
            String msg = "Keine Lyrics gefunden";
            try {
                JsonObject best = null;
                String cleaned = ti.replaceAll("\\s*[-(\\[].*$", "").trim();
                String[] queries = {
                        "track_name=" + enc(ti) + "&artist_name=" + enc(ar),
                        "q=" + enc(ar + " " + ti),
                        "q=" + enc(ar + " " + (cleaned.isEmpty() ? ti : cleaned))
                };
                for (String q : queries) {
                    best = pick(search(q), dur);
                    if (best != null) break;
                }
                if (best != null) {
                    String synced = str(best, "syncedLyrics"), plain = str(best, "plainLyrics");
                    if (!synced.isBlank()) result = parseLrc(synced);
                    else if (!plain.isBlank() && OverlayConfig.get().estimateUnsynced) result = spread(plain, dur);
                    if (!result.isEmpty()) msg = null;
                }
            } catch (Exception e) {
                msg = "Lyrics-Fehler: " + e.getClass().getSimpleName();
            }
            if (gen == generation) { lines = result; message = msg; }
        });
    }

    private static JsonArray search(String query) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://lrclib.net/api/search?" + query))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "FrogIsMusicOverlay/1.0 (Minecraft Fabric mod)")
                .GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) return new JsonArray();
        JsonElement el = JsonParser.parseString(res.body());
        return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
    }

    private static JsonObject pick(JsonArray arr, long durMs) {
        JsonObject firstSynced = null, firstPlain = null, closest = null;
        double bestDiff = Double.MAX_VALUE;
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            boolean synced = !str(o, "syncedLyrics").isBlank();
            boolean plain = !str(o, "plainLyrics").isBlank();
            if (synced) {
                if (firstSynced == null) firstSynced = o;
                double d = o.has("duration") && !o.get("duration").isJsonNull() ? Math.abs(o.get("duration").getAsDouble() * 1000 - durMs) : 1e9;
                if (d < bestDiff) { bestDiff = d; closest = o; }
            } else if (plain && firstPlain == null) firstPlain = o;
        }
        if (closest != null && bestDiff <= 5000) return closest;
        return firstSynced != null ? firstSynced : firstPlain;
    }

    static List<Line> parseLrc(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = TS.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                long min = Long.parseLong(m.group(1)), sec = Long.parseLong(m.group(2));
                long ms = 0;
                if (m.group(3) != null) {
                    String f = m.group(3);
                    ms = f.length() == 1 ? Long.parseLong(f) * 100 : f.length() == 2 ? Long.parseLong(f) * 10 : Long.parseLong(f);
                }
                times.add(min * 60_000 + sec * 1000 + ms);
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = raw.substring(end).trim();
            for (long t : times) out.add(new Line(t, text));
        }
        out.sort(Comparator.comparingLong(Line::timeMs));
        return out;
    }

    private static List<Line> spread(String plain, long durMs) {
        List<String> ls = new ArrayList<>();
        for (String s : plain.split("\\R")) if (!s.isBlank()) ls.add(s.trim());
        List<Line> out = new ArrayList<>();
        if (ls.isEmpty()) return out;
        long total = durMs > 0 ? durMs : ls.size() * 4000L;
        for (int i = 0; i < ls.size(); i++) out.add(new Line(total * i / ls.size(), ls.get(i)));
        return out;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
}
