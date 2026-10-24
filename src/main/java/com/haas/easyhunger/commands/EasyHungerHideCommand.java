package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.ui.EasyWaterHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

import javax.annotation.Nonnull;

public class EasyHungerHideCommand extends AbstractPlayerCommand {

    public EasyHungerHideCommand() {
        super("hide", "Hide Hunger and Thirst HUDs", false);
        // U6: canGeneratePermission() desaparecio de la API. requireNoPermission() es su
        // sustituto: sin esto el comando pasaria a EXIGIR permiso en silencio.
        requireNoPermission();
    }
    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        EasyHungerHud.updatePlayerHudVisibility(playerRef, false);
        EasyWaterHud.updatePlayerHudVisibility(playerRef, false);
        UUID uuid = playerRef.getUuid();
        if (uuid != null) {
            EasyHunger.get().getPlayerPrefsConfig().setHudVisible(uuid, false);
            EasyHunger.get().savePlayerPrefsConfig();
        }
    }
}
