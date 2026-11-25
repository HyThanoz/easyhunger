package com.haas.easyhunger.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Validation of anything a player can type or send from the client (commands, UI fields).
 *
 * Every method returns null (or -1 / "") instead of throwing, so callers only have to check one
 * value. Numbers are matched against a strict pattern BEFORE parsing: that rules out "NaN",
 * "Infinity", hex, exponents and very long strings, which Float.parseFloat would accept.
 */
public final class Entrada {

    /** Up to 15 integer digits and 9 decimals; "." or "," as decimal separator. */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d{1,15}([.,]\\d{1,9})?");
    private static final Pattern ENTERO = Pattern.compile("-?\\d{1,18}");

    private Entrada() {}

    /** Parses a decimal number between min and max (both included), or returns null. */
    public static Double decimal(String texto, double min, double max) {
        if (texto == null) return null;
        String t = texto.trim();
        if (!DECIMAL.matcher(t).matches()) return null;
        double v = Double.parseDouble(t.replace(',', '.'));
        return finitoEntre(v, min, max) ? v : null;
    }

    /** Parses a whole number between min and max (both included), or returns null. */
    public static Long entero(String texto, long min, long max) {
        if (texto == null) return null;
        String t = texto.trim();
        if (!ENTERO.matcher(t).matches()) return null;
        long v;
        try {
            v = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null; // 18 digits can still overflow a long
        }
        return v >= min && v <= max ? v : null;
    }

    /** True if v is a real number (not NaN or infinite) between min and max. */
    public static boolean finitoEntre(double v, double min, double max) {
        return Double.isFinite(v) && v >= min && v <= max;
    }

    /**
     * Free text cut to maxLargo code points, without control characters or the Unicode
     * bidirectional overrides (U+202A..U+202E, U+2066..U+2069) that can disguise text.
     */
    public static String texto(String texto, int maxLargo) {
        if (texto == null || maxLargo <= 0) return "";
        StringBuilder out = new StringBuilder(Math.min(texto.length(), maxLargo));
        int i = 0;
        while (i < texto.length() && out.codePointCount(0, out.length()) < maxLargo) {
            int cp = texto.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isISOControl(cp)) continue;
            if ((cp >= 0x202A && cp <= 0x202E) || (cp >= 0x2066 && cp <= 0x2069)) continue;
            out.appendCodePoint(cp);
        }
        return out.toString().trim();
    }

    /** Index in 0..tamano-1, or -1 if the text is not a valid index. */
    public static int indice(String texto, int tamano) {
        Long v = entero(texto, 0, tamano - 1L);
        return v == null ? -1 : v.intValue();
    }

    /**
     * Per-player cooldown. Thread safe (commands run off the world thread). The map is pruned
     * when it grows past 2048 entries; callers should also call {@link #olvidar} on disconnect.
     */
    public static final class Enfriamiento {
        private final long nanos;
        private final Map<UUID, Long> ultimo = new ConcurrentHashMap<>();

        public Enfriamiento(long milisegundos) {
            this.nanos = Math.max(0L, milisegundos) * 1_000_000L;
        }

        /** True (and starts the cooldown) if the player may act now. */
        public boolean intentar(UUID quien) {
            if (quien == null) return false;
            long ahora = System.nanoTime();
            if (ultimo.size() > 2048) podar(ahora);
            boolean[] ok = {false};
            ultimo.compute(quien, (k, antes) -> {
                if (antes == null || ahora - antes >= nanos) {
                    ok[0] = true;
                    return ahora;
                }
                return antes;
            });
            return ok[0];
        }

        /** Milliseconds left until the player may act again (0 if none). */
        public long restanteMs(UUID quien) {
            Long antes = quien == null ? null : ultimo.get(quien);
            if (antes == null) return 0L;
            long falta = nanos - (System.nanoTime() - antes);
            return falta <= 0L ? 0L : (falta + 999_999L) / 1_000_000L;
        }

        public void olvidar(UUID quien) {
            if (quien != null) ultimo.remove(quien);
        }

        private void podar(long ahora) {
            ultimo.entrySet().removeIf(e -> ahora - e.getValue() >= nanos);
        }
    }
}
