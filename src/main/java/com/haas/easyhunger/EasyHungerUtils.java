package com.haas.easyhunger;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.haas.easyhunger.systems.StarveSystem;
import com.haas.easyhunger.ui.EasyHungerHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.lang.reflect.Field;
import java.util.Arrays;

public class EasyHungerUtils {
    /**
     * Minimum hunger/thirst change (in stat units) worth a HUD network packet from
     * high-frequency sources (block break, jump). The bar is 318px for a 0..MaxHunger(50)
     * range, so one visible pixel ≈ 0.157 units — deltas below this are invisible.
     * Eating/death/starvation ticks produce larger deltas and always pass.
     */
    public static final float HUD_VISIBLE_DELTA = 0.15f;

    /** Valor que devuelve IndexedLookupTableAssetMap.getIndex para una clave que no existe. */
    private static final int MISSING_ASSET_INDEX = Integer.MIN_VALUE;

    private static EntityEffect starvingEntityEffect;
    private static EntityEffect hungryEntityEffect;
    private static EntityEffect dehydratedEntityEffect;

    /**
     * DamageCause de hambre y sed. Son inmutables (sin setters) y Damage solo lee su getId()
     * para buscar el indice en el asset map (javap 0.6.8), asi que basta una instancia de cada.
     * Van en un holder para crearse en el primer uso, como antes.
     */
    private static final class DamageCauses {
        static final DamageCause STARVATION = new DamageCause("Starvation", "Starvation", false, true, true);
        static final DamageCause THIRST = new DamageCause("Thirst", "Thirst", false, true, true);
    }

    public static final String starvingEntityEffectId = "Starving";
    public static final String hungryEntityEffectId = "Hungry";
    public static final String dehydratedEntityEffectId = "Dehydrated";


    @NonNullDecl
    public static EntityEffect getStarvingEntityEffect() {
        if (starvingEntityEffect == null) {
            starvingEntityEffect = EntityEffect.getAssetMap().getAsset(starvingEntityEffectId);
            assert starvingEntityEffect != null;
            EasyHungerConfig conf = EasyHunger.get().getConfig();
            try {
                // patch damageCalculator cooldown so that audio syncs properly with starvation tick rate (user configured)
                Field f = EntityEffect.class.getDeclaredField("damageCalculatorCooldown");
                f.setAccessible(true);
                // this only affect sfx. Users with low hunger tick rates may notice bugged sfx timings otherwise.
                float cooldown = Math.max(conf.getStarvationTickRate(), 1.0f);
                f.setFloat(starvingEntityEffect, cooldown);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return starvingEntityEffect;
    }

    @NonNullDecl
    public static EntityEffect getHungryEntityEffect() {
        if (hungryEntityEffect == null) {
            hungryEntityEffect = EntityEffect.getAssetMap().getAsset(hungryEntityEffectId);
            assert hungryEntityEffect != null;
        }
        return hungryEntityEffect;
    }

    /**
     * Compara por indice de asset, sin reflexion. EffectControllerComponent asigna a cada efecto
     * activo el indice de su entityEffectId (getIndex) y descarta los que no tienen asset, asi que
     * comparar el indice equivale a comparar el id. Se resuelve en cada llamada (es una busqueda
     * en un mapa) para no guardar un indice viejo si los assets se recargan.
     */
    public static boolean activeEntityEffectIs(ActiveEntityEffect effect, String entityEffectId) {
        if (effect == null) return false;
        int index = EntityEffect.getAssetMap().getIndex(entityEffectId);
        if (index == MISSING_ASSET_INDEX) return false;
        return effect.getEntityEffectIndex() == index;
    }

    public static boolean activeEntityEffectIsStarving(ActiveEntityEffect effect) {
        return activeEntityEffectIs(effect, starvingEntityEffectId);
    }

    public static boolean activeEntityEffectIsHungry(ActiveEntityEffect effect) {
        return activeEntityEffectIs(effect, hungryEntityEffectId);
    }

    @NonNullDecl
    public static EntityEffect getDehydratedEntityEffect() {
        if (dehydratedEntityEffect == null) {
            dehydratedEntityEffect = EntityEffect.getAssetMap().getAsset(dehydratedEntityEffectId);
            assert dehydratedEntityEffect != null;
        }
        return dehydratedEntityEffect;
    }

    public static boolean activeEntityEffectIsDehydrated(ActiveEntityEffect effect) {
        return activeEntityEffectIs(effect, dehydratedEntityEffectId);
    }

    public static boolean activeEntityEffectIsDehydrated(EffectControllerComponent effectController) {
        ActiveEntityEffect[] effects = effectController.getAllActiveEntityEffects();
        if (effects != null) {
            for (ActiveEntityEffect effect : effects) {
                if (activeEntityEffectIsDehydrated(effect)) return true;
            }
        }
        return false;
    }

    public static boolean activeEntityEffectIsThirstRelated(ActiveEntityEffect effect) {
        return activeEntityEffectIsDehydrated(effect);
    }

    public static void removeThirstRelatedEffectsFromEntity(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> componentAccessor,
            @NonNullDecl EffectControllerComponent effectController
    ) {
        final ActiveEntityEffect[] activeEffects = effectController.getAllActiveEntityEffects();
        if (activeEffects != null && activeEffects.length > 0) {
            Arrays.stream(activeEffects).filter(EasyHungerUtils::activeEntityEffectIsThirstRelated).forEach(effect -> {
                effectController.removeEffect(ref, effect.getEntityEffectIndex(), componentAccessor);
            });
        }
    }

    public static void removeHungerRelatedEffectsFromEntity(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> componentAccessor,
            @NonNullDecl EffectControllerComponent effectController
    ) {
        final ActiveEntityEffect[] activeEffects = effectController.getAllActiveEntityEffects();
        if (activeEffects != null && activeEffects.length > 0) {
            Arrays.stream(activeEffects).filter(StarveSystem::shouldRemoveEffectOnStarvation).forEach(effect -> {
                effectController.removeEffect(ref, effect.getEntityEffectIndex(), componentAccessor);
            });
        }
    }

    public static void removeHungerRelatedEffectsFromEntity(
            Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> componentAccessor
    ) {
        final EffectControllerComponent effectController = componentAccessor.getComponent(ref, EffectControllerComponent.getComponentType());
        if (effectController == null) return;
        removeHungerRelatedEffectsFromEntity(ref, componentAccessor, effectController);
    }

    public static void setPlayerHungerLevel(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl ComponentAccessor<EntityStore> componentAccessor,
            float hungerLevel
    ) {
        HungerComponent hungerComponent = componentAccessor.getComponent(ref, HungerComponent.getComponentType());
        PlayerRef playerRef = componentAccessor.getComponent(ref, PlayerRef.getComponentType());
        if (hungerComponent == null || playerRef == null) return;

        hungerComponent.setHungerLevel(hungerLevel);

        // Skip the HUD packet for sub-pixel changes (block break / jump spam); the component
        // keeps the exact value, and the send fires once the accumulated delta is visible.
        if (Math.abs(hungerLevel - hungerComponent.getLastSentHunger()) < HUD_VISIBLE_DELTA) return;
        hungerComponent.setLastSentHunger(hungerLevel);
        EasyHungerHud.updatePlayerHungerLevel(playerRef, hungerLevel);
    }

    @NonNullDecl
    public static DamageCause getStarvationDamageCause() {
        return DamageCauses.STARVATION;
    }

    @NonNullDecl
    public static DamageCause getThirstDamageCause() {
        return DamageCauses.THIRST;
    }
}



