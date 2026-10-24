package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.utils.Msg;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class EhVersionCommand extends AbstractPlayerCommand {

    public EhVersionCommand() {
        super("ehversion", "Show EasyHunger version and HUD mode", false);
        // U6: canGeneratePermission() desaparecio de la API. requireNoPermission() es su
        // sustituto: sin esto el comando pasaria a EXIGIR permiso en silencio.
        requireNoPermission();
    }
    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        // Creative only
        Player player = store.getComponent(ref, Player.getComponentType());
        java.util.UUID uuid = playerRef.getUuid();
        if (player != null && player.getGameMode() != GameMode.Creative) {
            context.sendMessage(Msg.error(uuid, "version.solo_creativo"));
            return;
        }

        // Real version from the plugin manifest (was hardcoded and permanently stale before)
        String version = String.valueOf(
                com.haas.easyhunger.EasyHunger.get().getManifest().getVersion());

        context.sendMessage(Msg.info(uuid, "version.version", "version", version));
        context.sendMessage(Msg.info(uuid, "version.modo"));
    }
}
