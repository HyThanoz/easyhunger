package com.haas.easyhunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.components.ThirstComponent;
import com.haas.easyhunger.config.EasyHungerConfig;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EasyWaterHud extends CustomUIHud {
    /**
     * Keyed by player UUID (not PlayerRef) so lookups are reliable regardless of which
     * PlayerRef instance the caller holds — PlayerRef does not override equals/hashCode.
     */
    static private final ConcurrentHashMap<UUID, EasyWaterHud> hudMap = new ConcurrentHashMap<>();
    static public final String hudIdentifier = "com.haas.easyhunger.hud.water";
    private GameMode gameMode;
    private float thirstLevel;
    private float previewThirstRestoration = 0.0f;
    private boolean visible = true;

    public EasyWaterHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float thirstLevel) {
        super(playerRef, hudIdentifier);
        this.gameMode = gameMode;
        this.thirstLevel = thirstLevel;
        hudMap.put(playerRef.getUuid(), this);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        EasyHungerConfig config = EasyHunger.get().getConfig();
        // 1.5.20: ANILLOS o BARRAS (HudEstilo); los dos .ui tienen los mismos ids. Cambiar de estilo pide relog.
        uiCommandBuilder.append(RuneLayout.uiFile(false));
        updateHudPosition(uiCommandBuilder);
        updateGameMode(uiCommandBuilder, this.gameMode);
        updateThirstLevel(uiCommandBuilder, this.thirstLevel);
        updateVisibility(uiCommandBuilder, this.visible);
    }

    protected void updateVisibility(UICommandBuilder uiCommandBuilder, boolean visible) {
        this.visible = visible;
        uiCommandBuilder.set("#EasyWaterContainer.Visible", this.visible);
    }

    protected void updateHudPosition(UICommandBuilder uiCommandBuilder) {
        // Geometria compartida con EasyHungerHud: ver RuneLayout (raiz centrada, 1.5.20).
        uiCommandBuilder.setObject("#EasyWaterRoot.Anchor", RuneLayout.rootAnchor());
        Anchor box = RuneLayout.containerAnchor(false);
        if (box != null) uiCommandBuilder.setObject("#EasyWaterContainer.Anchor", box);
    }

    protected void updateThirstLevel(UICommandBuilder uiCommandBuilder, float thirstLevel) {
        this.thirstLevel = thirstLevel;
        float max = EasyHunger.get().getConfig().getMaxThirst();
        float barValue = thirstLevel / max;
        uiCommandBuilder.set("#EasyWaterThirstBar.Value", barValue);
        uiCommandBuilder.set("#EasyWaterCreativeThirstBar.Value", barValue);

        // Also update preview if active
        if (this.previewThirstRestoration != 0.0f) {
            updateThirstPreview(uiCommandBuilder, this.previewThirstRestoration);
        }
    }

    protected void updateThirstPreview(UICommandBuilder uiCommandBuilder, float thirstRestoration) {
        this.previewThirstRestoration = thirstRestoration;

        if (thirstRestoration == 0.0f) {
            uiCommandBuilder.set("#EasyWaterPreviewBar.Value", 0.0f);
            return;
        }

        float max = EasyHunger.get().getConfig().getMaxThirst();
        float expectedLevel = Math.min(this.thirstLevel + thirstRestoration, max);
        float previewBarValue = expectedLevel / max;

        uiCommandBuilder.set("#EasyWaterPreviewBar.Value", previewBarValue);
    }

    protected void updateGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        this.gameMode = gameMode;
        String iconBackground = gameMode == GameMode.Adventure
            ? "HUD/Hunger/WaterIcon.png"
            : "HUD/Hunger/CreativeWaterIcon.png";
        uiCommandBuilder.set("#EasyWaterIcon.Background", iconBackground);
        uiCommandBuilder.set("#EasyWaterThirstBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#EasyWaterCreativeThirstBar.Visible", gameMode == GameMode.Creative);
    }

    /** true si este jugador tiene HUD de sed registrado. Ver {@link EasyHungerHud#hasHud}. */
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
     * Quita la barra del cliente al llegar a un mundo excluido (ver {@code WorldGate}).
     * Motivo de no usar HudManager y detalle del paquete: {@link EasyHungerHud#forceClear}.
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

    static public void updatePlayerThirstLevel(@NonNullDecl PlayerRef playerRef, float thirstLevel) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstLevel(uiCommandBuilder, thirstLevel);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerThirstPreview(@NonNullDecl PlayerRef playerRef, float thirstRestoration) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstPreview(uiCommandBuilder, thirstRestoration);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateGameMode(uiCommandBuilder, gameMode);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudVisibility(@NonNullDecl PlayerRef playerRef, boolean visible) {
        EasyWaterHud hud = hudMap.get(playerRef.getUuid());
        if (hud == null) return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateVisibility(uiCommandBuilder, visible);
        hud.update(false, uiCommandBuilder);
    }
}
