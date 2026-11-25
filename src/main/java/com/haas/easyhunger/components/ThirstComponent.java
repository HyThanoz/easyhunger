package com.haas.easyhunger.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.EasyHunger;

import javax.annotation.Nullable;

public class ThirstComponent implements Component<EntityStore> {
    public static final BuilderCodec<ThirstComponent> CODEC = BuilderCodec.builder(ThirstComponent.class, ThirstComponent::new)
            .append(new KeyedCodec<>("ThirstLevel", Codec.FLOAT),
                    ThirstComponent::setThirstLevel,
                    ThirstComponent::getThirstLevel).add()
            .build();

    private float thirstLevel;
    private float elapsedTime = 0.0f;
    private float lastSentThirst = -1.0f;
    // Pausa por inactividad (estado compartido con el hambre por la clase IdleTracker)
    private final IdleTracker idle = new IdleTracker();

    // Low-level warning stage already shown (0 none, 1 low, 2 critical). Runtime only, not saved.
    private int avisoEtapa = 0;

    public ThirstComponent() {
        this.thirstLevel = EasyHunger.get().getConfig().getMaxThirst(); // Init with Max
    }

    public ThirstComponent (float thirstLevel) {
        this.thirstLevel = thirstLevel;
    }

    public ThirstComponent (ThirstComponent other) {
        this.thirstLevel = other.thirstLevel;
        this.elapsedTime = other.elapsedTime;
        this.idle.copyIdleTimeFrom(other.idle);
    }

    public float getElapsedTime () {
        return this.elapsedTime;
    }
    public void addElapsedTime (float deltaTime) {
        this.elapsedTime += deltaTime;
    }
    public void resetElapsedTime () {
        this.elapsedTime = 0.0f;
    }

    public IdleTracker getIdleTracker() { return idle; }

    public float getLastSentThirst() { return lastSentThirst; }
    public void setLastSentThirst(float v) { this.lastSentThirst = v; }

    public int getAvisoEtapa() { return avisoEtapa; }
    public void setAvisoEtapa(int v) { this.avisoEtapa = v; }
    
    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new ThirstComponent(this);
    }

    public float getThirstLevel () {
        return this.thirstLevel;
    }

    public void setThirstLevel (float level) {
        if (!Float.isFinite(level)) return; // NaN would stick forever (every comparison is false)
        float max = EasyHunger.get().getConfig().getMaxThirst();
        this.thirstLevel = Math.max(0.0f, Math.min(level, max));
    }

    public void drink (float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0f) return;
        float max = EasyHunger.get().getConfig().getMaxThirst();
        this.thirstLevel = Math.min(this.thirstLevel + amount, max);
    }

    public void dehydrate(float amount) {
        // 1.5.30: a negative amount (e.g. a negative biome multiplier) raised thirst past the max.
        if (!Float.isFinite(amount) || amount <= 0.0f) return;
        this.thirstLevel = Math.max(this.thirstLevel - amount, 0.0f);
    }

    public static ComponentType<EntityStore, ThirstComponent> getComponentType() {
        return EasyHunger.get().getThirstComponentType();
    }
}
