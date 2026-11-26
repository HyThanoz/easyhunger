package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.utils.ChatStyle;
import com.haas.easyhunger.utils.Msg;

import java.util.Arrays;
import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * /eh link: where to get this mod's source code. AGPL-3.0 §13 asks that every player of a
 * modified version be offered its source; /eh lists this command (1.5.32).
 */
public class EasyHungerLinkCommand extends AbstractPlayerCommand {

    public static final String SOURCE_URL = "https://github.com/HyThanoz/easyhunger";
    public static final String SOURCE_TEXT = "github.com/HyThanoz/easyhunger";

    public EasyHungerLinkCommand() {
        super("link", "Where to get the EasyHunger source code (AGPL-3.0)", false);
        // Open to every player, like hide/show/avisos.
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
        Message[] texto = Msg.partes(uuid, "link.codigo");
        Message[] partes = Arrays.copyOf(texto, texto.length + 1);
        partes[texto.length] = ChatStyle.link(SOURCE_TEXT, SOURCE_URL);
        context.sendMessage(ChatStyle.info(partes));
    }
}
