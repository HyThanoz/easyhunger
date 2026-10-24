package com.haas.easyhunger.systems;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.ui.EasyWaterHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.utils.HungerProtectionUtils;
import com.haas.easyhunger.utils.WorldGate;

import javax.annotation.Nonnull;

/**
 * System that decreases thirst when a player breaks a block.
 * Mirror of EasyHungerBlockBreakSystem — keeps thirst drain sources in parity with hunger.
 */
public class EasyThirstBlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public EasyThirstBlockBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {

        // Skip entirely if thirst system disabled
        if (!EasyHunger.get().getConfig().isThirstEnabled()) return;

        // Mundo excluido: picar en el lobby no cuesta sed.
        if (!WorldGate.isActive(store)) return;

        ThirstComponent thirst = chunk.getComponent(index, ThirstComponent.getComponentType());
        if (thirst == null) return;

        // Skip thirst drain in Creative mode
        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player != null && player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) return;

        // Skip thirst drain in safe zones
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef != null && HungerProtectionUtils.isSafe(playerRef)) return;

        float blockBreakCost = EasyHunger.get().getConfig().getBlockBreakThirstCost();
        if (blockBreakCost <= 0) return;

        float currentThirst = thirst.getThirstLevel();
        float newThirst = Math.max(0, currentThirst - blockBreakCost);

        thirst.setThirstLevel(newThirst);

        // Update HUD only once the accumulated change is visible (1px ≈ 0.15) — mining spam
        // otherwise sends a network packet per broken block for a sub-pixel bar change.
        if (playerRef != null
                && Math.abs(newThirst - thirst.getLastSentThirst()) >= com.haas.easyhunger.EasyHungerUtils.HUD_VISIBLE_DELTA) {
            thirst.setLastSentThirst(newThirst);
            EasyWaterHud.updatePlayerThirstLevel(playerRef, newThirst);
        }
    }
}
