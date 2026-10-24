package com.haas.easyhunger.interactions;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.utils.WorldGate;

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

public class ConsumeFoodInteraction extends SimpleInstantInteraction {
    
    private float hungerRestoreAmount = 10.0f;
    
    public static final BuilderCodec<ConsumeFoodInteraction> CODEC = 
        ((BuilderCodec.Builder<ConsumeFoodInteraction>) BuilderCodec.builder(
            ConsumeFoodInteraction.class, 
            ConsumeFoodInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("HungerRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.hungerRestoreAmount = value, 
                interaction -> interaction.hungerRestoreAmount)
            .add()
        ).build();
    
    public ConsumeFoodInteraction() {
        super();
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
                context.getState().state = InteractionState.Failed;
                return;
            }
            
            // Mundo excluido: el mod queda inerte, comer no restaura nada. Se marca Finished (no
            // Failed) para que el item se consuma con normalidad y la interaccion no se quede
            // colgada — simplemente no tocamos el nivel de hambre.
            if (!WorldGate.isActive(store)) {
                context.getState().state = InteractionState.Finished;
                return;
            }

            HungerComponent hunger = store.getComponent(entityRef, HungerComponent.getComponentType());

            if (hunger != null) {
                float max = EasyHunger.get().getConfig().getMaxHunger();
                if (hunger.getHungerLevel() < max) {
                    // Get item ID using getOriginalItemType() - always available even for last item in stack
                    com.hypixel.hytale.server.core.asset.type.item.config.Item item = context.getOriginalItemType();
                    String itemId = (item != null) ? item.getId() : "unknown";
                    
                    // Check config for this item's value - only restore if configured
                    float restoreAmount = EasyHunger.get().getFoodsConfig().getFoodValue(itemId);
                    if (restoreAmount <= 0) {
                        // Item not in FoodsConfig - don't restore hunger (might be a drink like Food_Drink_*)
                        context.getState().state = InteractionState.Finished;
                        return;
                    }
                    
                    hunger.feed(restoreAmount);
                    
                    // Update HUD (la vista previa se limpia en el finally)
                    EasyHungerHud.updatePlayerHungerLevel(playerRef, hunger.getHungerLevel());
                }
            }

            context.getState().state = InteractionState.Finished;

        } catch (Exception e) {
            EasyHunger.LOGGER.at(java.util.logging.Level.WARNING).withCause(e).log("CONSUME FOOD ERROR");
            context.getState().state = InteractionState.Failed;
        } finally {
            // Antes solo se limpiaba si el hambre subia: con la comida sin configurar o el hambre
            // al maximo, el anillo de vista previa se quedaba pegado hasta la siguiente comida.
            if (playerRef != null) {
                try {
                    EasyHungerHud.updatePlayerHungerPreview(playerRef, 0.0f);
                } catch (Exception e) {
                    EasyHunger.LOGGER.at(java.util.logging.Level.WARNING).withCause(e).log("CONSUME FOOD PREVIEW ERROR");
                }
            }
        }
    }
}
