package com.haas.easyhunger.systems;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.PlayerActionTimes;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.ui.EasyWaterHud;
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
 * System that decreases thirst when a player jumps.
 * Mirror of EasyHungerJumpSystem — keeps thirst drain sources in parity with hunger.
 * Uses a cooldown to prevent draining thirst every tick while in the air.
 */
public class EasyThirstJumpSystem extends EntityTickingSystem<EntityStore> {

    // El timestamp vive ahora en PlayerActionTimes (componente de la entidad), no en un mapa
    // estatico que habia que limpiar a mano al desconectar.

    // Cooldown entre saltos que cuestan sed: el mismo que el del hambre,
    // PlayerActionTimes.JUMP_COOLDOWN_MS (0,5 s).

    public EasyThirstJumpSystem() {
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
        // Skip entirely if thirst system disabled
        if (!EasyHunger.get().getConfig().isThirstEnabled()) return;

        // Mundo excluido: saltar en el lobby no cuesta sed.
        if (!WorldGate.isActive(store)) return;

        MovementStatesComponent movementComp = chunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (movementComp == null) return;

        MovementStates states = movementComp.getMovementStates();
        if (states == null) return;

        // Check if jumping (but not swimming or flying)
        if (states.jumping && !states.swimming && !states.flying) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            // Skip thirst drain in Creative mode
            Player player = chunk.getComponent(index, Player.getComponentType());
            if (player != null && player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) return;

            // Skip thirst drain in safe zones
            if (HungerProtectionUtils.isSafe(playerRef)) return;

            PlayerActionTimes times = chunk.getComponent(index, PlayerActionTimes.getComponentType());
            if (times == null) return;

            long currentTime = System.currentTimeMillis();

            // Only drain thirst if enough time has passed since last jump
            if (times.lastThirstJump == 0L || (currentTime - times.lastThirstJump) >= PlayerActionTimes.JUMP_COOLDOWN_MS) {
                float jumpCost = EasyHunger.get().getConfig().getJumpThirstCost();
                if (jumpCost <= 0) return;

                ThirstComponent thirst = chunk.getComponent(index, ThirstComponent.getComponentType());
                if (thirst != null) {
                    float currentThirst = thirst.getThirstLevel();
                    float newThirst = Math.max(0, currentThirst - jumpCost);
                    thirst.setThirstLevel(newThirst);

                    // Update HUD only once the accumulated change is visible (1px ≈ 0.15)
                    if (Math.abs(newThirst - thirst.getLastSentThirst()) >= com.haas.easyhunger.EasyHungerUtils.HUD_VISIBLE_DELTA) {
                        thirst.setLastSentThirst(newThirst);
                        EasyWaterHud.updatePlayerThirstLevel(playerRef, newThirst);
                    }

                    times.lastThirstJump = currentTime;
                }
            }
        }
    }
}
