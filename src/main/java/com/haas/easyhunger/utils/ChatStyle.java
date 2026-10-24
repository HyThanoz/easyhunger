package com.haas.easyhunger.utils;

// ---- MYTALE ChatStyle v1 BEGIN (pieza comun de MyTale) ----
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

/**
 * Formato unico del chat de los mods es.mytale. La base es EssentialsPlus (announcements.json):
 * prefijo [!] verde, cuerpo gris, enfasis rojo, valores y enlaces amarillo. Sin negrita en los
 * mensajes: el enfasis va por color. La paleta de la UI (#FFC233 y compania) no va en el chat.
 *
 * <p>Message es MUTABLE (color/bold/insert modifican this): nunca guardes un Message en una
 * constante; cada metodo de aqui devuelve una instancia nueva.
 */
public final class ChatStyle {

    public static final String BRAND = "#55FF55";
    public static final String PREFIX_INFO = BRAND;
    public static final String PREFIX_ANNOUNCE = BRAND;
    public static final String PREFIX_ERROR = "#FF5555";
    public static final String BODY = "#AAAAAA";
    public static final String EMPHASIS = "#FF5555";
    public static final String VALUE = "#FFFF55";
    public static final String PLAYER = "#FFFF55";
    public static final String LINK = "#FFFF55";
    public static final String MUTED = "#7F7F7F";
    public static final String HEADER = "#FFAA00";

    private static final String TAG = "[!] ";

    private ChatStyle() {}

    // ---- Segmentos ----

    public static Message body(String text) { return Message.raw(text).color(BODY); }

    public static Message emphasis(String text) { return Message.raw(text).color(EMPHASIS); }

    public static Message value(Object value) { return Message.raw(String.valueOf(value)).color(VALUE); }

    public static Message player(String name) { return Message.raw(name).color(PLAYER); }

    public static Message muted(String text) { return Message.raw(text).color(MUTED); }

    public static Message link(String text, String url) { return Message.raw(text).color(LINK).link(url); }

    // ---- Lineas ----

    /** [!] verde + cuerpo gris. Sirve para avisos y confirmaciones. */
    public static Message info(String text) { return info(body(text)); }

    public static Message info(Message... parts) { return line(PREFIX_INFO, parts); }

    /** [!] rojo + cuerpo gris. */
    public static Message error(String text) { return error(body(text)); }

    public static Message error(Message... parts) { return line(PREFIX_ERROR, parts); }

    /** Anuncio automatico de un mod a todo el servidor. Los del staff van por /broadcast. */
    public static Message announce(Message... parts) { return line(PREFIX_ANNOUNCE, parts); }

    /** Linea sin prefijo: continuaciones y filas de una lista. */
    public static Message plain(Message... parts) { return Message.join(parts); }

    /** Cabecera de lista "=== Titulo ===". Es la unica negrita permitida (asi las pinta EssentialsPlus). */
    public static Message header(String title) {
        return Message.raw("=== " + title + " ===").color(HEADER).bold(true);
    }

    /** A todos los conectados. Si falla un jugador, el resto lo recibe igual. */
    public static void broadcast(Message message) {
        Universe universe = Universe.get();
        if (universe == null) return;
        for (PlayerRef player : universe.getPlayers()) {
            try {
                player.sendMessage(message);
            } catch (RuntimeException ignored) {
                // un jugador a medio desconectar no corta el aviso a los demas
            }
        }
    }

    private static Message line(String prefixColor, Message... parts) {
        Message[] all = new Message[parts.length + 1];
        all[0] = Message.raw(TAG).color(prefixColor);
        System.arraycopy(parts, 0, all, 1, parts.length);
        return Message.join(all);
    }
}
// ---- MYTALE ChatStyle v1 END ----
