package com.haas.easyhunger.utils;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.UUID;

/**
 * Low hunger / thirst warnings: an on-screen notification (and optional sound) when the level
 * drops below the "Hungry"/"Thirsty" threshold (stage 1) and again at the critical threshold
 * (stage 2).
 *
 * The caller keeps the stage already shown in the component and passes it back every tick, so
 * a warning is sent only when the stage gets WORSE. Eating or drinking lowers the stage again
 * and re-arms the warning. Players can turn it off with /eh avisos.
 */
public final class AvisoNivelBajo {

    private AvisoNivelBajo() {}

    public enum Tipo {
        HAMBRE("Food_Bread", "aviso.hambre", "aviso.hambre_critico"),
        SED("Ingredient_Water_Essence", "aviso.sed", "aviso.sed_critico");

        /** Item shown as the notification icon. */
        final String item;
        final String claveBajo;
        final String claveCritico;

        Tipo(String item, String claveBajo, String claveCritico) {
            this.item = item;
            this.claveBajo = claveBajo;
            this.claveCritico = claveCritico;
        }
    }

    /**
     * @return the new stage (0 fine, 1 low, 2 critical) to store in the component. Never throws:
     *         on any error it keeps the previous stage, so a warning can never break the tick.
     */
    public static int revisar(PlayerRef pr, Tipo tipo, float nivel, float umbralBajo, int etapaAnterior) {
        try {
            EasyHungerConfig cfg = EasyHunger.get().getConfig();
            float umbralCritico = Math.min(cfg.getCriticalWarningThreshold(), umbralBajo);
            int etapa = nivel <= umbralCritico ? 2 : (nivel < umbralBajo ? 1 : 0);
            if (etapa > etapaAnterior && cfg.isLowWarningsEnabled() && pr != null) {
                UUID uuid = pr.getUuid();
                if (uuid == null || EasyHunger.get().getPlayerPrefsConfig().isWarningsOn(uuid)) {
                    enviar(pr, uuid, tipo, etapa == 2, cfg.getWarningSound());
                }
            }
            return etapa;
        } catch (Throwable t) {
            return etapaAnterior;
        }
    }

    private static void enviar(PlayerRef pr, UUID uuid, Tipo tipo, boolean critico, String sonido) {
        try {
            ItemWithAllMetadata item = null;
            try {
                item = new ItemStack(tipo.item).toPacket();
            } catch (Throwable ignored) {
                // No icon if the item does not exist; the text is still sent.
            }
            Message titulo = Message.raw(Lang.t(uuid, critico ? tipo.claveCritico : tipo.claveBajo));
            String clavecuerpo = critico ? null : tipo.claveBajo + "_cuerpo";
            Message cuerpo = clavecuerpo == null ? null : Message.raw(Lang.t(uuid, clavecuerpo));
            NotificationStyle estilo = critico ? NotificationStyle.Danger : NotificationStyle.Warning;
            NotificationUtil.sendNotification(pr.getPacketHandler(), titulo, cuerpo, null, item, estilo);
        } catch (Throwable ignored) {
            // A failed notification must not stop the sound or the tick.
        }

        try {
            if (sonido != null && !sonido.isBlank()) {
                SoundUtil.playSoundEvent2dToPlayer(pr, sonido, SoundCategory.SFX);
            }
        } catch (Throwable ignored) {
            // Unknown sound id: ignore.
        }
    }
}
