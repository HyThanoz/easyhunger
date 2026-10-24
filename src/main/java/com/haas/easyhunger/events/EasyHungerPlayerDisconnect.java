package com.haas.easyhunger.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.haas.easyhunger.ui.EasyHungerHud;
import com.haas.easyhunger.ui.EasyWaterHud;
import com.haas.easyhunger.utils.HungerProtectionUtils;

import java.util.UUID;

/**
 * Limpia el estado per-jugador que TODAVIA vive fuera de la entidad.
 *
 * Historia: en 1.3.1 este listener nacio para vaciar cinco mapas estaticos que crecian con cada
 * jugador conectado. Tres de ellos (los cooldowns de salto x2 y el de colocacion) ya no existen:
 * su estado se movio a {@link com.haas.easyhunger.components.PlayerActionTimes}, un componente de
 * la entidad que se libera con ella. Ver
 * {@link com.haas.easyhunger.systems.PlayerActionTimesProvisionSystem}.
 *
 * Lo que queda aqui NO se puede colgar de la entidad con la misma facilidad:
 *   - Los mapas de HUD ({@code EasyHungerHud}/{@code EasyWaterHud}) guardan el objeto Hud, al que
 *     se llega desde metodos estaticos que solo reciben un PlayerRef (interacciones, comandos).
 *   - El cache de zonas seguras de {@link HungerProtectionUtils} se consulta desde sitios sin Ref.
 *
 * ⚠️ Este evento NO se dispara al cambiar de mundo, solo al salir del servidor. Es otra razon por
 * la que el estado que puede vivir en la entidad debe vivir ahi.
 */
public class EasyHungerPlayerDisconnect {
    public static void handle(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) return;

        UUID uuid = playerRef.getUuid();
        if (uuid != null) {
            HungerProtectionUtils.invalidateCache(uuid);
            EasyHungerPlayerReady.forgetPendingSend(uuid);
        }

        EasyHungerHud.removePlayer(playerRef);
        EasyWaterHud.removePlayer(playerRef);
    }
}
