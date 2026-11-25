package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.config.PlayerPrefsConfig;
import com.haas.easyhunger.utils.Entrada;
import com.haas.easyhunger.utils.Msg;

import java.util.UUID;

import javax.annotation.Nonnull;

/** /eh avisos: turns the player's low hunger/thirst warnings on or off (see AvisoNivelBajo). */
public class EasyHungerAvisosCommand extends AbstractPlayerCommand {

    /** Each toggle saves the player prefs file, so it is limited to once every 3 s. */
    public static final Entrada.Enfriamiento ENFRIAMIENTO = new Entrada.Enfriamiento(3_000L);

    public EasyHungerAvisosCommand() {
        super("avisos", "Turn hunger and thirst warnings on or off", false);
        // Open to every player, like hide/show.
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
        if (uuid == null) return;

        if (!ENFRIAMIENTO.intentar(uuid)) {
            long segundos = (ENFRIAMIENTO.restanteMs(uuid) + 999L) / 1000L;
            context.sendMessage(Msg.error(uuid, "avisos.espera", "segundos", Math.max(1L, segundos)));
            return;
        }

        PlayerPrefsConfig prefs = EasyHunger.get().getPlayerPrefsConfig();
        boolean encender = !prefs.isWarningsOn(uuid);
        prefs.setWarningsOn(uuid, encender);
        EasyHunger.get().savePlayerPrefsConfig();
        context.sendMessage(Msg.info(uuid, encender ? "avisos.activados" : "avisos.desactivados"));
    }
}
