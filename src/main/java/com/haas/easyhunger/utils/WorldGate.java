package com.haas.easyhunger.utils;

import com.haas.easyhunger.EasyHunger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Decide si EasyHunger actua en el mundo donde esta ocurriendo algo.
 *
 * Es una LISTA NEGRA ({@code DisabledWorlds} en HungerConfig.json): el mod esta activo en todos
 * los mundos salvo en los listados. Se hizo asi a proposito y no al reves — hay instancias
 * (dungeons, arenas) cuyo nombre cambia en cada creacion y en las que SI se quiere el mod, asi
 * que una lista blanca habria que ampliarla cada vez que aparece un nombre nuevo. Los mundos que
 * quedan fuera (el Hub) tienen nombre fijo.
 *
 * El nombre del mundo se lee con {@code World.getName()}, que existe de verdad en 0.5.7
 * (verificado con javap). NO se usa {@code PlayerRef.getWorld()}: ese metodo no existe y en su
 * dia lanzaba y tragaba una excepcion por cada bloque roto.
 * La via buena es el World del propio Store, que ya
 * es el que usan los eventos del mod.
 *
 * El estado NO se toca en un mundo excluido: se congela. Quien entra al Hub con 12/50 sale con
 * 12/50 — ni drena ni se rellena, para que el Hub no sea un punto de recarga gratis.
 */
public final class WorldGate {

    private WorldGate() {
    }

    /** Version para sistemas ECS: todos reciben el Store del mundo que estan ticando. */
    public static boolean isActive(Store<EntityStore> store) {
        if (store == null) return true;
        EntityStore external = store.getExternalData();
        return external == null || isActive(external.getWorld());
    }

    /** Version para interacciones, que parten de un Ref en vez de un Store. */
    public static boolean isActive(Ref<EntityStore> ref) {
        return ref == null || isActive(ref.getStore());
    }

    public static boolean isActive(World world) {
        return world == null || isActiveName(world.getName());
    }

    public static boolean isActiveName(String worldName) {
        EasyHunger plugin = EasyHunger.get();
        // Ante la duda, activo: preferimos que el mod funcione a que se apague por accidente.
        if (plugin == null) return true;
        return !plugin.getConfig().isWorldDisabled(worldName);
    }
}
