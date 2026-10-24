package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Abre una ventana 150 ms despues, en el hilo del mundo del jugador.
 *
 * <p>Por que: cuando el comando llega desde el dialogo de un NPC de otro mod que cierra su pagina y
 * ejecuta el comando en el mismo tick, abrir ahi deja el PageManager con una confirmacion pendiente y
 * la ventana descarta todos los clics («Cargando...» eterno). Desde el chat o la tecla O la espera
 * no se nota.
 *
 * <p>Copia identica en cada mod de MyTale (mods standalone). Nunca lanza.
 */
public final class AperturaDiferida {

    public static final long ESPERA_MS = 150L;

    private AperturaDiferida() {}

    public static void abrir(PlayerRef playerRef, Supplier<? extends CustomUIPage> pagina) {
        if (playerRef == null) return;
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            try {
                UUID w = playerRef.getWorldUuid();
                Universe u = Universe.get();
                World world = w == null || u == null ? null : u.getWorld(w);
                if (world == null) return;
                world.execute(() -> {
                    try {
                        Ref<EntityStore> ref = playerRef.getReference();
                        if (ref == null || !ref.isValid()) return;
                        Store<EntityStore> st = ref.getStore();
                        Player player = st.getComponent(ref, Player.getComponentType());
                        if (player != null) player.getPageManager().openCustomPage(ref, st, pagina.get());
                    } catch (Throwable ignored) {
                        // el jugador se fue o cambio de mundo en esos 150 ms
                    }
                });
            } catch (Throwable ignored) {
                // mundo descargandose
            }
        }, ESPERA_MS, TimeUnit.MILLISECONDS);
    }
}
