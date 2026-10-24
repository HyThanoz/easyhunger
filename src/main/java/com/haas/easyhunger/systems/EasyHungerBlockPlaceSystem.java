package com.haas.easyhunger.systems;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.PlayerActionTimes;
import com.haas.easyhunger.utils.WorldGate;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class EasyHungerBlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    // El timestamp vive en PlayerActionTimes (componente de la entidad). Antes era un
    // `static Map<UUID, Long>` con limpieza manual en PlayerDisconnect.

    public EasyHungerBlockPlaceSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {
        
        if (event.isCancelled()) return;

        // Mundo excluido: no se registra el timestamp de colocacion. Si se registrara, colocar un
        // bloque de agua en el lobby afectaria a la deteccion de bebida al volver al mundo normal.
        if (!WorldGate.isActive(store)) return;

        ItemStack item = event.getItemInHand();
        if (item == null) return;
        
        // Get player component
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) return;

        String itemId = item.getItemId();
        
        // Remove leading asterisk if present (Hytale adds this for state variants)
        if (itemId != null && itemId.startsWith("*")) {
            itemId = itemId.substring(1);
        }
        
        // Clean up itemId just like in handlers
        if (itemId != null && itemId.contains(":")) {
             itemId = itemId.substring(0, itemId.indexOf(":"));
        }

        // Only register timestamp if this item is configured as a drink OR food in EasyHunger
        Float drinkValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
        Float foodValue = EasyHunger.get().getFoodsConfig().getFoodValue(itemId);
        
        if ((drinkValue != null && drinkValue > 0) || (foodValue != null && foodValue > 0)) {
            PlayerActionTimes times = chunk.getComponent(index, PlayerActionTimes.getComponentType());
            if (times != null) {
                times.lastBlockPlace = System.currentTimeMillis();
            }
        }
    }
}
