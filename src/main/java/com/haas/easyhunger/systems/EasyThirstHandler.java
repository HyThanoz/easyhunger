package com.haas.easyhunger.systems;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.ui.EasyWaterHud;
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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EasyThirstHandler extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

    private final com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.haas.easyhunger.components.ThirstComponent> thirstComponentType;

    public EasyThirstHandler(com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.haas.easyhunger.components.ThirstComponent> thirstComponentType) {
        super(InventoryChangeEvent.class);
        this.thirstComponentType = thirstComponentType;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), this.thirstComponentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        if (this.thirstComponentType == null) {
             return;
        }
        // Skip if thirst system is disabled
        if (!EasyHunger.get().getConfig().isThirstEnabled()) return;

        // Mundo excluido (Hub): el estado se congela, beber no restaura sed. Igual que los
        // sistemas y las interacciones del mod (ver WorldGate).
        if (!WorldGate.isActive(store)) return;
        try {
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) return;

            Transaction transaction = event.getTransaction();
            if (transaction == null) return;

            // Comprobacion de TIPO primero: mover un item es el caso mas frecuente con diferencia
            // y ahora sale sin asignar una sola String. Antes se serializaba la transaccion entera
            // con toString() y se buscaba "MoveTransaction" como subcadena dentro.
            if (transaction instanceof MoveTransaction<?>) return;

            // Despues, la comprobacion ESTRUCTURAL: solo cuenta una transaccion con la forma exacta de
            // un consumo (ver getConsumedItemId), sus exclusiones y su valor de bebida.
            String itemId = getConsumedItemId(transaction);
            if (itemId == null) return;

            // Remove leading asterisk if present (Hytale adds this for state variants)
            if (itemId.startsWith("*")) {
                itemId = itemId.substring(1);
            }

            // Skip EasyHunger items - they use EasyHunger_DrinkWater interaction which already handles thirst
            if (itemId.startsWith("EasyHunger_")) {
                return;
            }

            // Skip items that use Root_Secondary_Consume_Potion - Consume_Charge_Potion_Fast now handles them
            // Only skip specific mod prefixes that use the potion consumption chain
            if (itemId.startsWith("NoCube_Drink") ||
                itemId.startsWith("Brewery_") ||
                itemId.startsWith("NoCube_Template_Juice")) {
                return;
            }

            // Lookup drink value from config - only restore if configured
            Float drinkValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
            if (drinkValue == null || drinkValue <= 0) return;

            // El antiguo filtro por cadena («query=null» en el toString) ahora es getQuery() == null
            // dentro de getConsumedItemId.

            // Check if player just placed a block (to avoid restoring thirst on block placement)
            // Using the new system that tracks placement of configured drink items
            // Se sigue necesitando el PlayerRef mas abajo, para refrescar el HUD.
            PlayerRef playerRef = getPlayerRef(player);

            // Se lee del componente de la entidad; antes venia de un mapa estatico por UUID.
            com.haas.easyhunger.components.PlayerActionTimes placeTimes =
                    archetypeChunk.getComponent(index, com.haas.easyhunger.components.PlayerActionTimes.getComponentType());
            // Small window (BLOCK_PLACE_GUARD_MS) is enough because inventory change happens immediately after placement in the same tick or next
            if (placeTimes != null && placeTimes.lastBlockPlace != 0L
                    && System.currentTimeMillis() - placeTimes.lastBlockPlace < com.haas.easyhunger.components.PlayerActionTimes.BLOCK_PLACE_GUARD_MS) {
                return;
            }

            // Get player's thirst component and restore thirst (same pattern as FoodHandler)
            try {
                // Re-use logic to get accessor for thirst component
                var entityStore = player.getReference().getStore();

                ComponentAccessor accessor = null;
                if (entityStore instanceof ComponentAccessor) {
                    accessor = (ComponentAccessor) entityStore;
                } else {
                    accessor = (ComponentAccessor) (Object) entityStore;
                }

                ThirstComponent thirst = (ThirstComponent) accessor.getComponent(
                    (Ref) player.getReference(),
                    (ComponentType) (Object) this.thirstComponentType
                );

                if (thirst != null) {
                    float max = EasyHunger.get().getConfig().getMaxThirst();
                    if (thirst.getThirstLevel() < max) {
                        thirst.drink(drinkValue);

                        // Update HUD
                        try {
                            if (playerRef != null) { // Use cached playerRef
                                EasyWaterHud.updatePlayerThirstLevel(playerRef, thirst.getThirstLevel());
                            }
                        } catch (Exception e) {
                            // La sed ya subio; solo fallo el refresco de la barra.
                            EasyHunger.get().getLogger().at(Level.WARNING).withCause(e).log("[ThirstHandler] Error updating thirst HUD");
                        }
                    }
                }
            } catch (Exception e) {
                EasyHunger.get().getLogger().at(Level.WARNING).withCause(e).log("[ThirstHandler] Error applying thirst");
            }

        } catch (Exception e) {
            // Exception y no Throwable: un Error (OOM, StackOverflow) no se debe tragar aqui.
            EasyHunger.get().getLogger().at(Level.SEVERE).withCause(e).log("Error in EasyThirstHandler");
        }
    }

    /**
     * El id del item SOLO si la transaccion tiene la forma exacta de un consumo; si no, null.
     *
     * Consumir (comer o beber) lo hace la interaccion nativa ModifyInventory con
     * AdjustHeldItemQuantity -1, que llama a ItemContainer.removeItemStackFromSlot(slot, itemEnMano, 1)
     * (verificado con javap en 0.6.8): UNA ranura, accion REMOVE, con query (el item en mano), y la
     * ranura baja exactamente 1 sin cambiar de item. Lo que antes tambien contaba y ya no:
     *  - craftear, vender, recetas: quitan por removeItemStack/materiales, transacciones de varias
     *    ranuras (el antiguo «Case 2»);
     *  - vaciar un cubo o gastar durabilidad: setItemStackForSlot, accion SET, misma cantidad y otro id
     *    (Container_Bucket_State_Filled_Water -> Container_Bucket; la antigua «Detection 2»). Las
     *    bebidas de durabilidad de verdad (taza, jarra, cubo de leche) usan Root_Secondary_Consume_Drink,
     *    cuya cadena ya llama a EasyHunger_DrinkWater: contarlas tambien aqui era sed doble;
     *  - soltar con la tecla: removeItemStackFromSlot(slot, n) sin query (InventoryPacketHandler).
     * Colocar el item como bloque tiene la misma forma que consumir; lo filtra la ventana de
     * PlaceBlockEvent de handle().
     */
    private String getConsumedItemId(Transaction transaction) {
        if (!(transaction instanceof ItemStackSlotTransaction)) {
            return null;
        }
        ItemStackSlotTransaction slotTrans = (ItemStackSlotTransaction) transaction;
        if (!slotTrans.succeeded() || slotTrans.getAction() != ActionType.REMOVE || slotTrans.getQuery() == null) {
            return null;
        }
        ItemStack before = slotTrans.getSlotBefore();
        if (before == null || before.getQuantity() < 1 || before.getItemId() == null) {
            return null;
        }
        ItemStack after = slotTrans.getSlotAfter();
        int afterQty = (after != null) ? after.getQuantity() : 0;
        if (before.getQuantity() - afterQty != 1) {
            return null;
        }
        // Si queda algo en la ranura, tiene que ser el mismo item (un consumo no transforma).
        if (afterQty > 0 && !before.getItemId().equals(after.getItemId())) {
            return null;
        }
        return before.getItemId();
    }

    private PlayerRef getPlayerRef(Player player) {
        try {
            var store = player.getReference().getStore();
            ComponentAccessor accessor = null;
            if (store instanceof ComponentAccessor) {
                accessor = (ComponentAccessor) store;
            } else {
                accessor = (ComponentAccessor) (Object) store;
            }
            return (PlayerRef) accessor.getComponent(
                (Ref) player.getReference(), 
                (ComponentType) (Object) PlayerRef.getComponentType()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
