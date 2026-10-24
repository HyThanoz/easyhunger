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
import com.haas.easyhunger.utils.Msg;
import com.haas.easyhunger.utils.WorldGate;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

import javax.annotation.Nonnull;

public class EasyHungerShowCommand extends AbstractPlayerCommand {

    public EasyHungerShowCommand() {
        super("show", "Show Hunger and Thirst HUDs", false);
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
        // La preferencia se guarda SIEMPRE, aunque no haya HUD que mostrar: asi al reconectar
        // ya sale visible.
        UUID uuid = playerRef.getUuid();
        if (uuid != null) {
            EasyHunger.get().getPlayerPrefsConfig().setHudVisible(uuid, true);
            EasyHunger.get().savePlayerPrefsConfig();
        }

        // Mundo excluido (el Hub): aqui no hay HUD a proposito. Sin este aviso, el jugador cree
        // que el comando esta roto.
        if (!WorldGate.isActive(world)) {
            context.sendMessage(Msg.info(uuid, "barras.mundo_sin_hambre"));
            return;
        }

        boolean tenia = EasyHungerHud.hasHud(playerRef);

        if (tenia) {
            // Camino normal: el HUD existe, solo estaba oculto. Un `set` incremental, nada mas.
            EasyHungerHud.updatePlayerHudVisibility(playerRef, true);
            EasyWaterHud.updatePlayerHudVisibility(playerRef, true);
            context.sendMessage(Msg.info(uuid, "barras.activadas"));
            return;
        }

        // RESCATE: no hay HUD registrado, asi que se construye ahora.
        //
        // Esto NO es el reenvio que en 1.3.1 tumbo todos los HUDs del servidor. Aquello reenviaba
        // un HUD que YA existia, automaticamente, a todo el mundo y dentro de la ventana del join.
        // Aqui la condicion es la contraria — solo se entra si el jugador NO tiene HUD — y lo pide
        // el a mano, mucho despues de entrar. Para un cliente que perdio el paquete inicial esto
        // es su PRIMER envio de la sesion, no un segundo.
        //
        // La puerta es `hasHud`: mientras exista, no se reconstruye nunca.
        EasyHunger.logInfo("[HUD] Rescate manual solicitado por " + playerRef.getUsername()
                + " — no tenia HUD registrado; se reconstruye.");
        com.haas.easyhunger.events.EasyHungerPlayerReady.createHuds(ref, store);

        if (EasyHungerHud.hasHud(playerRef)) {
            context.sendMessage(Msg.info(uuid, "barras.rescate_ok"));
        } else {
            context.sendMessage(Msg.error(uuid, "barras.rescate_fallo"));
        }
    }
}
