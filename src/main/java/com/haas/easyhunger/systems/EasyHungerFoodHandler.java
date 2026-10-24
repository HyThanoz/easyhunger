package com.haas.easyhunger.systems;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.EasyHungerUtils;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.utils.WorldGate;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EasyHungerFoodHandler extends EntityEventSystem<EntityStore, InventoryChangeEvent> {



    private final com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.haas.easyhunger.components.HungerComponent> hungerComponentType;

    public EasyHungerFoodHandler(com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.haas.easyhunger.components.HungerComponent> hungerComponentType) {
        super(InventoryChangeEvent.class);
        this.hungerComponentType = hungerComponentType;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), this.hungerComponentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        if (this.hungerComponentType == null) {
             return;
        }
        // Mundo excluido (Hub): el estado se congela, comer no restaura hambre. Igual que los
        // sistemas y las interacciones del mod (ver WorldGate).
        if (!WorldGate.isActive(store)) return;
        try {
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) return;

            Transaction transaction = event.getTransaction();
            if (transaction == null) return;

            // Comprobacion de TIPO primero: mover un item es el caso mas frecuente con diferencia
            // y sale sin asignar una sola String. Antes se serializaba la transaccion entera con
            // toString() y se buscaba "MoveTransaction" como subcadena.
            if (transaction instanceof MoveTransaction<?>) return;

            // Despues, la comprobacion ESTRUCTURAL: sin un item consumido que sea eternal_meat
            // no se hace nada mas abajo, asi que se sale aqui sin serializar la transaccion.
            // Las comprobaciones por cadena de abajo se mantienen tal cual para los que pasan.
            String itemId = getConsumedItemId(transaction);
            if (itemId == null || !itemId.toLowerCase().contains("eternal_meat")) return;

            // El toString() solo se paga si se consumio un eternal_meat.
            String transactionInfo = transaction.toString();
            if (transactionInfo.contains("query=null")) return;

            // Filtro historico por cadena: se conserva para no cambiar que transacciones cuentan.
            if (!transactionInfo.toLowerCase().contains("eternal_meat")) return;
            
            // Se lee del componente de la entidad. Antes habia que sacar el PlayerRef con un
            // ComponentAccessor casteado a mano solo para usar su UUID como clave del mapa
            // estatico; con el estado en la entidad basta con leerlo del chunk.
            com.haas.easyhunger.components.PlayerActionTimes placeTimes =
                    archetypeChunk.getComponent(index, com.haas.easyhunger.components.PlayerActionTimes.getComponentType());
            // If a block was placed in the last BLOCK_PLACE_GUARD_MS, abort the Food consumption Handler for this inventory change
            if (placeTimes != null && placeTimes.lastBlockPlace != 0L
                    && System.currentTimeMillis() - placeTimes.lastBlockPlace < com.haas.easyhunger.components.PlayerActionTimes.BLOCK_PLACE_GUARD_MS) {
                return;
            }

            // itemId ya es un eternal_meat: se comprobo arriba, antes del toString().
            Item itemAsset = Item.getAssetMap().getAsset(itemId);
            if (itemAsset == null || !itemAsset.isConsumable()) return;

            float hungerRestore = EasyHunger.get().getFoodsConfig().getFoodValue(itemId);
            if (hungerRestore <= 0) return;

            try {
                var entityStore = player.getReference().getStore();
                ComponentAccessor accessor = (entityStore instanceof ComponentAccessor) ? (ComponentAccessor) entityStore : (ComponentAccessor) (Object) entityStore;

                HungerComponent hunger = (HungerComponent) accessor.getComponent(
                    (Ref) player.getReference(),
                    (ComponentType) (Object) this.hungerComponentType
                );

                if (hunger != null) {
                    float current = hunger.getHungerLevel();
                    float maxHunger = EasyHunger.get().getConfig().getMaxHunger();

                    if (current < maxHunger) {
                        hunger.feed(hungerRestore);

                        try {
                            if (accessor != null) {
                                com.hypixel.hytale.server.core.universe.PlayerRef playerRef =
                                    (com.hypixel.hytale.server.core.universe.PlayerRef) accessor.getComponent(
                                        (Ref) player.getReference(),
                                        (ComponentType) (Object) com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType()
                                    );

                                if (playerRef != null) {
                                    com.haas.easyhunger.ui.EasyHungerHud.updatePlayerHungerLevel(
                                        playerRef,
                                        hunger.getHungerLevel()
                                    );
                                }
                            }
                        } catch (Exception e) {
                            // El hambre ya subio; solo fallo el refresco de la barra.
                            EasyHunger.get().getLogger().at(Level.WARNING).withCause(e).log("Error updating hunger HUD");
                        }
                    }
                }
            } catch (Exception e) {
                EasyHunger.get().getLogger().at(Level.SEVERE).withCause(e).log("Error applying hunger");
            }

        } catch (Exception e) {
            // Exception y no Throwable: un Error (OOM, StackOverflow) no se debe tragar aqui.
            EasyHunger.get().getLogger().at(Level.SEVERE).withCause(e).log("Error in EasyHungerFoodHandler");
        }
    }

    private String getConsumedItemId(Transaction transaction) {
        // Case 1: Single Slot
        if (transaction instanceof com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction) {
             com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction slotTrans = 
                 (com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction) transaction;
             
             com.hypixel.hytale.server.core.inventory.ItemStack before = slotTrans.getSlotBefore();
             com.hypixel.hytale.server.core.inventory.ItemStack after = slotTrans.getSlotAfter();

             if (before != null && before.getQuantity() >= 1) {
                String beforeId = before.getItemId();
                int afterQty = (after != null) ? after.getQuantity() : 0;
                
                // Quantity strictly decreased (Consumption)
                if (afterQty < before.getQuantity()) {
                    return beforeId;
                }
             }
        }

        // Case 2: Multi-Slot
        if (transaction instanceof com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction) {
            com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction stackTransaction = 
                (com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction) transaction;
    
            for (Object slotTransObj : stackTransaction.getSlotTransactions()) {
                if (slotTransObj instanceof com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction) {
                     com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction slotTrans = 
                         (com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction) slotTransObj;
                     com.hypixel.hytale.server.core.inventory.ItemStack before = slotTrans.getSlotBefore();
                     com.hypixel.hytale.server.core.inventory.ItemStack after = slotTrans.getSlotAfter();
        
                    if (before != null && before.getQuantity() >= 1) {
                        int afterQty = (after != null) ? after.getQuantity() : 0;
                        if (afterQty < before.getQuantity()) {
                            return before.getItemId();
                        }
                    }
                }
            }
        }
        return null;
    }


}
