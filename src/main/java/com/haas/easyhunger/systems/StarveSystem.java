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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.protocol.MovementStates;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.haas.easyhunger.EasyHungerUtils;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.utils.HungerProtectionUtils;
import com.haas.easyhunger.utils.BiomeUtils;
import com.haas.easyhunger.utils.WorldGate;
import com.haas.easyhunger.config.BiomeModifiersConfig;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StarveSystem extends EntityTickingSystem<EntityStore> {

    private StarveSystem() {
        // Empty constructor - we read config dynamically each tick
    }

    public static StarveSystem create () {
        return new StarveSystem();
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
            EntityStatMap.getComponentType(),
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
        @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Mundo excluido: no se drena, no se hace dano y no se toca el HUD. El nivel se queda
        // congelado tal cual, que es justo lo que se quiere al pasar por el lobby.
        if (!WorldGate.isActive(store)) return;

        HungerComponent hunger = archetypeChunk.getComponent(index, HungerComponent.getComponentType());
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (hunger == null || entityStatMap == null) return;

        hunger.setStaminaSeen(getStaminaValue(entityStatMap));
        hunger.addElapsedTime(dt);
        if (hunger.getElapsedTime() < EasyHunger.get().getConfig().getStarvationTickRate()) return;
        hunger.resetElapsedTime();

        float lowestStaminaSeen = hunger.getAndResetLowestStaminaSeen();
        
        // Check if player is in a protected zone
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef != null && HungerProtectionUtils.isSafe(playerRef)) {
            // Skip hunger drain in safe zones, but still update HUD
            float hungerLevel = hunger.getHungerLevel();
            if (Math.abs(hungerLevel - hunger.getLastSentHunger()) >= 0.01f) {
                hunger.setLastSentHunger(hungerLevel);
                EasyHungerHud.updatePlayerHungerLevel(playerRef, hungerLevel);
            }
            return;
        }
        
        // Check if player is sleeping (pause hunger while in bed)
        if (EasyHunger.get().getConfig().isPauseWhileSleeping() 
            && com.haas.easyhunger.utils.SleepUtils.isSleeping(index, archetypeChunk)) {
            return;
        }

        // Check if player is idle (pause hunger while idle) - position + rotation based
        if (EasyHunger.get().getConfig().isIdlePauseEnabled()) {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform =
                archetypeChunk.getComponent(index, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (hunger.getIdleTracker().updateAndIsPaused(transform)) {
                return; // Paused
            }
        }
        
        float staminaModifier = ((10.0f - lowestStaminaSeen) / 10.0f) * EasyHunger.get().getConfig().getStarvationStaminaModifier();
        
        // Apply biome multiplier
        float biomeMultiplier = 1.0f;
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player != null && player.getWorld() != null) {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform =
                archetypeChunk.getComponent(index, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            String biomeName = BiomeUtils.getPlayerBiomeName(player, transform);
            biomeMultiplier = EasyHunger.get().getBiomeConfig().getHungerMultiplier(biomeName);
        }
        
        hunger.starve((EasyHunger.get().getConfig().getStarvationPerTick() + staminaModifier) * biomeMultiplier);

        float hungerLevel = hunger.getHungerLevel();
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        // Apply hungry effect when hunger level is below 20 (only if not already applied)
        if (hungerLevel != 0 && hungerLevel < EasyHunger.get().getConfig().getHungryThreshold()) {
            EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectController == null) return;

            // Si viene de 0 y comio poco (sin llegar al umbral), Starving seguia puesto hasta
            // agotar sus 300 s. Se quita aqui; Hungry no se toca.
            removeStarvingEffect(ref, commandBuffer, effectController);

            // Only apply if not already hungry
            if (!hasHungerEffect(effectController)) {
                EntityEffect hungryEffect = EasyHungerUtils.getHungryEntityEffect();
                effectController.addEffect(ref, hungryEffect, commandBuffer);
            }
        }

        // Apply starvation effect when hunger level reaches 0
        else if (hungerLevel == 0) {
            EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectController == null) return;
            
            // Only apply starving effect if not already starving
            if (!hasStarvingEffect(effectController)) {
                // remove hungry effect if present
                EasyHungerUtils.removeHungerRelatedEffectsFromEntity(ref, commandBuffer, effectController);
                EntityEffect starvingEffect = EasyHungerUtils.getStarvingEntityEffect();
                effectController.addEffect(ref, starvingEffect, commandBuffer);
            }
            // apply starvation damage
            Damage damage = new Damage(Damage.NULL_SOURCE, EasyHungerUtils.getStarvationDamageCause(), EasyHunger.get().getConfig().getStarvationDamage());
            DamageSystems.executeDamage(ref, commandBuffer, damage);
        }
        
        // Remove effects if hunger is sufficient
        else if (hungerLevel >= EasyHunger.get().getConfig().getHungryThreshold()) {
              EffectControllerComponent effectController = commandBuffer.getComponent(ref, EffectControllerComponent.getComponentType());
              if (effectController != null) {
                  EasyHungerUtils.removeHungerRelatedEffectsFromEntity(ref, commandBuffer, effectController);
              }
        }

        if (playerRef == null) return;
        
        // Optimization: Only update HUD if value changed
        if (Math.abs(hungerLevel - hunger.getLastSentHunger()) < 0.01f) return;
        hunger.setLastSentHunger(hungerLevel);
        
        EasyHungerHud.updatePlayerHungerLevel(playerRef, hungerLevel);
    }

    public static boolean shouldRemoveEffectOnStarvation (ActiveEntityEffect effect) {
        // Only remove our specific hunger-related debuffs
        if (EasyHungerUtils.activeEntityEffectIsHungry(effect)) return true;
        if (EasyHungerUtils.activeEntityEffectIsStarving(effect)) return true;
        // Don't remove any other effects (like health regen from food)
        return false;
    }

    public static float getStaminaValue(@NonNullDecl EntityStatMap entityStatMap) {
        final int staminaRef = DefaultEntityStatTypes.getStamina();
        final EntityStatValue statValue = entityStatMap.get(staminaRef);
        if (statValue == null) return 10.0f; // Default stamina (max) value if not found
        return statValue.get();
    }
    
    private boolean hasHungerEffect(EffectControllerComponent effectController) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (EasyHungerUtils.activeEntityEffectIsHungry(effect)) return true;
            }
        }
        return false;
    }
    
    private void removeStarvingEffect(
        Ref<EntityStore> ref,
        CommandBuffer<EntityStore> commandBuffer,
        EffectControllerComponent effectController
    ) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects == null) return;
        for (ActiveEntityEffect effect : effects) {
            if (EasyHungerUtils.activeEntityEffectIsStarving(effect)) {
                effectController.removeEffect(ref, effect.getEntityEffectIndex(), commandBuffer);
            }
        }
    }

    private boolean hasStarvingEffect(EffectControllerComponent effectController) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (EasyHungerUtils.activeEntityEffectIsStarving(effect)) return true;
            }
        }
        return false;
    }
}



