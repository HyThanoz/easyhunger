package com.haas.easyhunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.config.EasyHungerConfig;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class EasyHungerHud extends CustomUIHud {
    /**
     * Keyed by player UUID (not PlayerRef) so lookups are reliable regardless of which
     * PlayerRef instance the caller holds — PlayerRef does not override equals/hashCode.
     */
    static private final ConcurrentHashMap<UUID, EasyHungerHud> hudMap = new ConcurrentHashMap<>();
    static public final String hudIdentifier = "com.haas.easyhunger.hud.hunger";
    private GameMode gameMode;
    private float hungerLevel;
    private float previewHungerRestoration = 0.0f;
    private boolean visible = true;

    public EasyHungerHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float hungerLevel) {
        super(playerRef, hudIdentifier);
        this.gameMode = gameMode;
        this.hungerLevel = hungerLevel;
        hudMap.put(playerRef.getUuid(), this);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        EasyHungerConfig config = EasyHunger.get().getConfig();
        // 1.5.20: ANILLOS o BARRAS (HudEstilo); los dos .ui tienen los mismos ids. Cambiar de estilo pide relog.
        uiCommandBuilder.append(RuneLayout.uiFile(true));
        updateHudPosition(uiCommandBuilder);
        updateGameMode(uiCommandBuilder, this.gameMode);
        updateHungerLevel(uiCommandBuilder, this.hungerLevel);
        updateVisibility(uiCommandBuilder, this.visible);
    }

    protected void updateVisibility(UICommandBuilder uiCommandBuilder, boolean visible) {
        this.visible = visible;
        uiCommandBuilder.set("#EasyHungerContainer.Visible", this.visible);
    }

    protected void updateHudPosition(UICommandBuilder uiCommandBuilder) {
        // Geometria compartida con EasyWaterHud: ver RuneLayout (raiz centrada, 1.5.20).
        uiCommandBuilder.setObject("#EasyHungerRoot.Anchor", RuneLayout.rootAnchor());
        Anchor box = RuneLayout.containerAnchor(true);
        if (box != null) uiCommandBuilder.setObject("#EasyHungerContainer.Anchor", box);
    }

    protected void updateHungerLevel(UICommandBuilder uiCommandBuilder, float hungerLevel) {
        this.hungerLevel = hungerLevel;
        float max = EasyHunger.get().getConfig().getMaxHunger();
        float barValue = hungerLevel / max;
        uiCommandBuilder.set("#EasyHungerHungerBar.Value", barValue);
        uiCommandBuilder.set("#EasyHungerCreativeHungerBar.Value", barValue);

        // Also update preview if active
        if (this.previewHungerRestoration != 0.0f) {
            updateHungerPreview(uiCommandBuilder, this.previewHungerRestoration);
        }
    }

    protected void updateHungerPreview(UICommandBuilder uiCommandBuilder, float hungerRestoration) {
        this.previewHungerRestoration = hungerRestoration;

        if (hungerRestoration == 0.0f) {
            uiCommandBuilder.set("#EasyHungerPreviewBar.Value", 0.0f);
            return;
        }

        float max = EasyHunger.get().getConfig().getMaxHunger();
        float expectedLevel = Math.min(this.hungerLevel + hungerRestoration, max);
        float previewBarValue = expectedLevel / max;

        uiCommandBuilder.set("#EasyHungerPreviewBar.Value", previewBarValue);
    }

    protected void updateGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        this.gameMode = gameMode;
        String iconBackground = gameMode == GameMode.Adventure
            ? "HUD/Hunger/HungerIcon.png"
            : "HUD/Hunger/CreativeHungerIcon.png";
        uiCommandBuilder.set("#EasyHungerIcon.Background", iconBackground);
        uiCommandBuilder.set("#EasyHungerHungerBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#EasyHungerCreativeHungerBar.Visible", gameMode == GameMode.Creative);
    }

    /**
     * true si este jugador tiene HUD registrado en el servidor.
     *
     * Sirve para distinguir "lo tienes oculto" de "no tienes HUD", que hasta ahora se veian igual
     * desde fuera: ambos casos hacian que /eh show no hiciera nada y sin decir nada.
     */
    static public boolean hasHud(@NonNullDecl PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        return uuid != null && hudMap.containsKey(uuid);
    }

    /** Call on PlayerDisconnectEvent to release the map entry for this player. */
    static public void removePlayer(@NonNullDecl PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        if (uuid != null) hudMap.remove(uuid);
    }

    /**
     * Quita la barra del cliente al llegar a un mundo excluido (ver {@code WorldGate}), sin pasar
     * por HudManager.
     *
     * Hace falta saltarse HudManager porque {@code removeCustomHud()} SOLO envia el paquete de
     * remocion si el HUD esta en su mapa — verificado en el bytecode de 0.5.7: hay un {@code ifnull}
     * que se salta el envio. Al cambiar de mundo el jugador estrena entidad y estrena HudManager
     * vacio, con lo cual esa llamada seria un no-op y no limpiaria lo que el cliente ya tenga
     * pintado de antes. Aqui enviamos el mismo paquete que enviaria el server.
     *
     * Esto es una remocion dirigida a NUESTRA key, no tiene que ver con el re-send que en 1.3.1
     * tumbo todos los HUDs del server: aquello reenviaba HUDs enteros sobre la capa compartida de
     * custom UI del cliente.
     */
    static public void forceClear(@NonNullDecl PlayerRef playerRef) {
        removePlayer(playerRef);
        try {
            com.hypixel.hytale.server.core.io.PacketHandler handler = playerRef.getPacketHandler();
            if (handler == null) return;
            handler.writeNoCache(new com.hypixel.hytale.protocol.packets.interface_.CustomHud(
                    hudIdentifier, 0, true, null));
        } catch (Exception ignored) {
            // El jugador puede haberse desconectado entre la comprobacion y el envio.
        }
    }

    static public void updatePlayerHungerLevel(@NonNullDecl PlayerRef playerRef, float hungerLevel) {
        EasyHungerHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerLevel(uiCommandBuilder, hungerLevel);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHungerPreview(@NonNullDecl PlayerRef playerRef, float hungerRestoration) {
        EasyHungerHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHungerPreview(uiCommandBuilder, hungerRestoration);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        EasyHungerHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateGameMode(uiCommandBuilder, gameMode);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudVisibility(@NonNullDecl PlayerRef playerRef, boolean visible) {
        EasyHungerHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateVisibility(uiCommandBuilder, visible);
        hud.update(false, uiCommandBuilder);
    }
}
