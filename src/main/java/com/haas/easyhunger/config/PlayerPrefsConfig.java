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
    private static final KeyedCodec<Map<String, Boolean>> WARNINGS_OFF =
            new KeyedCodec<>("WarningsOff", new MapCodec<>(Codec.BOOLEAN, HashMap::new));

    public static final BuilderCodec<PlayerPrefsConfig> CODEC = BuilderCodec
            .builder(PlayerPrefsConfig.class, PlayerPrefsConfig::new)
            // MapCodec hands back an immutable map on load — copy it, or put/remove throw
            // UnsupportedOperationException after every server restart. ConcurrentHashMap
            // because hide/show commands run async (AbstractAsyncCommand).
            .append(HUD_VISIBLE,
                    (c, v) -> c.hudVisible = (v != null ? new java.util.concurrent.ConcurrentHashMap<>(v) : new java.util.concurrent.ConcurrentHashMap<>()),
                    c -> c.hudVisible)
            .add()
            .append(WARNINGS_OFF,
                    (c, v) -> c.warningsOff = (v != null ? new java.util.concurrent.ConcurrentHashMap<>(v) : new java.util.concurrent.ConcurrentHashMap<>()),
                    c -> c.warningsOff)
            .add()
            .build();

    private Map<String, Boolean> hudVisible = new java.util.concurrent.ConcurrentHashMap<>();
    // Only players who turned the warnings OFF are stored (on is the default).
    private Map<String, Boolean> warningsOff = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean isWarningsOn(UUID uuid) {
        return !warningsOff.containsKey(uuid.toString());
    }

    public void setWarningsOn(UUID uuid, boolean on) {
        if (on) {
            warningsOff.remove(uuid.toString());
        } else {
            warningsOff.put(uuid.toString(), true);
        }
    }

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
