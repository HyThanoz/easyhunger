package com.haas.easyhunger.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

public class EasyHungerConfig {
    
    // Original fields codecs
    private static final KeyedCodec<Integer> MAX_HUNGER = new KeyedCodec<>("MaxHunger", Codec.INTEGER);
    private static final KeyedCodec<Float> STARVATION_TICK_RATE = new KeyedCodec<>("StarvationTickRate", Codec.FLOAT);
    private static final KeyedCodec<Float> STARVATION_PER_TICK = new KeyedCodec<>("StarvationPerTick", Codec.FLOAT);
    private static final KeyedCodec<Float> STARVATION_STAMINA_MODIFIER = new KeyedCodec<>("StarvationStaminaModifier", Codec.FLOAT);
    private static final KeyedCodec<Float> HUNGRY_THRESHOLD = new KeyedCodec<>("HungryThreshold", Codec.FLOAT);
    private static final KeyedCodec<Float> STARVATION_DAMAGE = new KeyedCodec<>("StarvationDamage", Codec.FLOAT);


    private static final KeyedCodec<String> HUD_POSITION = new KeyedCodec<>("HudPosition", Codec.STRING);
    private static final KeyedCodec<Integer> HUD_OFFSET_Y     = new KeyedCodec<>("HudOffsetY",     Codec.INTEGER);
    private static final KeyedCodec<Integer> HUD_RIGHT_MARGIN = new KeyedCodec<>("HudRightMargin", Codec.INTEGER);
    private static final KeyedCodec<Integer> HUD_INIT_DELAY_MS = new KeyedCodec<>("HudInitDelayMs", Codec.INTEGER);
    // 1.5.20: ANILLOS (en espejo del espacio Z) o BARRAS (encima de vida y stamina). HudPosition y
    // HudRightMargin quedan en el archivo por compatibilidad, pero ya no se usan.
    private static final KeyedCodec<String> HUD_ESTILO = new KeyedCodec<>("HudEstilo", Codec.STRING);
    private static final KeyedCodec<Boolean> HUD_AUTO_RECOVER = new KeyedCodec<>("HudAutoRecoverEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Integer> HUD_AUTO_RECOVER_INTERVAL_MS = new KeyedCodec<>("HudAutoRecoverIntervalMs", Codec.INTEGER);
    private static final KeyedCodec<Float> BLOCK_BREAK_HUNGER_COST = new KeyedCodec<>("BlockBreakHungerCost", Codec.FLOAT);
    private static final KeyedCodec<Float> JUMP_HUNGER_COST = new KeyedCodec<>("JumpHungerCost", Codec.FLOAT);
    
    // Thirst Configuration
    private static final KeyedCodec<Boolean> THIRST_ENABLED = new KeyedCodec<>("ThirstEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Integer> MAX_THIRST = new KeyedCodec<>("MaxThirst", Codec.INTEGER);
    private static final KeyedCodec<Float> THIRST_DECAY_RATE = new KeyedCodec<>("ThirstDecayRate", Codec.FLOAT);
    private static final KeyedCodec<Float> SPRINT_THIRST_MULTIPLIER = new KeyedCodec<>("SprintThirstMultiplier", Codec.FLOAT);
    private static final KeyedCodec<Float> THIRSTY_THRESHOLD = new KeyedCodec<>("ThirstyThreshold", Codec.FLOAT);


    private static final KeyedCodec<Float> THIRST_DAMAGE = new KeyedCodec<>("ThirstDamage", Codec.FLOAT);
    private static final KeyedCodec<Float> BLOCK_BREAK_THIRST_COST = new KeyedCodec<>("BlockBreakThirstCost", Codec.FLOAT);
    private static final KeyedCodec<Float> JUMP_THIRST_COST = new KeyedCodec<>("JumpThirstCost", Codec.FLOAT);
    private static final KeyedCodec<Boolean> PAUSE_WHILE_SLEEPING = new KeyedCodec<>("PauseWhileSleeping", Codec.BOOLEAN);

    // Well Fed Configuration
    private static final KeyedCodec<Boolean> WELL_FED_ENABLED = new KeyedCodec<>("WellFedEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Float> WELL_FED_THRESHOLD = new KeyedCodec<>("WellFedThreshold", Codec.FLOAT);

    // Idle Pause Configuration
    private static final KeyedCodec<Boolean> IDLE_PAUSE_ENABLED = new KeyedCodec<>("IdlePauseEnabled", Codec.BOOLEAN);
    private static final KeyedCodec<Float> IDLE_PAUSE_SECONDS = new KeyedCodec<>("IdlePauseSeconds", Codec.FLOAT);

    // Mundos excluidos (lista negra — ver WorldGate)
    private static final KeyedCodec<String[]> DISABLED_WORLDS = new KeyedCodec<>("DisabledWorlds", Codec.STRING_ARRAY);


    public static final BuilderCodec<EasyHungerConfig> CODEC = BuilderCodec.builder(EasyHungerConfig.class, EasyHungerConfig::new)
            // === HUNGER / FOOD CONFIGS ===
            .append(MAX_HUNGER, (c, v) -> c.maxHunger = v, EasyHungerConfig::getMaxHunger).add()
            .append(STARVATION_TICK_RATE, (c, v) -> c.starvationTickRate = v, EasyHungerConfig::getStarvationTickRate).add()
            .append(STARVATION_PER_TICK, (c, v) -> c.starvationPerTick = v, EasyHungerConfig::getStarvationPerTick).add()
            .append(STARVATION_STAMINA_MODIFIER, (c, v) -> c.starvationStaminaModifier = v, EasyHungerConfig::getStarvationStaminaModifier).add()
            .append(HUNGRY_THRESHOLD, (c, v) -> c.hungryThreshold = v, EasyHungerConfig::getHungryThreshold).add()
            .append(STARVATION_DAMAGE, (c, v) -> c.starvationDamage = v, EasyHungerConfig::getStarvationDamage).add()
            .append(BLOCK_BREAK_HUNGER_COST, (c, v) -> c.blockBreakHungerCost = v, EasyHungerConfig::getBlockBreakHungerCost).add()
            .append(JUMP_HUNGER_COST, (c, v) -> c.jumpHungerCost = v, EasyHungerConfig::getJumpHungerCost).add()

            // === THIRST / WATER CONFIGS ===
            .append(THIRST_ENABLED, (c, v) -> c.thirstEnabled = v, EasyHungerConfig::isThirstEnabled).add()
            .append(MAX_THIRST, (c, v) -> c.maxThirst = v, EasyHungerConfig::getMaxThirst).add()
            .append(THIRST_DECAY_RATE, (c, v) -> c.thirstDecayRate = v, EasyHungerConfig::getThirstDecayRate).add()
            .append(SPRINT_THIRST_MULTIPLIER, (c, v) -> c.sprintThirstMultiplier = v, EasyHungerConfig::getSprintThirstMultiplier).add()
            .append(THIRST_DAMAGE, (c, v) -> c.thirstDamage = v, EasyHungerConfig::getThirstDamage).add()
            .append(THIRSTY_THRESHOLD, (c, v) -> c.thirstyThreshold = v, EasyHungerConfig::getThirstyThreshold).add()
            .append(BLOCK_BREAK_THIRST_COST, (c, v) -> c.blockBreakThirstCost = v, EasyHungerConfig::getBlockBreakThirstCost).add()
            .append(JUMP_THIRST_COST, (c, v) -> c.jumpThirstCost = v, EasyHungerConfig::getJumpThirstCost).add()

            // === HUD POSITION ===
            .append(HUD_POSITION, (c, v) -> c.hudPosition = parseHudPositionOrDefault(v), c -> c.getHudPosition().name()).add()
            .append(HUD_OFFSET_Y,     (c, v) -> c.hudOffsetY    = v, EasyHungerConfig::getHudOffsetY).add()
            .append(HUD_RIGHT_MARGIN, (c, v) -> c.hudRightMargin = v, EasyHungerConfig::getHudRightMargin).add()
            .append(HUD_ESTILO, (c, v) -> c.hudEstilo = parseHudEstilo(v), EasyHungerConfig::getHudEstilo).add()
            .append(HUD_INIT_DELAY_MS, (c, v) -> c.hudInitDelayMs = v, EasyHungerConfig::getHudInitDelayMs).add()
            .append(HUD_AUTO_RECOVER, (c, v) -> c.hudAutoRecoverEnabled = v, EasyHungerConfig::isHudAutoRecoverEnabled).add()
            .append(HUD_AUTO_RECOVER_INTERVAL_MS, (c, v) -> c.hudAutoRecoverIntervalMs = v, EasyHungerConfig::getHudAutoRecoverIntervalMs).add()


            // === SLEEP PAUSE ===
            .append(PAUSE_WHILE_SLEEPING, (c, v) -> c.pauseWhileSleeping = v, EasyHungerConfig::isPauseWhileSleeping).add()

            // === WELL FED BONUS ===
            .append(WELL_FED_ENABLED, (c, v) -> c.wellFedEnabled = v, EasyHungerConfig::isWellFedEnabled).add()
            .append(WELL_FED_THRESHOLD, (c, v) -> c.wellFedThreshold = v, EasyHungerConfig::getWellFedThreshold).add()

            // === IDLE PAUSE ===
            .append(IDLE_PAUSE_ENABLED, (c, v) -> c.idlePauseEnabled = v, EasyHungerConfig::isIdlePauseEnabled).add()
            .append(IDLE_PAUSE_SECONDS, (c, v) -> c.idlePauseSeconds = v, EasyHungerConfig::getIdlePauseSeconds).add()

            // === MUNDOS EXCLUIDOS ===
            .append(DISABLED_WORLDS, EasyHungerConfig::setDisabledWorlds, EasyHungerConfig::getDisabledWorlds).add()
            .build();

    // === HARD-balanced defaults (v1.2.6) ===
    // Calibrated for active Hytale gameplay (sprint/combat heavy).
    // Hunger and thirst drain sources are now symmetric; values tuned so both
    // resources stay relevant without feeling oppressive.
    private Integer maxHunger = 50;
    private float starvationTickRate = 5f;          // Chunky 5s ticks (visible bar updates)
    private float starvationPerTick = 0.10f;        // 0.02/s walking → 42min full→0

    public Integer getMaxHunger() { return maxHunger; }
    private float starvationStaminaModifier = 0.5f; // Sprint/combat punishes hunger meaningfully
    private float hungryThreshold = 20.0f;          // "Hungry" effect at 40% hunger
    private float starvationDamage = 5.0f;          // 1 HP/s at 0 hunger (with 5s tick)
    private float thirstDamage = 5.0f;              // Same for thirst

    private HudPosition hudPosition = HudPosition.BOTTOM;
    /** Pixel offset added to the preset bottom value. Positive = up, negative = down. */
    private int hudOffsetY = 0;
    /** Distance in pixels from the right edge of the screen (SIDE_RIGHT mode only). */
    private int hudRightMargin = 10;
    /** "ANILLOS" (por defecto) o "BARRAS". */
    private String hudEstilo = "ANILLOS";
    /**
     * Delay in ms between PlayerReadyEvent and the single initial HUD send. Sending at ready
     * sometimes races the client's UI channel and the packet is lost (bars never appear until
     * relog). 0 = send immediately (old behavior).
     */
    private int hudInitDelayMs = 1500;
    /** Barrido periodico que recrea el HUD a quien deberia tenerlo y no lo tiene (Modo A). SEGURO:
     *  solo crea si falta, nunca reenvia. Apagar solo si molesta. */
    private boolean hudAutoRecoverEnabled = true;
    private int hudAutoRecoverIntervalMs = 10000;
    private float blockBreakHungerCost = 0.01f;     // Mining cost
    private float jumpHungerCost = 0.02f;           // Jump cost (with 500ms cooldown)

    // Thirst settings — drain sources now match hunger for parity
    private boolean thirstEnabled = true;
    private Integer maxThirst = 50;                 // Symmetric with hunger
    private float thirstDecayRate = 0.05f;          // 0.01/s walking → 83min full→0
    private float sprintThirstMultiplier = 1.5f;    // Sprint deshidrata moderado
    private float thirstyThreshold = 20.0f;         // "Thirsty" effect at 40% thirst
    private float blockBreakThirstCost = 0.005f;    // ½ of hunger (sprint already drains thirst)
    private float jumpThirstCost = 0.01f;           // ½ of hunger
    private boolean pauseWhileSleeping = true;

    // Well Fed settings — buffs activate at 60% full (achievable with normal play)
    private boolean wellFedEnabled = true;
    private float wellFedThreshold = 30.0f;         // 60% of MaxHunger / MaxThirst (50)

    // Idle Pause settings
    private boolean idlePauseEnabled = true;
    private float idlePauseSeconds = 60.0f;

    /**
     * LISTA NEGRA de mundos: el mod esta activo en TODOS salvo en los que aparezcan aqui.
     *
     * Es una lista negra y no una blanca a proposito: hay instancias (dungeons, arenas) cuyo
     * nombre cambia en cada creacion y en las que SI se quiere hambre/sed, asi que una lista
     * blanca habria que ampliarla cada vez que sale un nombre nuevo. Los mundos excluidos (el
     * lobby) tienen nombre fijo. Se compara contra World.getName().
     */
    private String[] disabledWorlds = { "Hub" };

    /**
     * Vista de {@link #disabledWorlds} para consultar sin distinguir mayusculas y sin asignar
     * memoria: se mira una vez por jugador y tick, asi que un toLowerCase() por consulta si se
     * notaria. TreeSet con CASE_INSENSITIVE_ORDER resuelve el contains() sin crear Strings.
     */
    private volatile java.util.Set<String> disabledWorldLookup = buildWorldLookup(new String[] { "Hub" });

    /** Prefijos de las entradas con comodin. Vacio = sin comodines (caso normal, coste cero). */
    private volatile String[] disabledWorldPrefixes = new String[0];

    public EasyHungerConfig() {
    }

    private static java.util.Set<String> buildWorldLookup(String[] names) {
        java.util.TreeSet<String> lookup = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) continue;
                String n = name.trim();
                if (n.endsWith("*")) continue; // comodin: lo gestiona buildWorldPrefixes
                lookup.add(n);
            }
        }
        return lookup;
    }

