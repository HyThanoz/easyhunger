package com.haas.easyhunger.systems;

import com.haas.easyhunger.components.PlayerActionTimes;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Cuelga {@link PlayerActionTimes} de cada jugador en cuanto entra al mundo.
 *
 * {@code RefSystem} es la via ECS para engancharse al alta y baja de entidades: se dispara para
 * TODA entidad que case con la query, incluidos los cambios de mundo, sin registrar eventos ni
 * mantener mapas. Verificado que existe en 0.5.7.
 *
 * Es preferible a hacerlo en {@code PlayerReadyEvent} porque no depende de que ese evento llegue
 * ni de su timing: si la entidad esta, el componente esta.
 *
 * Patron tomado de {@code JumpStaminaProvisionSystem} del Unofficial Hytale Patch.
 */
public final class PlayerActionTimesProvisionSystem extends RefSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // ComponentType implementa Query, asi que vale como query de un solo componente.
        return PlayerRef.getComponentType();
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (commandBuffer.getComponent(ref, PlayerActionTimes.getComponentType()) == null) {
            commandBuffer.addComponent(ref, PlayerActionTimes.getComponentType(), new PlayerActionTimes());
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // VACIO A PROPOSITO, y es justo el punto del refactor: el estado vive en la entidad y se
        // libera con ella. No hay mapa que limpiar ni listener de desconexion que recordar.
    }
}
