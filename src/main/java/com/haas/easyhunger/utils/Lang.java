package com.haas.easyhunger.utils;

// ---- MYTALE Lang v2 BEGIN (pieza comun de MyTale) ----
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Textos de jugador en tres idiomas: es (por defecto), en y pt (portugues de Brasil).
 *
 * <p>Los textos viven en el jar del mod: {@code lang/es.json}, {@code lang/en.json},
 * {@code lang/pt.json}. Cada uno es un objeto JSON plano, clave a texto, con parametros
 * {@code {nombre}}. {@code {nombre:n}} pinta un numero con separador de miles del idioma
 * (1.800 en es/pt, 1,800 en en). Si falta la clave o el archivo, sale el espanol, y el log lo
 * dice UNA vez por clave.
 *
 * <p>Como se decide el idioma de un jugador:
 * <ol>
 *   <li>Lo que eligio en MytaleComunidad (Mi perfil). Se pide por reflexion a
 *       {@code es.mytale.comunidad.api.ComunidadAPI.language(java.util.UUID)}, que devuelve
 *       "es" | "en" | "pt" | null. Una eleccion explicita manda siempre.</li>
 *   <li>Si no eligio (null, o no hay Comunidad): el idioma de los ajustes de su cliente,
 *       {@code PlayerRef.getLanguage()} ("es-ES", "en-US", "pt-BR"...). El cliente avisa al
 *       servidor cuando el jugador lo cambia (UpdateLanguage).</li>
 *   <li>Si el cliente esta en otro idioma (fr, ru, zh...), o el jugador no esta conectado, o es
 *       la consola: espanol.</li>
 * </ol>
 * La respuesta se cachea {@link #CACHE_MS} por jugador. Si el jugador aun no esta en el
 * Universe (justo al entrar), no se cachea, para no dejarlo 30 s en espanol.
 *
 * <p>Marcas de color para el chat (solo las lee {@link #spans}; {@link #t} las quita):
 * {@code <v>valor</v>}, {@code <e>enfasis</e>}, {@code <m>apagado</m>}, {@code <p>jugador</p>}.
 * Sin anidar. El texto fuera de marcas es cuerpo. Las marcas se leen ANTES de poner los
 * parametros, asi que un nombre de jugador con "<v>" dentro no pinta nada.
 *
 * <p>Seguro desde cualquier hilo. Nunca lanza: lo peor que devuelve es la clave misma.
 */
public final class Lang {

    public static final String ES = "es";
    public static final String EN = "en";
    public static final String PT = "pt";
    /** El idioma de quien no eligio, de la consola y de lo que falte. */
    public static final String DEFAULT = ES;

    /** Clase y metodo que MytaleComunidad tiene que exponer. */
    public static final String API_CLASS = "es.mytale.comunidad.api.ComunidadAPI";
    public static final String API_METHOD = "language";

    /** Cuanto vale la respuesta de Comunidad para un jugador. Cambiar de idioma tarda esto. */
    public static final long CACHE_MS = 30_000L;
    /** Si Comunidad no esta (o carga despues), cada cuanto se vuelve a buscar el metodo. */
    private static final long RETRY_LOOKUP_MS = 60_000L;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    /** El logger ya pone [Lang]; el paquete dice de que mod es la copia. */
    private static final String TAG = "(" + Lang.class.getPackageName() + ") ";
    private static final String[] LANGS = {ES, EN, PT};

    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static volatile Method apiMethod;
    private static volatile long nextLookupMs;

    private Lang() {}

    /** Un trozo de linea de chat. style: 'b' cuerpo, 'v' valor, 'e' enfasis, 'm' apagado, 'p' jugador. */
    public record Span(char style, String text) {}

    private record Cached(String lang, long expiresMs) {}

    /** Las tablas se cargan la primera vez que se usa Lang, del jar del propio mod. */
    private static final class Tables {
        static final Map<String, Map<String, String>> BY_LANG = loadAll();
    }

    // ---- API ----

    /** Texto para un jugador, sin marcas de color. Para la UI y para lo que no va al chat. */
    public static String t(UUID who, String key, Object... args) {
        return plain(spansIn(language(who), key, args));
    }

    public static String t(PlayerRef who, String key, Object... args) {
        return t(who == null ? null : who.getUuid(), key, args);
    }

    /** Texto en un idioma concreto ("es", "en", "pt"). Lo desconocido cae a espanol. */
    public static String tIn(String lang, String key, Object... args) {
        return plain(spansIn(normalize(lang), key, args));
    }

    /** Texto partido en trozos de color, para montar la linea con ChatStyle. */
    public static List<Span> spans(UUID who, String key, Object... args) {
        return spansIn(language(who), key, args);
    }

    public static List<Span> spans(PlayerRef who, String key, Object... args) {
        return spans(who == null ? null : who.getUuid(), key, args);
    }

    public static List<Span> spansIn(String lang, String key, Object... args) {
        try {
            String l = normalize(lang);
            String raw = template(l, key);
            List<Span> out = new ArrayList<>();
            for (Span s : parse(raw)) {
                String text = fill(s.text(), l, args);
                if (!text.isEmpty()) out.add(new Span(s.style(), text));
            }
            return out;
        } catch (RuntimeException e) {
            warnOnce("fail:" + key, "No se pudo montar el texto '" + key + "': " + e);
            return List.of(new Span('b', key == null ? "" : key));
        }
    }

    /** "es", "en" o "pt". null (consola, desconocido) es espanol. */
    public static String language(UUID who) {
        if (who == null) return DEFAULT;
        long now = System.currentTimeMillis();
        Cached c = CACHE.get(who);
        if (c != null && c.expiresMs() > now) return c.lang();
        String chosen = askComunidad(who);
        if (chosen != null) {
            String lang = normalize(chosen);
            CACHE.put(who, new Cached(lang, now + CACHE_MS));
            return lang;
        }
        String client = clientLanguage(who);
        if (client == null) return DEFAULT;          // aun no esta conectado: sin cache
        String lang = normalize(client);
        CACHE.put(who, new Cached(lang, now + CACHE_MS));
        return lang;
    }

    /**
     * El idioma de los ajustes del cliente tal como llega ("es-ES", "en-US", "pt-BR"...), o null si
     * el jugador no esta conectado. Sin normalizar: para mostrar "Automatico (English)" en Mi perfil
     * usa {@code normalize(clientLanguage(uuid))}. Barato (un mapa concurrente) y nunca lanza.
     */
    public static String clientLanguage(UUID who) {
        if (who == null) return null;
        try {
            Universe u = Universe.get();
            if (u == null) return null;
            PlayerRef p = u.getPlayer(who);
            if (p == null) return null;
            String l = p.getLanguage();
            return l == null || l.isBlank() ? null : l;
        } catch (Throwable e) {
            warnOnce("cliente", "No se pudo leer el idioma del cliente; ese jugador va en espanol: " + e);
            return null;
        }
    }

    /** Olvida la cache de un jugador (al desconectarse, o si se sabe que cambio de idioma). */
    public static void forget(UUID who) {
        if (who != null) CACHE.remove(who);
    }

    public static void forgetAll() {
        CACHE.clear();
    }

    /** "pt-BR", "PT", "pt_br" -> "pt". Lo que no sea es/en/pt -> "es". */
    public static String normalize(String lang) {
        if (lang == null) return DEFAULT;
        String s = lang.trim().toLowerCase(Locale.ROOT);
        int cut = s.indexOf('-');
        if (cut < 0) cut = s.indexOf('_');
        if (cut > 0) s = s.substring(0, cut);
        for (String l : LANGS) if (l.equals(s)) return l;
        return DEFAULT;
    }

    /** Numero con separador de miles del idioma: 1.800 (es, pt), 1,800 (en). */
    public static String number(String lang, long value) {
        char sep = EN.equals(normalize(lang)) ? ',' : '.';
        String digits = Long.toString(Math.abs(value));
        if (value == Long.MIN_VALUE) digits = digits.substring(1);
        StringBuilder sb = new StringBuilder(digits.length() + digits.length() / 3 + 1);
        if (value < 0) sb.append('-');
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) sb.append(sep);
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    /** true si la clave existe en espanol. Para arneses y comprobaciones. */
    public static boolean has(String key) {
        Map<String, String> es = Tables.BY_LANG.get(ES);
        return es != null && es.containsKey(key);
    }

    /** Las claves de un idioma, para arneses que comparan es/en/pt. */
    public static Set<String> keys(String lang) {
        Map<String, String> m = Tables.BY_LANG.get(normalize(lang));
        return m == null ? Set.of() : Collections.unmodifiableSet(m.keySet());
    }

    // ---- Comunidad ----

    private static String askComunidad(UUID who) {
        Method m = apiMethod;
        if (m == null) {
            long now = System.currentTimeMillis();
            if (now < nextLookupMs) return null;
            nextLookupMs = now + RETRY_LOOKUP_MS;
            try {
                Class<?> api = Class.forName(API_CLASS);
                Method found = api.getMethod(API_METHOD, UUID.class);
                if (!Modifier.isStatic(found.getModifiers()) || found.getReturnType() != String.class) {
                    warnOnce("api:firma", "ComunidadAPI.language existe pero no es static String(UUID); todo en espanol");
                    return null;
                }
                apiMethod = m = found;
                LOGGER.at(Level.INFO).log(TAG + "idioma por jugador conectado a MytaleComunidad");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                warnOnce("api:falta", "MytaleComunidad no expone ComunidadAPI.language(UUID): todo en espanol"
                        + " (se reintenta cada " + (RETRY_LOOKUP_MS / 1000) + " s)");
                return null;
            } catch (Throwable e) {
                warnOnce("api:error", "No se pudo enlazar ComunidadAPI.language: " + e);
                return null;
            }
        }
        try {
            Object r = m.invoke(null, who);
            return r instanceof String s ? s : null;
        } catch (Throwable e) {
            warnOnce("api:invoke", "ComunidadAPI.language fallo; ese jugador va en espanol: " + e);
            return null;
        }
    }

    // ---- Plantillas ----

    private static String template(String lang, String key) {
        if (key == null) return "";
        Map<String, Map<String, String>> all = Tables.BY_LANG;
        Map<String, String> table = all.get(lang);
        if (table != null) {
            String v = table.get(key);
            if (v != null) return v;
            if (!ES.equals(lang)) warnOnce("key:" + lang + ":" + key, "Falta '" + key + "' en lang/" + lang + ".json; sale en espanol");
        }
        Map<String, String> es = all.get(ES);
        String v = es == null ? null : es.get(key);
        if (v != null) return v;
        warnOnce("key:es:" + key, "Falta '" + key + "' en lang/es.json; se ve la clave");
        return key;
    }

    /** Parte por marcas <v> <e> <m> <p>. Lo que no cierre se queda como texto. */
    private static List<Span> parse(String raw) {
        List<Span> out = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '<' && i + 2 < raw.length() && raw.charAt(i + 2) == '>' && "vemp".indexOf(raw.charAt(i + 1)) >= 0) {
                char style = raw.charAt(i + 1);
                String close = "</" + style + ">";
                int end = raw.indexOf(close, i + 3);
                if (end >= 0) {
                    if (body.length() > 0) { out.add(new Span('b', body.toString())); body.setLength(0); }
                    out.add(new Span(style, raw.substring(i + 3, end)));
                    i = end + close.length();
                    continue;
                }
            }
            body.append(c);
            i++;
        }
        if (body.length() > 0) out.add(new Span('b', body.toString()));
        return out;
    }

    /** Pone los parametros. args va por pares: "nombre", valor, "otro", valor... */
    private static String fill(String text, String lang, Object... args) {
        if (text.indexOf('{') < 0 || args == null || args.length < 2) return text;
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int end = c == '{' ? text.indexOf('}', i + 1) : -1;
            if (end > i + 1) {
                String name = text.substring(i + 1, end);
                boolean asNumber = name.endsWith(":n");
                if (asNumber) name = name.substring(0, name.length() - 2);
                Object value = arg(name, args);
                if (value != NONE) {
                    if (asNumber && value instanceof Number n) sb.append(number(lang, n.longValue()));
                    else sb.append(String.valueOf(value));
                    i = end + 1;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static final Object NONE = new Object();

    private static Object arg(String name, Object... args) {
        for (int k = 0; k + 1 < args.length; k += 2) {
            if (name.equals(String.valueOf(args[k]))) return args[k + 1];
        }
        return NONE;
    }

    private static String plain(List<Span> spans) {
        if (spans.size() == 1) return spans.get(0).text();
        StringBuilder sb = new StringBuilder();
        for (Span s : spans) sb.append(s.text());
        return sb.toString();
    }

    // ---- Carga ----

    private static Map<String, Map<String, String>> loadAll() {
        Map<String, Map<String, String>> out = new HashMap<>();
        for (String l : LANGS) {
            Map<String, String> m = loadOne("lang/" + l + ".json");
            if (m != null) out.put(l, m);
            else warnOnce("file:" + l, "No hay lang/" + l + ".json en el jar; ese idioma sale en espanol");
        }
        return Collections.unmodifiableMap(out);
    }

    /** Solo el jar de ESTE mod: getResource() podria devolver el lang/es.json de otro plugin. */
    private static Map<String, String> loadOne(String path) {
        try {
            ClassLoader cl = Lang.class.getClassLoader();
            URL url = cl instanceof URLClassLoader u ? u.findResource(path) : cl.getResource(path);
            if (url == null) return null;
            byte[] bytes;
            try (InputStream in = url.openStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                in.transferTo(buf);
                bytes = buf.toByteArray();
            }
            Map<String, String> m = new Json(new String(bytes, StandardCharsets.UTF_8)).object();
            return Collections.unmodifiableMap(m);
        } catch (Throwable e) {
            LOGGER.at(Level.SEVERE).withCause(e).log(TAG + "no se pudo leer " + path + "; ese idioma sale en espanol");
            return null;
        }
    }

    private static void warnOnce(String id, String msg) {
        if (WARNED.add(id)) LOGGER.at(Level.WARNING).log(TAG + msg);
    }

    /** Lector de JSON minimo: un objeto plano de cadenas. Solo JDK, sin Gson. */
    private static final class Json {
        private final String s;
        private int i;

        Json(String s) {
            this.s = s.startsWith("﻿") ? s.substring(1) : s;
        }

        Map<String, String> object() {
            Map<String, String> out = new java.util.LinkedHashMap<>();
            ws();
            expect('{');
            ws();
            if (peek() == '}') { i++; return out; }
            while (true) {
                ws();
                String k = string();
                ws();
                expect(':');
                ws();
                String v = string();
                if (out.put(k, v) != null) throw new IllegalArgumentException("clave repetida '" + k + "'");
                ws();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("se esperaba , o } en " + (i - 1));
            }
            ws();
            if (i < s.length()) throw new IllegalArgumentException("sobra texto en " + i);
            return out;
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = next();
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw new IllegalArgumentException("\\u cortado");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw new IllegalArgumentException("escape \\" + e + " en " + i);
                }
            }
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private char peek() {
            return i < s.length() ? s.charAt(i) : '\0';
        }

        private char next() {
            if (i >= s.length()) throw new IllegalArgumentException("fin inesperado");
            return s.charAt(i++);
        }

        private void expect(char c) {
            if (next() != c) throw new IllegalArgumentException("se esperaba '" + c + "' en " + (i - 1));
        }
    }
}
// ---- MYTALE Lang v2 END ----
