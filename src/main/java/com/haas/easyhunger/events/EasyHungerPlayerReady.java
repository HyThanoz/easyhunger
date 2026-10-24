package com.haas.easyhunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.ui.EasyWaterHud;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.utils.WorldGate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EasyHungerPlayerReady {
    /**
     * Jugadores con un envio diferido del HUD todavia en curso. El barrido de
     * {@link #recoverMissingHuds} los salta: antes les creaba el HUD mientras esperaban sus
     * 1500 ms, y luego el envio diferido lo volvia a mandar. Dos show() seguidos tras el join
     * pueden tumbar los HUD de todos los jugadores.
     *
     * El valor es una ficha por envio: si dos cambios de mundo seguidos dejan dos envios en cola,
     * el primero en terminar no borra la marca del segundo ({@code remove(uuid, ficha)}).
     */
    private static final ConcurrentHashMap<UUID, Object> pendingDeferredSends = new ConcurrentHashMap<>();

    /** Al desconectar: que una marca huerfana no deje al jugador fuera del barrido para siempre. */
    public static void forgetPendingSend(UUID uuid) {
        if (uuid != null) pendingDeferredSends.remove(uuid);
    }

    private static boolean hasPendingSend(UUID uuid) {
        return uuid != null && pendingDeferredSends.containsKey(uuid);
    }

    private static void clearPendingSend(UUID uuid, Object token) {
        if (uuid != null) pendingDeferredSends.remove(uuid, token);
    }

    public static void handle(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            try {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                // Components must exist immediately so decay/damage systems tick from the
                // moment of join — only the HUD send is deferred below.
                //
                // Se crean TAMBIEN en los mundos excluidos, a proposito: el componente es quien
                // guarda el nivel de hambre/sed. Si no existiera en el Hub, pasar por el Hub
                // borraria el estado y el Hub seria una recarga gratis. Existiendo, el valor se
                // conserva intacto — los sistemas ya no lo tocan alli (ver WorldGate).
                store.ensureAndGetComponent(ref, HungerComponent.getComponentType());
                boolean thirstEnabled = EasyHunger.get().getConfig().isThirstEnabled();
                if (thirstEnabled) {
                    store.ensureAndGetComponent(ref, ThirstComponent.getComponentType());
                }

                // Mundo excluido: ni barras ni nada. PlayerReadyEvent se dispara tambien al
                // LLEGAR a otro mundo, no solo al hacer login, asi que este es el punto correcto
                // para limpiar al entrar al Hub — y para volver a montar el HUD al salir de el.
                if (!WorldGate.isActive(world)) {
                    int clearDelayMs = EasyHunger.get().getConfig().getHudInitDelayMs();
                    if (clearDelayMs <= 0) {
                        clearHuds(playerRef);
                    } else {
                        // Mismo retardo que el envio inicial: una remocion enviada antes de que el
                        // canal de UI del cliente este listo se pierde igual que se perdia el alta.
                        EasyHunger.get().scheduleOnWorld(world, clearDelayMs, () -> clearHuds(playerRef));
                    }
                    return;
                }

                // Sending the HUD packet right at PlayerReadyEvent occasionally races the
                // client's UI channel and the packet is lost — the bars then never appear
                // until a relog. Deferring the SINGLE initial send past that window fixes it
                // without ever re-sending later (re-sends after other mods have drawn their
                // HUDs disrupt the client's shared custom-UI layer — broke all HUDs in 1.3.1).
                int delayMs = EasyHunger.get().getConfig().getHudInitDelayMs();
                if (delayMs <= 0) {
                    createHuds(ref, store);
                } else {
                    // NO se captura el Ref para usarlo dentro de 1500ms: si el jugador cambia de
                    // mundo durante la espera, ese Ref queda invalido y el HUD se perdia PARA
                    // SIEMPRE (en el log salia
                    // "[HUD] Abortado: la referencia de entidad ya no es valida").
                    // Se re-resuelve el jugador en el momento del envio.
                    scheduleResolvedCreate(playerRef, delayMs);
                }
            } catch (Exception e) {
                EasyHunger.LOGGER.at(Level.SEVERE).withCause(e).log("Failed to set up hunger/thirst on player join");
            }
        });
    }

    /**
     * Programa la creacion del HUD resolviendo al jugador EN EL MOMENTO DEL ENVIO.
     *
     * El bug que arregla: antes se capturaba el {@code Ref} de la entidad y se usaba 1500ms
     * despues. Si el jugador cambiaba de mundo en ese hueco (rutas de spawn, portales, teleports
     * de otros mods), el Ref moria y {@code createHuds} abortaba sin reintentar. Resultado: sin
     * barras hasta que el jugador usara el rescate manual, si es que sabia que existe.
     *
     * Ahora se guarda solo el PlayerRef (que sobrevive al cambio de mundo) y al disparar se
     * pregunta DONDE esta ahora y CUAL es su entidad ahi.
     */
    private static void scheduleResolvedCreate(PlayerRef playerRef, int delayMs) {
        // Marca de envio pendiente: el barrido no toca a este jugador hasta que el envio termine
        // o aborte. Si la tarea llega al hilo del mundo, es ella la que quita la marca al acabar.
        UUID pendingUuid = playerRef.getUuid();
        Object token = new Object();
        if (pendingUuid != null) pendingDeferredSends.put(pendingUuid, token);

        EasyHunger.get().scheduleDetached(delayMs, () -> {
            boolean handedToWorld = false;
            try {
                UUID uuid = playerRef.getUuid();
                if (uuid == null || !playerRef.isValid()) return;

                World current = Universe.get().getWorld(playerRef.getWorldUuid());
                if (current == null) {
                    EasyHunger.logInfo("[HUD] Abortado para " + playerRef.getUsername()
                            + ": no se pudo resolver su mundo actual.");
                    return;
                }

                // El mundo de destino puede ser otro que el de origen — y puede estar excluido.
                if (!WorldGate.isActive(current)) {
                    clearHuds(playerRef);
                    return;
                }

                current.execute(() -> {
                    try {
                        Ref<EntityStore> freshRef = current.getEntityRef(uuid);
                        if (freshRef == null || !freshRef.isValid()) {
                            EasyHunger.logInfo("[HUD] Abortado para " + playerRef.getUsername()
                                    + ": sin entidad valida en " + current.getName() + " tras el retardo.");
                            return;
                        }
                        createHuds(freshRef, current.getEntityStore().getStore());
                    } finally {
                        clearPendingSend(pendingUuid, token);
                    }
                });
                handedToWorld = true;
            } catch (Exception e) {
                EasyHunger.LOGGER.at(Level.SEVERE).withCause(e).log("Failed to resolve player for deferred HUD send");
            } finally {
                if (!handedToWorld) clearPendingSend(pendingUuid, token);
            }
        });
    }

    /**
     * Retira las dos barras en un mundo excluido. Se llama tanto si el jugador acaba de entrar al
     * servidor por el Hub (no habia HUD: la remocion es inofensiva) como si viene de un mundo donde
     * si estaban puestas.
     */
    private static void clearHuds(PlayerRef playerRef) {
        try {
            EasyHungerHud.forceClear(playerRef);
            EasyWaterHud.forceClear(playerRef);
        } catch (Exception e) {
            EasyHunger.LOGGER.at(Level.SEVERE).withCause(e).log("Failed to clear hunger/thirst HUD in disabled world");
        }
    }

    /**
     * Runs on the world thread. The player may have disconnected if the send was deferred.
     *
     * Los abortos se LOGUEAN. Antes eran cuatro `return` mudos, asi que cuando un jugador decia
     * "no me carga el HUD" no habia forma de saber en cual de los cuatro caia — ni siquiera si
     * llegaba hasta aqui. Es {@code logInfo}, que en este mod escribe a nivel WARNING.
     */
    public static void createHuds(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            if (!ref.isValid()) {
                EasyHunger.logInfo("[HUD] Abortado: la referencia de entidad ya no es valida "
                        + "(el jugador se fue o cambio de mundo durante el retardo inicial).");
                return;
            }
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            Player player = store.getComponent(ref, Player.getComponentType());
            if (playerRef == null || player == null) {
                EasyHunger.logInfo("[HUD] Abortado: falta componente en la entidad — PlayerRef="
                        + (playerRef == null ? "null" : "ok") + " Player=" + (player == null ? "null" : "ok"));
                return;
            }

            HungerComponent hungerComponent = store.getComponent(ref, HungerComponent.getComponentType());
            if (hungerComponent == null) {
                EasyHunger.logInfo("[HUD] Abortado para " + playerRef.getUsername()
                        + ": no tiene HungerComponent. Deberia haberse creado en el ready.");
                return;
            }
            float hungerLevel = hungerComponent.getHungerLevel();

            boolean thirstEnabled = EasyHunger.get().getConfig().isThirstEnabled();
            float thirstLevel = 0.0f;
            if (thirstEnabled) {
                ThirstComponent thirstComponent = store.getComponent(ref, ThirstComponent.getComponentType());
                if (thirstComponent != null) {
                    thirstLevel = thirstComponent.getThirstLevel();
                } else {
                    thirstEnabled = false;
                }
            }

            // Update 5+ supports multiple custom HUDs natively (HudManager.addCustomHud),
            // so we always register separate hunger/water HUDs. No MultipleHUD dependency needed.
            EasyHungerHud hud = new EasyHungerHud(playerRef, player.getGameMode(), hungerLevel);
            player.getHudManager().addCustomHud(playerRef, hud);

            if (thirstEnabled) {
                EasyWaterHud waterHud = new EasyWaterHud(playerRef, player.getGameMode(), thirstLevel);
                player.getHudManager().addCustomHud(playerRef, waterHud);
            }

            // Restore HUD visibility preference from last session
            UUID uuid = playerRef.getUuid();
            if (uuid != null && !EasyHunger.get().getPlayerPrefsConfig().isHudVisible(uuid)) {
                EasyHungerHud.updatePlayerHudVisibility(playerRef, false);
                if (thirstEnabled) {
                    EasyWaterHud.updatePlayerHudVisibility(playerRef, false);
                }
            }
        } catch (Exception e) {
            EasyHunger.LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create HUD for player");
        }
    }

    /**
     * Recuperacion automatica SEGURA del HUD (Modo A: el HUD ni se llego a registrar por un cambio
     * de mundo / Ref invalido durante el envio diferido). Corre en bucle desde {@code setup()}.
     *
     * CLAVE DE SEGURIDAD: solo CREA el que falta
     * ({@code hasHud()==false}); NUNCA reenvia uno existente — reenviar un {@code show()} post-join
     * tumbo TODOS los HUDs del servidor en la 1.3.1. Es exactamente la rama de rescate de /eh show,
     * automatizada, sin que el jugador escriba nada. NO cubre el Modo B (el server lo mando pero el
     * paquete se perdio en el cliente: ahi {@code hasHud()==true} y no hay forma segura de saberlo),
     * que es raro gracias al retardo del envio inicial.
     */
    public static void recoverMissingHuds() {
        try {
            for (PlayerRef pr : Universe.get().getPlayers()) {
                try {
                    UUID uuid = pr.getUuid();
                    if (uuid == null || !pr.isValid()) continue;
                    if (EasyHungerHud.hasHud(pr)) continue;   // ya lo tiene -> NO tocar (la puerta de seguridad)
                    if (hasPendingSend(uuid)) continue;       // su envio diferido aun no sale -> no duplicarlo
                    World world = Universe.get().getWorld(pr.getWorldUuid());
                    if (world == null || !WorldGate.isActive(world)) continue;   // Hub/excluido: sin HUD a proposito
                    world.execute(() -> {
                        try {
                            if (EasyHungerHud.hasHud(pr)) return;   // otra via pudo crearlo entre medias
                            if (hasPendingSend(uuid)) return;       // le llego un envio diferido mientras esto esperaba
                            Ref<EntityStore> ref = world.getEntityRef(uuid);
                            if (ref == null || !ref.isValid()) return;
                            EasyHunger.logInfo("[HUD] Auto-recuperacion: " + pr.getUsername()
                                    + " no tenia HUD registrado; se crea.");
                            createHuds(ref, world.getEntityStore().getStore());
                        } catch (Exception ignored) { }
                    });
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            EasyHunger.LOGGER.at(Level.SEVERE).withCause(e).log("Fallo el barrido de auto-recuperacion de HUD");
        }
    }
}
