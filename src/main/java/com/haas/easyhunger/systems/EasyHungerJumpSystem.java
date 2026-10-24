package com.haas.easyhunger.systems;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.EasyHungerUtils;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.components.PlayerActionTimes;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.utils.HungerProtectionUtils;
import com.haas.easyhunger.utils.WorldGate;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


/**
 * System that decreases hunger when a player jumps.
 * Uses a cooldown to prevent draining hunger every tick while int the air.
 */
public class EasyHungerJumpSystem extends EntityTickingSystem<EntityStore> {

    // El timestamp del ultimo salto vive en PlayerActionTimes, un componente de la entidad.
    // Antes era un `static Map<UUID, Long>` que crecia con cada jugador y habia que limpiar a
    // mano en PlayerDisconnect — y ese listener no se dispara al cambiar de mundo.

    // Cooldown entre saltos que cuestan hambre: PlayerActionTimes.JUMP_COOLDOWN_MS (0,5 s).

    public EasyHungerJumpSystem() {
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), MovementStatesComponent.getComponentType());
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Mundo excluido: saltar en el lobby no cuesta hambre.
        if (!WorldGate.isActive(store)) return;

        MovementStatesComponent movementComp = chunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (movementComp == null) return;

        MovementStates states = movementComp.getMovementStates();
        if (states == null) return;

        // Check if jumping (but not swimming or flying)
        if (states.jumping && !states.swimming && !states.flying) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;
            
            // Skip hunger drain in Creative mode
            Player player = chunk.getComponent(index, Player.getComponentType());
            if (player != null && player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) return;
            
            // Skip hunger drain in safe zones
            if (HungerProtectionUtils.isSafe(playerRef)) return;
            
            PlayerActionTimes times = chunk.getComponent(index, PlayerActionTimes.getComponentType());
            if (times == null) return;

            long currentTime = System.currentTimeMillis();

            // Only drain hunger if enough time has passed since last jump
            if (times.lastHungerJump == 0L || (currentTime - times.lastHungerJump) >= PlayerActionTimes.JUMP_COOLDOWN_MS) {
                float jumpCost = EasyHunger.get().getConfig().getJumpHungerCost();
                if (jumpCost <= 0) return;

                HungerComponent hunger = chunk.getComponent(index, HungerComponent.getComponentType());
                if (hunger != null) {
                   float currentHunger = hunger.getHungerLevel();
                   float newHunger = Math.max(0, currentHunger - jumpCost);
                   hunger.setHungerLevel(newHunger);
                   
                   // Update HUD
                   EasyHungerUtils.setPlayerHungerLevel(chunk.getReferenceTo(index), commandBuffer, newHunger);

                   times.lastHungerJump = currentTime;
                }
            }
        }
    }
}
