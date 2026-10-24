package com.haas.easyhunger.utils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.List;
import java.util.UUID;

/**
 * Los mensajes de chat de EasyHunger con idioma. Es una fachada: el texto sale de
 * lang/&lt;idioma&gt;.json del jar ({@link Lang}) y el formato lo pone {@link ChatStyle}.
 *
 * Las marcas &lt;v&gt; &lt;e&gt; &lt;m&gt; &lt;p&gt; del json pasan a value/emphasis/muted/player de
 * ChatStyle, y el resto a body. En espanol la linea queda igual, trozo a trozo, que la que se
 * montaba a mano.
 */
public final class Msg {

    private Msg() {
    }

    /** [!] verde + el texto de la clave en el idioma de quien lo recibe. */
    public static Message info(UUID quien, String clave, Object... args) {
        return ChatStyle.info(partes(quien, clave, args));
    }

    /** [!] rojo + el texto de la clave. */
    public static Message error(UUID quien, String clave, Object... args) {
        return ChatStyle.error(partes(quien, clave, args));
    }

    /** Linea sin prefijo con el texto de la clave. */
    public static Message suelta(UUID quien, String clave, Object... args) {
        return ChatStyle.plain(partes(quien, clave, args));
    }

    /** Quien escribio el comando; la consola es null y sale en espanol. */
    public static UUID quien(CommandContext context) {
        return context != null && context.isPlayer() ? context.sender().getUuid() : null;
    }

    /** Los trozos de color de una clave, ya traducidos. */
    public static Message[] partes(UUID quien, String clave, Object... args) {
        List<Lang.Span> spans = Lang.spans(quien, clave, args);
        Message[] out = new Message[spans.size()];
        for (int i = 0; i < out.length; i++) {
            Lang.Span s = spans.get(i);
            out[i] = switch (s.style()) {
                case 'v' -> ChatStyle.value(s.text());
                case 'e' -> ChatStyle.emphasis(s.text());
                case 'm' -> ChatStyle.muted(s.text());
                case 'p' -> ChatStyle.player(s.text());
                default -> ChatStyle.body(s.text());
            };
        }
        return out;
    }
}
