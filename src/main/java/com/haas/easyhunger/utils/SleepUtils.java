package com.haas.easyhunger.utils;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Utility class for detecting player sleep state.
 */
public class SleepUtils {
    
    /**
     * Check if a player is currently sleeping (in bed, not awake).
     * 
     * @param index The entity index in the archetype chunk
     * @param archetypeChunk The archetype chunk containing the entity
     * @return true if player is sleeping, false if awake or component not found
     */
    public static boolean isSleeping(int index, ArchetypeChunk<EntityStore> archetypeChunk) {
        PlayerSomnolence somnolence = archetypeChunk.getComponent(index, PlayerSomnolence.getComponentType());
        if (somnolence == null) {
            return false;
        }

        // Se compara el ESTADO, no el componente.
        //
        // Antes esto era `somnolence != PlayerSomnolence.AWAKE`, y eso comparaba la instancia del
        // componente contra un singleton estatico. Verificado con javap sobre 0.5.7:
        //   - PlayerSomnolence NO es un enum: es una clase con AWAKE como campo estatico, creado
        //     en su <clinit> como `new PlayerSomnolence(FullyAwake.INSTANCE)`.
        //   - Su clone() SIEMPRE devuelve una instancia nueva copiando el campo `state`
        //     (bytecode: new + <init> + getfield state + putfield state + areturn).
        // Cualquier copia del componente deja de ser AWAKE por referencia aunque su estado sea
        // "despierto" -> isSleeping() devolvia true para un jugador despierto y, con
        // PauseWhileSleeping activo, hambre y sed dejaban de bajar en silencio.
        //
        // PlayerSleep.FullyAwake SI es un enum de una sola constante, asi que comparar el estado
        // por referencia es correcto y sobrevive al clonado (clone copia la referencia del estado).
        PlayerSleep state = somnolence.getSleepState();
        if (state == null) {
            return false; // Ante la duda, no pausar: preferimos que el mod siga funcionando.
        }
        // Despierto = FullyAwake. Cualquier otro estado (NoddingOff, Slumber, MorningWakeUp) duerme.
        return state != PlayerSleep.FullyAwake.INSTANCE;
    }
}
