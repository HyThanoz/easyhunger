package com.haas.easyhunger.interactions;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.ui.EasyHungerHud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Interaction that runs when player STARTS eating (before the eating animation completes).
 * Shows a preview of how much hunger will be restored on the HUD.
 */
public class StartFeedingInteraction extends SimpleInstantInteraction {
    
    private float hungerRestoreAmount = 10.0f;
    
    public static final BuilderCodec<StartFeedingInteraction> CODEC = 
        ((BuilderCodec.Builder<StartFeedingInteraction>) BuilderCodec.builder(
            StartFeedingInteraction.class, 
            StartFeedingInteraction::new,
            SimpleInstantInteraction.CODEC)
            .append(new KeyedCodec<>("HungerRestoreAmount", Codec.FLOAT), 
                (interaction, value) -> interaction.hungerRestoreAmount = value, 
                interaction -> interaction.hungerRestoreAmount)
            .add()
        ).build();
    
    public StartFeedingInteraction() {
        super();
    }
    
    @Override
    protected void firstRun(@Nonnull InteractionType type, 
                           @Nonnull InteractionContext context, 
                           @Nonnull CooldownHandler cooldownHandler) {
        try {
            Ref<EntityStore> entityRef = context.getEntity();
            if (entityRef == null || !entityRef.isValid()) return;
            
            Store<EntityStore> store = entityRef.getStore();
            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
            Item item = context.getOriginalItemType();
            
            if (playerRef == null || item == null) return;
            
            // Misma fuente que ConsumeFoodInteraction: solo FoodsConfig. Antes, si la comida no
            // estaba configurada, la vista previa caia en HungerRestoreAmount del JSON (5/10/15) y
            // prometia hambre que ConsumeFood nunca daba (restaura 0). Sin configurar: sin vista previa.
            // El campo HungerRestoreAmount se sigue leyendo del JSON para que el codec no falle.
            String itemId = item.getId();
            float restoreAmount = Math.max(0.0f, EasyHunger.get().getFoodsConfig().getFoodValue(itemId));

            // Show preview on HUD
            EasyHungerHud.updatePlayerHungerPreview(playerRef, restoreAmount);
            
        } catch (Exception e) {
            EasyHunger.LOGGER.at(java.util.logging.Level.WARNING).withCause(e).log("START FEEDING ERROR");
        }
    }
}
