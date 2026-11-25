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
import com.haas.easyhunger.utils.Entrada;
import com.haas.easyhunger.utils.Msg;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

import javax.annotation.Nonnull;

public class EasyHungerHideCommand extends AbstractPlayerCommand {

    /** Shared by /eh hide and /eh show: each one saves the player prefs file, so spamming them is limited. */
    public static final Entrada.Enfriamiento ENFRIAMIENTO_BARRAS = new Entrada.Enfriamiento(3_000L);

    /** True if the player may toggle the bars now; otherwise tells them how long to wait. */
    static boolean pasaEnfriamiento(CommandContext context, UUID uuid) {
        if (uuid == null || ENFRIAMIENTO_BARRAS.intentar(uuid)) return true;
        long segundos = (ENFRIAMIENTO_BARRAS.restanteMs(uuid) + 999L) / 1000L;
        context.sendMessage(Msg.error(uuid, "barras.espera", "segundos", Math.max(1L, segundos)));
        return false;
    }

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
        UUID uuid = playerRef.getUuid();
        if (!pasaEnfriamiento(context, uuid)) {
            return;
        }
        EasyHungerHud.updatePlayerHudVisibility(playerRef, false);
        EasyWaterHud.updatePlayerHudVisibility(playerRef, false);
        if (uuid != null) {
            EasyHunger.get().getPlayerPrefsConfig().setHudVisible(uuid, false);
            EasyHunger.get().savePlayerPrefsConfig();
        }
    }
}
