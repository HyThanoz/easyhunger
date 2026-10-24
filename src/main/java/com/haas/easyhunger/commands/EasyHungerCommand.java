package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.utils.ChatStyle;
import com.haas.easyhunger.utils.Lang;
import com.haas.easyhunger.utils.Msg;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

import javax.annotation.Nonnull;

public class EasyHungerCommand extends AbstractPlayerCommand {

    public EasyHungerCommand() {
        super("easyhunger", "EasyHunger Base Command", false);
        // U6: canGeneratePermission() desaparecio de la API. requireNoPermission() es su
        // sustituto: sin esto el comando pasaria a EXIGIR permiso en silencio.
        requireNoPermission();
        this.addAliases("eh");

        this.addSubCommand(new EasyHungerHideCommand());
        this.addSubCommand(new EasyHungerShowCommand());
        this.addSubCommand(new EhAddAllCommand());
    }

    /**
     * Player-facing QoL command: open to everyone. Returning false prevents the engine from
     * auto-generating a "<group>.<plugin>.command.easyhunger" node (null permission = allowed).
     * Admin subcommands (addall) keep their own explicit permission nodes.
     */
        @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        // Default execution if no subcommand matches
        // The addall command should only show up if they have its specific permission, which is handled
        // intrinsically by the command engine. We'll simply omit it from the hardcoded help menu.

        UUID uuid = playerRef.getUuid();
        playerRef.sendMessage(ChatStyle.header(Lang.t(uuid, "ayuda.titulo")));
        playerRef.sendMessage(Msg.suelta(uuid, "ayuda.hide"));
        playerRef.sendMessage(Msg.suelta(uuid, "ayuda.show"));
        playerRef.sendMessage(Msg.suelta(uuid, "ayuda.config"));
    }
}
