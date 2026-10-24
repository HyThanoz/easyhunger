package com.haas.easyhunger.components;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import javax.annotation.Nullable;

/**
 * Pausa por inactividad (IdlePause*): estado y logica compartidos por el hambre y la sed.
 *
 * Antes estaba copiado casi literal en StarveSystem y EasyThirstSystem, con los mismos campos
 * de posicion y rotacion duplicados en HungerComponent y ThirstComponent. Cada componente tiene
 * su propio IdleTracker, asi que el hambre y la sed siguen contando por separado.
 *
 * No es un componente ni se persiste: vive dentro de HungerComponent y ThirstComponent.
 */
public final class IdleTracker {

    /** Distancia al cuadrado por debajo de la cual se considera que no se movio (~0,1 bloques). */
    private static final double POS_EPSILON_SQ = 0.01;

    /** Giro de camara por debajo del cual se considera que no se movio (~0,5 rad). */
    private static final float ROT_EPSILON = 0.5f;

    private float idleTime = 0.0f;
    private double lastPosX = Double.NaN;
    private double lastPosY = Double.NaN;
    private double lastPosZ = Double.NaN;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastRoll = Float.NaN;

    /**
     * Copia solo el tiempo acumulado, como hacian los constructores de copia de los componentes:
     * la ultima posicion y rotacion no se clonaban.
     */
    void copyIdleTimeFrom(IdleTracker other) {
        this.idleTime = other.idleTime;
    }

    /**
     * Se llama una vez por tick de hambre/sed, con IdlePause activado. Guarda la posicion y la
     * rotacion actuales, suma el ritmo del tick si el jugador no se movio (o reinicia si se movio)
     * y devuelve true si lleva quieto al menos IdlePauseSeconds, es decir, si hay que pausar.
     *
     * Sin transform o sin posicion no toca nada y devuelve false.
     */
    public boolean updateAndIsPaused(@Nullable TransformComponent transform) {
        if (transform == null || transform.getPosition() == null) return false;

        org.joml.Vector3d pos = transform.getPosition();
        Rotation3f rot = transform.getRotation();
        double px = pos.x, py = pos.y, pz = pos.z;
        float ry = rot != null ? rot.yaw()   : 0f;
        float rp = rot != null ? rot.pitch() : 0f;
        float rr = rot != null ? rot.roll()  : 0f;
        boolean stationary = false;
        if (hasLastPos() && hasLastRotation()) {
            double dx = px - lastPosX;
            double dy = py - lastPosY;
            double dz = pz - lastPosZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            float dyaw = Math.abs(ry - lastYaw);
            float dpitch = Math.abs(rp - lastPitch);
            float droll = Math.abs(rr - lastRoll);
            stationary = distSq <= POS_EPSILON_SQ
                && dyaw <= ROT_EPSILON && dpitch <= ROT_EPSILON && droll <= ROT_EPSILON;
        }
        lastPosX = px;
        lastPosY = py;
        lastPosZ = pz;
        lastYaw = ry;
        lastPitch = rp;
        lastRoll = rr;
        if (stationary) {
            EasyHungerConfig config = EasyHunger.get().getConfig();
            idleTime += config.getStarvationTickRate();
            return idleTime >= config.getIdlePauseSeconds();
        }
        idleTime = 0.0f;
        return false;
    }

    private boolean hasLastPos() { return !Double.isNaN(lastPosX); }
    private boolean hasLastRotation() { return !Float.isNaN(lastYaw); }
}