    /**
     * Entradas con comodin al final ("instance-Hauldron*") -> prefijos sin el asterisco.
     * Las instancias de mazmorra se generan con un sufijo aleatorio (instance-Hauldron-<uuid>),
     * asi que una coincidencia exacta nunca las pilla. Un "*" suelto se IGNORA a proposito:
     * como prefijo vacio desactivaria el mod en TODOS los mundos por un descuido al teclear.
     */
    private static String[] buildWorldPrefixes(String[] names) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (names != null) {
            for (String name : names) {
                if (name == null) continue;
                String n = name.trim();
                if (!n.endsWith("*")) continue;
                String prefix = n.substring(0, n.length() - 1).trim();
                if (!prefix.isEmpty()) out.add(prefix);
            }
        }
        return out.toArray(new String[0]);
    }

    public float getStarvationTickRate() {
        return starvationTickRate;
    }
    public float getStarvationPerTick() {
        return starvationPerTick;
    }
    public float getStarvationStaminaModifier() {
        return starvationStaminaModifier;
    }
    public float getHungryThreshold() {
        return hungryThreshold;
    }
    public float getStarvationDamage() {
        return starvationDamage;
    }
    public float getThirstDamage() { return thirstDamage; }

    public HudPosition getHudPosition() {
        return hudPosition;
    }

    /**
     * HudPosition del archivo. Antes era HudPosition.valueOf(v) a pelo: una errata en
     * HungerConfig.json lanzaba IllegalArgumentException, la carga de la config fallaba entera y
     * el plugin no arrancaba (sin hambre, sed ni HUD). Ahora un valor no valido deja BOTTOM (el
     * valor de fabrica), avisa con un WARNING y el resto de la config carga. Acepta mayusculas o
     * minusculas, igual que el panel de /eh values.
     */
    private static HudPosition parseHudPositionOrDefault(String v) {
        if (v != null) {
            try {
                return HudPosition.valueOf(v.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Cae al aviso de abajo.
            }
        }
        com.haas.easyhunger.EasyHunger.LOGGER.at(java.util.logging.Level.WARNING).log(
                "HungerConfig.json: HudPosition no valido (\"" + v + "\"); se usa BOTTOM. "
                + "Valores validos: TOP, BOTTOM, SIDE_RIGHT.");
        return HudPosition.BOTTOM;
    }
    public int getHudOffsetY()     { return hudOffsetY; }
    public int getHudRightMargin() { return hudRightMargin; }
    public String getHudEstilo() { return hudEstilo; }
    public boolean isHudBarras() { return "BARRAS".equals(hudEstilo); }
    /** Cualquier cosa que no sea BARRAS (o una errata) cae en ANILLOS: un estilo roto no deja sin HUD. */
    public static String parseHudEstilo(String v) {
        return v != null && v.trim().equalsIgnoreCase("BARRAS") ? "BARRAS" : "ANILLOS";
    }
    public int getHudInitDelayMs() { return hudInitDelayMs; }
    public boolean isHudAutoRecoverEnabled() { return hudAutoRecoverEnabled; }
    public int getHudAutoRecoverIntervalMs() { return hudAutoRecoverIntervalMs; }
    public float getBlockBreakHungerCost() {
        return blockBreakHungerCost;
    }
    public float getJumpHungerCost() {
        return jumpHungerCost;
    }
    
    public boolean isThirstEnabled() { return thirstEnabled; }
    public Integer getMaxThirst() { return maxThirst; }
    public float getThirstDecayRate() { return thirstDecayRate; }
    public float getSprintThirstMultiplier() { return sprintThirstMultiplier; }
    public float getThirstyThreshold() { return thirstyThreshold; }
    public float getBlockBreakThirstCost() { return blockBreakThirstCost; }
    public float getJumpThirstCost() { return jumpThirstCost; }
    public boolean isPauseWhileSleeping() { return pauseWhileSleeping; }
    
    // Well Fed getters
    public boolean isWellFedEnabled() { return wellFedEnabled; }
    public float getWellFedThreshold() { return wellFedThreshold; }

    // Idle Pause getters
    public boolean isIdlePauseEnabled() { return idlePauseEnabled; }
    public float getIdlePauseSeconds() { return idlePauseSeconds; }

    public String[] getDisabledWorlds() { return disabledWorlds; }

    /** true si el mod NO debe actuar en este mundo. Comparacion sin distinguir mayusculas. */
    public boolean isWorldDisabled(String worldName) {
        if (worldName == null) return false;
        java.util.Set<String> lookup = this.disabledWorldLookup;
        if (!lookup.isEmpty() && lookup.contains(worldName)) return true;
        // Comodines: regionMatches con ignoreCase no asigna memoria (se llama por jugador y tick).
        String[] prefixes = this.disabledWorldPrefixes;
        for (String p : prefixes) {
            if (worldName.regionMatches(true, 0, p, 0, p.length())) return true;
        }
        return false;
    }

    // ===================== SETTERS =====================

    // Validacion de rangos. Los setters solo los llama el panel de /eh values, y antes aceptaban
    // cualquier cosa: MaxHunger 0 dividia por cero en el HUD, MaxHunger 1 recortaba el hambre
    // guardada de todos a 1, y StarvationTickRate 0 drenaba y hacia dano en cada tick.
    // Fuera de rango se lanza IllegalArgumentException y el valor anterior se queda.
    // `!(a >= min)` en vez de `a < min` para rechazar tambien NaN.
    private static int requireAtLeast(String key, int value, int min) {
        if (value < min) {
            throw new ValorRechazado(key + " debe ser >= " + min + " (llegó " + value + ")",
                    "rango.minimo", "clave", key, "min", min, "valor", value);
        }
        return value;
    }

    private static float requireAtLeast(String key, float value, float min) {
        if (!(value >= min) || Float.isInfinite(value)) {
            throw new ValorRechazado(key + " debe ser >= " + min + " (llegó " + value + ")",
                    "rango.minimo", "clave", key, "min", min, "valor", value);
        }
        return value;
    }

    private static float requireBetween(String key, float value, float min, float max) {
        if (!(value >= min && value <= max)) {
            throw new ValorRechazado(key + " debe estar entre " + min + " y " + max
                    + " (llegó " + value + ")",
                    "rango.entre", "clave", key, "min", min, "max", max, "valor", value);
        }
        return value;
    }

    /**
     * Valor fuera de rango. El mensaje sigue en espanol (logs); el panel pinta {@link #clave} con
     * sus {@link #args} en el idioma de quien lo ve (lang/*.json).
     */
    public static final class ValorRechazado extends IllegalArgumentException {
        public final String clave;
        public final transient Object[] args;

        public ValorRechazado(String mensaje, String clave, Object... args) {
            super(mensaje);
            this.clave = clave;
            this.args = args;
        }
    }

    // Hunger setters
    public void setMaxHunger(int value) { this.maxHunger = requireAtLeast("MaxHunger", value, 1); }
    public void setStarvationTickRate(float value) { this.starvationTickRate = requireAtLeast("StarvationTickRate", value, 0.5f); }
    public void setStarvationPerTick(float value) { this.starvationPerTick = requireAtLeast("StarvationPerTick", value, 0f); }
    public void setStarvationStaminaModifier(float value) { this.starvationStaminaModifier = requireAtLeast("StarvationStaminaModifier", value, 0f); }
    public void setHungryThreshold(float value) { this.hungryThreshold = requireBetween("HungryThreshold", value, 0f, maxHunger); }
    public void setStarvationDamage(float value) { this.starvationDamage = requireAtLeast("StarvationDamage", value, 0f); }
    public void setBlockBreakHungerCost(float value) { this.blockBreakHungerCost = requireAtLeast("BlockBreakHungerCost", value, 0f); }
    public void setJumpHungerCost(float value) { this.jumpHungerCost = requireAtLeast("JumpHungerCost", value, 0f); }

    // Thirst setters
    public void setThirstEnabled(boolean value) { this.thirstEnabled = value; }
    public void setMaxThirst(int value) { this.maxThirst = requireAtLeast("MaxThirst", value, 1); }
    public void setThirstDecayRate(float value) { this.thirstDecayRate = requireAtLeast("ThirstDecayRate", value, 0f); }
    public void setSprintThirstMultiplier(float value) { this.sprintThirstMultiplier = requireAtLeast("SprintThirstMultiplier", value, 0f); }
    public void setThirstDamage(float value) { this.thirstDamage = requireAtLeast("ThirstDamage", value, 0f); }
    public void setThirstyThreshold(float value) { this.thirstyThreshold = requireBetween("ThirstyThreshold", value, 0f, maxThirst); }
    public void setBlockBreakThirstCost(float value) { this.blockBreakThirstCost = requireAtLeast("BlockBreakThirstCost", value, 0f); }
    public void setJumpThirstCost(float value) { this.jumpThirstCost = requireAtLeast("JumpThirstCost", value, 0f); }

    // Other setters
    public void setHudPosition(HudPosition value)   { this.hudPosition    = value; }
    public void setHudOffsetY(int value)             { this.hudOffsetY     = value; }
    public void setHudRightMargin(int value)         { this.hudRightMargin = value; }
    public void setHudEstilo(String value)           { this.hudEstilo = parseHudEstilo(value); }
    public void setHudInitDelayMs(int value)         { this.hudInitDelayMs = value; }
    public void setPauseWhileSleeping(boolean value) { this.pauseWhileSleeping = value; }
    public void setWellFedEnabled(boolean value) { this.wellFedEnabled = value; }
    // Se compara contra hambre Y sed (WellFedSystem), asi que el tope es el mayor de los dos maximos.
    public void setWellFedThreshold(float value) { this.wellFedThreshold = requireBetween("WellFedThreshold", value, 0f, Math.max(maxHunger, maxThirst)); }

    // Idle Pause setters
    public void setIdlePauseEnabled(boolean value) { this.idlePauseEnabled = value; }
    public void setIdlePauseSeconds(float value) { this.idlePauseSeconds = requireAtLeast("IdlePauseSeconds", value, 0f); }

    /**
     * Se copia el array en vez de guardar el que entrega el codec, por lo mismo que en 1.3.6 con
     * MapCodec: al RECARGAR de disco el codec puede entregar una estructura compartida/inmutable y
     * la bomba solo explota tras un reinicio. Copiando aqui, el array que guardamos siempre es
     * nuestro. Reconstruye ademas la vista de busqueda.
     */
    public void setDisabledWorlds(String[] value) {
        this.disabledWorlds = (value == null) ? new String[0] : value.clone();
        this.disabledWorldLookup = buildWorldLookup(this.disabledWorlds);
        this.disabledWorldPrefixes = buildWorldPrefixes(this.disabledWorlds);
    }

}



