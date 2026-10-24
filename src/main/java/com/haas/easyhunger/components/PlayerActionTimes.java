package com.haas.easyhunger.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Marcas de tiempo de acciones con cooldown (saltar, colocar bloque), UNA POR ENTIDAD.
 *
 * Sustituye a tres `static Map<UUID, Long>` que vivian en los propios sistemas
 * ({@code EasyHungerJumpSystem.lastJumpTime}, {@code EasyThirstJumpSystem.lastJumpTime} y
 * {@code EasyHungerBlockPlaceSystem.LAST_PLACE_TIME}). Aquellos mapas crecian con cada jugador
 * y obligaban a limpiarlos a mano en {@code EasyHungerPlayerDisconnect} — el parche de 1.3.1.
 * Ademas ese listener NO se dispara al cambiar de mundo, asi que las entradas sobrevivian a los
 * teleports.
 *
 * Al colgar el estado de la entidad, se libera con ella: no hay mapa que crezca, no hay limpieza
 * que recordar y no hay nada que se quede colgado al cambiar de mundo.
 *
 * **Sin codec a proposito.** Es estado efimero de sesion: no tiene sentido persistir "hace cuanto
 * saltaste" en el disco. Se registra con la sobrecarga
 * {@code registerComponent(Class, Supplier)}, que no pide BuilderCodec.
 *
 * Patron tomado del Unofficial Hytale Patch de NightLum ({@code JumpStaminaState} +
 * {@code JumpStaminaProvisionSystem}).
 */
public final class PlayerActionTimes implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerActionTimes> componentType;

    /** Tiempo minimo entre dos saltos que cuestan hambre (o sed), en ms. */
    public static final long JUMP_COOLDOWN_MS = 500;

    /**
     * Ventana tras colocar un bloque de comida/bebida en la que los handlers de inventario
     * ignoran el cambio: el inventario baja en el mismo tick o el siguiente y no es consumo.
     */
    public static final long BLOCK_PLACE_GUARD_MS = 100;

    /** System.currentTimeMillis() del ultimo salto que costo hambre. 0 = nunca. */
    public long lastHungerJump;

    /** System.currentTimeMillis() del ultimo salto que costo sed. 0 = nunca. */
    public long lastThirstJump;

    /** System.currentTimeMillis() de la ultima colocacion de un bloque de comida/bebida. */
    public long lastBlockPlace;

    public static ComponentType<EntityStore, PlayerActionTimes> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, PlayerActionTimes> type) {
        componentType = type;
    }

    @Override
    public Component<EntityStore> clone() {
        PlayerActionTimes copy = new PlayerActionTimes();
        copy.lastHungerJump = this.lastHungerJump;
        copy.lastThirstJump = this.lastThirstJump;
        copy.lastBlockPlace = this.lastBlockPlace;
        return copy;
    }
}
