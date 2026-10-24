package com.haas.easyhunger.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPrefsConfig {
    private static final KeyedCodec<Map<String, Boolean>> HUD_VISIBLE =
            new KeyedCodec<>("HudVisible", new MapCodec<>(Codec.BOOLEAN, HashMap::new));

    public static final BuilderCodec<PlayerPrefsConfig> CODEC = BuilderCodec
            .builder(PlayerPrefsConfig.class, PlayerPrefsConfig::new)
            // MapCodec hands back an immutable map on load — copy it, or put/remove throw
            // UnsupportedOperationException after every server restart. ConcurrentHashMap
            // because hide/show commands run async (AbstractAsyncCommand).
            .append(HUD_VISIBLE,
                    (c, v) -> c.hudVisible = (v != null ? new java.util.concurrent.ConcurrentHashMap<>(v) : new java.util.concurrent.ConcurrentHashMap<>()),
                    c -> c.hudVisible)
            .add()
            .build();

    private Map<String, Boolean> hudVisible = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean isHudVisible(UUID uuid) {
        return hudVisible.getOrDefault(uuid.toString(), true);
    }

    public void setHudVisible(UUID uuid, boolean visible) {
        if (visible) {
            hudVisible.remove(uuid.toString()); // true is the default, no need to store it
        } else {
            hudVisible.put(uuid.toString(), false);
        }
    }
}
