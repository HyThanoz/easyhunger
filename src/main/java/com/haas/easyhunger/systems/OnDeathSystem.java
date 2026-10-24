package com.haas.easyhunger.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.EasyHungerUtils;
import com.haas.easyhunger.utils.WorldGate;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.ui.EasyHungerHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.ui.EasyWaterHud;
import com.haas.easyhunger.EasyHunger;

public class OnDeathSystem extends DeathSystems.OnDeathSystem {
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            HungerComponent.getComponentType(),
            ThirstComponent.getComponentType(), // Add Filter for Thirst
            PlayerRef.getComponentType()
        );
    }

    @Override
    public void onComponentAdded(
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl DeathComponent deathComponent,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action needed on death, however method must be overridden
    }

    @Override
    public void onComponentRemoved(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl DeathComponent component, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        // Mundo excluido: morir alli NO rellena las barras. Si rellenara, el lobby seria una
        // recarga gratis y se cargaria la regla de "el estado se congela".
        if (!WorldGate.isActive(store)) return;

        EasyHungerUtils.setPlayerHungerLevel(ref, store, EasyHunger.get().getConfig().getMaxHunger());
        
        // Reset Thirst
        ThirstComponent thirst = store.getComponent(ref, ThirstComponent.getComponentType());
        if (thirst != null) {
            float max = EasyHunger.get().getConfig().getMaxThirst();
            thirst.setThirstLevel(max);
            
            // Should also update HUD if possible, but PlayerRef might be hard to get here?
            // Query has PlayerRef.
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                EasyWaterHud.updatePlayerThirstLevel(playerRef, max);
            }
        }
    }
}



