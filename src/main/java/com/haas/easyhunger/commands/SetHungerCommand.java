package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.utils.Msg;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SetHungerCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.hunger.set.self";
    private final RequiredArg<Float> hungerLevel = this.withRequiredArg("hungerLevel", "A value between 0 and 100", ArgTypes.FLOAT);

    public SetHungerCommand() {
        super("hset", "Set own hunger level", false);
        this.requirePermission(requiredPermission);
        this.addUsageVariant(new SetHungerOtherCommand());
    }

    private static void setHungerLevel (
        @NonNullDecl CommandContext context,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl PlayerRef targetPlayerRef,
        float newHungerLevel
    ) {
        HungerComponent hungerComponent = store.getComponent(ref, HungerComponent.getComponentType());
        if (hungerComponent == null) {
            context.sendMessage(Msg.error(Msg.quien(context), "nivel.sin_hambre"));
            return;
        }

        if (newHungerLevel < 0 || newHungerLevel > 100) {
            context.sendMessage(Msg.error(Msg.quien(context), "nivel.hambre_rango"));
            return;
        }
        hungerComponent.setHungerLevel(newHungerLevel);
        context.sendMessage(Msg.info(Msg.quien(context), "nivel.hambre_ok",
                "jugador", targetPlayerRef.getUsername(), "nivel", newHungerLevel));
        EasyHungerHud.updatePlayerHungerLevel(targetPlayerRef, newHungerLevel);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        float newHungerLevel = this.hungerLevel.get(context);
        setHungerLevel(context, store, ref, playerRef, newHungerLevel);
    }

    private static class SetHungerOtherCommand extends CommandBase {
        public static final String requiredPermission = "easyhunger.hunger.set.other";
        private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        private final RequiredArg<Float> hungerLevel = this.withRequiredArg("hungerLevel", "A value between 0 and 100", ArgTypes.FLOAT);

        public SetHungerOtherCommand() {
            super("Set another player's hunger level");
            this.requirePermission(requiredPermission);
        }

        @Override
        protected void executeSync(@NonNullDecl CommandContext context) {
            PlayerRef targetPlayerRef = this.playerArg.get(context);
            float newHungerLevel = this.hungerLevel.get(context);
            Ref<EntityStore> ref = targetPlayerRef.getReference();
            if (ref == null || !ref.isValid()) {
                context.sendMessage(Msg.error(Msg.quien(context), "nivel.sin_jugador"));
                return;
            }
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> setHungerLevel(context, store, ref, targetPlayerRef, newHungerLevel));
        }
    }
}



