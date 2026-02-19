package com.haas.easyhunger.compat.hud;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.haas.easyhunger.EasyHunger;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class HudManager implements HudProvider {
    private final HudProvider hudProvider;
    private final boolean multipleHudAvailable;
    private static final HudManager instance = new HudManager();

    public HudManager() {
        PluginBase multiplehud = PluginManager.get().getPlugin(PluginIdentifier.fromString("Buuz135:MultipleHUD"));
        if (multiplehud == null || !multiplehud.isEnabled()) {
            hudProvider = new DefaultHudProvider();
            multipleHudAvailable = false;
            EasyHunger.logInfo("MultipleHUD not found. Using combined vanilla HUD (compatible with AutoMultiHud).");
        } else {
            hudProvider = new MultipleHudProvider();
            multipleHudAvailable = true;
            EasyHunger.logInfo("MultipleHUD detected. Using separate HUDs.");
        }
    }

    public boolean isMultipleHudAvailable() {
        return multipleHudAvailable;
    }

    public void setCustomHud(
        @NonNullDecl Player player,
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl String hudIdentifier,
        @NonNullDecl CustomUIHud hud
    ) {
        hudProvider.setCustomHud(player, playerRef, hudIdentifier, hud);
    }

    public static HudManager get() {
        return instance;
    }
}
