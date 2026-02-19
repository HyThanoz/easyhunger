package com.haas.easyhunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.haas.easyhunger.compat.hud.HudManager;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.components.HungerComponent;

public class EasyHungerPlayerReady {
    public static void handle(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            // Trigger recipe pruning (will only run if needed based on config)
            com.haas.easyhunger.EasyHunger.get().pruneRecipes();

            HungerComponent hungerComponent = store.ensureAndGetComponent(ref, HungerComponent.getComponentType());
            float hungerLevel = hungerComponent.getHungerLevel();

            boolean thirstEnabled = com.haas.easyhunger.EasyHunger.get().getConfig().isThirstEnabled();
            float thirstLevel = 0.0f;
            if (thirstEnabled) {
                com.haas.easyhunger.components.ThirstComponent thirstComponent = store.ensureAndGetComponent(ref, com.haas.easyhunger.components.ThirstComponent.getComponentType());
                thirstLevel = thirstComponent.getThirstLevel();
            }

            if (HudManager.get().isMultipleHudAvailable()) {
                // MultipleHUD: Register separate HUDs
                EasyHungerHud hud = new EasyHungerHud(playerRef, player.getGameMode(), hungerLevel);
                HudManager.get().setCustomHud(player, playerRef, EasyHungerHud.hudIdentifier, hud);

                if (thirstEnabled) {
                    com.haas.easyhunger.ui.EasyWaterHud waterHud = new com.haas.easyhunger.ui.EasyWaterHud(playerRef, player.getGameMode(), thirstLevel);
                    HudManager.get().setCustomHud(player, playerRef, com.haas.easyhunger.ui.EasyWaterHud.hudIdentifier, waterHud);
                }
            } else {
                // Vanilla / AutoMultiHud: Single combined HUD with both bars
                com.haas.easyhunger.ui.EasyCombinedHud combinedHud = new com.haas.easyhunger.ui.EasyCombinedHud(
                    playerRef, player.getGameMode(), hungerLevel, thirstLevel, thirstEnabled
                );
                player.getHudManager().setCustomHud(playerRef, combinedHud);
            }
        });
    }
}


