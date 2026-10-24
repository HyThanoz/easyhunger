package com.haas.easyhunger.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.utils.WorldGate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * System that applies regeneration buffs based on hunger and thirst levels.
 * - Nourished (Health regen) when hunger >= threshold
 * - Hydrated (Stamina regen) when thirst >= threshold
 */
public class WellFedSystem extends EntityTickingSystem<EntityStore> {
    
    private final float tickRate;
    
    // Effect IDs
    public static final String NOURISHED_EFFECT_ID = "Nourished";  // Health regen from food
    public static final String HYDRATED_EFFECT_ID = "Hydrated";    // Stamina regen from water
    
    private EntityEffect nourishedEffect;
    private EntityEffect hydratedEffect;
    private boolean effectsLoaded = false;
    
    /** Cada cuantos segundos se comprueban Nourished y Hydrated. */
    private static final float CHECK_INTERVAL_SECONDS = 1.0f;

    private WellFedSystem(float tickRate) {
        this.tickRate = tickRate;
    }

    /**
     * Este sistema se registra SIEMPRE, tambien con WellFedEnabled=false.
     *
     * Motivo: Nourished y Hydrated son efectos `"Infinite": true` y este sistema es el UNICO que
     * los retira. Cuando solo se registraba con la opcion activada, apagarla no quitaba los buffs
     * — se quedaban puestos para siempre en quien ya los tuviera, con su Health +0.5 y Stamina
     * +0.2 incluidos. Es decir, "desactivado" significaba en realidad "congelado".
     *
     * Ahora con la opcion apagada el sistema sigue vivo y su unico trabajo es limpiar.
     */
    public static WellFedSystem create() {
        return new WellFedSystem(CHECK_INTERVAL_SECONDS);
    }
    
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            HungerComponent.getComponentType(),
            ThirstComponent.getComponentType(),
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            Query.not(DeathComponent.getComponentType()),
            Query.not(Invulnerable.getComponentType())
        );
    }
    
    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Load effects on first tick
        if (!effectsLoaded) {
            nourishedEffect = EntityEffect.getAssetMap().getAsset(NOURISHED_EFFECT_ID);
            hydratedEffect = EntityEffect.getAssetMap().getAsset(HYDRATED_EFFECT_ID);
            effectsLoaded = true;
            
            if (nourishedEffect != null) {
                EasyHunger.logInfo("Nourished effect loaded: " + NOURISHED_EFFECT_ID);
            } else {
                EasyHunger.logInfo("Nourished effect NOT FOUND: " + NOURISHED_EFFECT_ID);
            }
            if (hydratedEffect != null) {
                EasyHunger.logInfo("Hydrated effect loaded: " + HYDRATED_EFFECT_ID);
            } else {
                EasyHunger.logInfo("Hydrated effect NOT FOUND: " + HYDRATED_EFFECT_ID);
            }
        }
        
        // Mundo excluido: tampoco se conceden los buffs de WellFed. La carga de efectos de arriba
        // se deja fuera del gate a proposito, para que no dependa de en que mundo cae el 1er tick.
        if (!WorldGate.isActive(store)) return;

        HungerComponent hunger = archetypeChunk.getComponent(index, HungerComponent.getComponentType());
        ThirstComponent thirst = archetypeChunk.getComponent(index, ThirstComponent.getComponentType());
        if (hunger == null || thirst == null) return;
        
        // Track elapsed time for tick rate
        hunger.addWellFedElapsedTime(dt);
        if (hunger.getWellFedElapsedTime() < this.tickRate) return;
        hunger.resetWellFedElapsedTime();
        
        // Compare against absolute threshold (like HungryThreshold)
        // Example: if threshold=45 and maxHunger=50, buff activates at 45+ hunger
        float hungerLevel = hunger.getHungerLevel();
        float thirstLevel = thirst.getThirstLevel();
        
        // Se lee la config en cada pasada, no al construir: si la opcion esta apagada, ambos
        // quedan en false y abajo se dispara la rama que RETIRA los efectos.
        EasyHungerConfig conf = EasyHunger.get().getConfig();
        boolean wellFedEnabled = conf.isWellFedEnabled();
        float wellFedThreshold = conf.getWellFedThreshold();

        boolean shouldBeNourished = wellFedEnabled && hungerLevel >= wellFedThreshold;
        boolean shouldBeHydrated = wellFedEnabled && thirstLevel >= wellFedThreshold;
        
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
        if (effectController == null) return;
        
        // El null-check del asset envuelve SOLO el alta: para retirar un efecto no hace falta el
        // EntityEffect (removeEffect va por indice sobre los activos). Antes ambas ramas estaban
        // dentro del if, asi que si el asset no cargaba tampoco se podia limpiar.

        // Nourished — regeneracion de vida por estar bien alimentado
        boolean hasNourished = hasEffect(effectController, NOURISHED_EFFECT_ID);
        if (shouldBeNourished && !hasNourished) {
            if (nourishedEffect != null) {
                effectController.addEffect(ref, nourishedEffect, commandBuffer);
            }
        } else if (!shouldBeNourished && hasNourished) {
            removeEffect(ref, commandBuffer, effectController, NOURISHED_EFFECT_ID);
        }

        // Hydrated — regeneracion de estamina por estar hidratado
        boolean hasHydrated = hasEffect(effectController, HYDRATED_EFFECT_ID);
        if (shouldBeHydrated && !hasHydrated) {
            if (hydratedEffect != null) {
                effectController.addEffect(ref, hydratedEffect, commandBuffer);
            }
        } else if (!shouldBeHydrated && hasHydrated) {
            removeEffect(ref, commandBuffer, effectController, HYDRATED_EFFECT_ID);
        }
    }
    
    private boolean hasEffect(EffectControllerComponent effectController, String effectId) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (isEffect(effect, effectId)) return true;
            }
        }
        return false;
    }
    
    private boolean isEffect(ActiveEntityEffect effect, String effectId) {
        return com.haas.easyhunger.EasyHungerUtils.activeEntityEffectIs(effect, effectId);
    }
    
    private void removeEffect(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, EffectControllerComponent effectController, String effectId) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (isEffect(effect, effectId)) {
                    effectController.removeEffect(ref, effect.getEntityEffectIndex(), accessor);
                }
            }
        }
    }
}
