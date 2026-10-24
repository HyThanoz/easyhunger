package com.haas.easyhunger.interactions;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.ui.EasyWaterHud;
import com.haas.easyhunger.utils.WorldGate;
import com.haas.easyhunger.utils.ConsumableKind;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.ui.EasyHungerHud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DrinkWaterInteraction extends SimpleInstantInteraction {
    
    private float thirstRestoreAmount = 0.0f;
    
    public static final BuilderCodec<DrinkWaterInteraction> CODEC = 
        ((BuilderCodec.Builder) ((BuilderCodec.Builder) BuilderCodec.builder(
            DrinkWaterInteraction.class, 
            DrinkWaterInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("ThirstRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.thirstRestoreAmount = value, 
                interaction -> interaction.thirstRestoreAmount)
            .add())
        ).build();
    
    public DrinkWaterInteraction() {
        super();
    }
    
    /** Id del item consumido sin el asterisco de estado, o null. Mismo orden que el resto del mod. */
    private static String resolveItemId(InteractionContext context) {
        com.hypixel.hytale.server.core.asset.type.item.config.Item item = context.getOriginalItemType();
        String id = (item != null) ? item.getId() : null;
        if (id == null && context.getHeldItem() != null) {
            id = context.getHeldItem().getItemId();
        }
        return ConsumableKind.normalize(id);
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, 
                           @Nonnull InteractionContext context, 
                           @Nonnull CooldownHandler cooldownHandler) {
        // Fuera del try para poder limpiar la vista previa en el finally, salga por donde salga.
        PlayerRef playerRef = null;
        try {
            Ref<EntityStore> entityRef = context.getEntity();
            if (entityRef == null || !entityRef.isValid()) {
                context.getState().state = InteractionState.Failed;
                return;
            }

            Store<EntityStore> store = entityRef.getStore();
            playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            
            if (playerRef == null) {
                // Try fallback search? No, Component Access is safer.
                context.getState().state = InteractionState.Failed;
                return;
            }
            
            // Mundo excluido: el mod queda inerte, beber no restaura nada. Finished y no Failed,
            // por lo mismo que en ConsumeFoodInteraction.
            if (!WorldGate.isActive(store)) {
                context.getState().state = InteractionState.Finished;
                return;
            }

            // Si el item que llega por la cadena de beber es SOLO comida (ni bebida configurada ni
            // ThirstRestoreAmount explicito en el JSON), sube HAMBRE y no sed. Es el caso de los platos
            // de AleAndHearth: usan esta cadena porque es la unica que gasta durabilidad y deja el plato
            // vacio, y antes daban 15 de sed por defecto sin tocar la barra de comida.
            // Ver ConsumableKind para la regla completa.
            String consumedId = resolveItemId(context);
            if (this.thirstRestoreAmount <= 0 && consumedId != null
                    && ConsumableKind.isFoodOnly(EasyHunger.get().getDrinksConfig(),
                            EasyHunger.get().getFoodsConfig(), consumedId)) {
                HungerComponent hunger = store.getComponent(entityRef, HungerComponent.getComponentType());
                if (hunger != null && hunger.getHungerLevel() < EasyHunger.get().getConfig().getMaxHunger()) {
                    hunger.feed(ConsumableKind.foodValue(EasyHunger.get().getFoodsConfig(), consumedId));
                    EasyHungerHud.updatePlayerHungerLevel(playerRef, hunger.getHungerLevel());
                }
                context.getState().state = InteractionState.Finished;
                return;
            }

            ThirstComponent thirst = store.getComponent(entityRef, ThirstComponent.getComponentType());

            if (thirst != null) {
                float max = EasyHunger.get().getConfig().getMaxThirst();
                if (thirst.getThirstLevel() < max) {
                    // Default: use CODEC value from JSON, fallback to 15.0
                    float restoreAmount = (this.thirstRestoreAmount > 0) ? this.thirstRestoreAmount : 15.0f;
                    
                    // Get item ID using getOriginalItemType() - always available even for last item in stack
                    com.hypixel.hytale.server.core.asset.type.item.config.Item item = context.getOriginalItemType();
                    String itemId = (item != null) ? item.getId() : null;
                    
                    // Fallback to getHeldItem if original is null
                    if (itemId == null && context.getHeldItem() != null) {
                        itemId = context.getHeldItem().getItemId();
                    }
                    
                    // Clean up item ID and look up in config
                    if (itemId != null) {
                        // Remove leading asterisk if present (Hytale adds this for state variants)
                        if (itemId.startsWith("*")) {
                            itemId = itemId.substring(1);
                        }
                        
                        // Try full ID first (e.g. Deco_Tankard_State_Filled_Water)
                        Float configValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
                        
                        // If not found, try without state suffix (e.g. Deco_Tankard)
                        if (configValue <= 0 && itemId.contains(":")) {
                            String baseId = itemId.substring(0, itemId.indexOf(":"));
                            configValue = EasyHunger.get().getDrinksConfig().getDrinkValue(baseId);
                        }
                        
                        if (configValue > 0) {
                            restoreAmount = configValue;
                        }
                    }
                    
                    
                    thirst.drink(restoreAmount);
                    
                    // Update HUD (la vista previa se limpia en el finally)
                    EasyWaterHud.updatePlayerThirstLevel(playerRef, thirst.getThirstLevel());
                } else {
                     // Debug: EasyHunger.logInfo("[DrinkInteraction] Thirst Full.");
                }
            }

            context.getState().state = InteractionState.Finished;

        } catch (Exception e) {
            EasyHunger.LOGGER.at(java.util.logging.Level.WARNING).withCause(e).log("DRINK WATER ERROR");
            context.getState().state = InteractionState.Failed;
        } finally {
            // Antes solo se limpiaba si la sed subia: con la sed llena el anillo de vista previa
            // se quedaba pegado hasta la siguiente bebida.
            if (playerRef != null) {
                try {
                    EasyWaterHud.updatePlayerThirstPreview(playerRef, 0.0f);
                    // La vista previa de START puede haber ido a la barra de hambre (plato).
                    EasyHungerHud.updatePlayerHungerPreview(playerRef, 0.0f);
                } catch (Exception e) {
                    EasyHunger.LOGGER.at(java.util.logging.Level.WARNING).withCause(e).log("DRINK WATER PREVIEW ERROR");
                }
            }
        }
    }
}
