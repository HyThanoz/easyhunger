package com.haas.easyhunger.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for biome-based hunger and thirst modifiers.
 * Allows customizing how different biomes affect hunger/thirst drain rates.
 */
public class BiomeModifiersConfig {
    
    // Codec definitions
    private static final KeyedCodec<Boolean> ENABLED = new KeyedCodec<>("Enabled", Codec.BOOLEAN);
    private static final KeyedCodec<Float> DEFAULT_HUNGER_MULTIPLIER = new KeyedCodec<>("DefaultHungerMultiplier", Codec.FLOAT);
    private static final KeyedCodec<Float> DEFAULT_THIRST_MULTIPLIER = new KeyedCodec<>("DefaultThirstMultiplier", Codec.FLOAT);
    // LinkedHashMap y no HashMap: MapCodec mete las claves en el mapa que le da el supplier en el
    // orden del archivo (MapCodec.decode y decodeJson en 0.6.8), asi que con un
    // LinkedHashMap ese orden se conserva. Con HashMap se perdia.
    private static final KeyedCodec<Map<String, Float>> HUNGER_MODIFIERS = new KeyedCodec<>("HungerModifiers", new MapCodec<>(Codec.FLOAT, java.util.LinkedHashMap::new));
    private static final KeyedCodec<Map<String, Float>> THIRST_MODIFIERS = new KeyedCodec<>("ThirstModifiers", new MapCodec<>(Codec.FLOAT, java.util.LinkedHashMap::new));

    /*
     * PRIORIDAD de las coincidencias parciales (ver resolveMultiplier): gana la PRIMERA clave de
     * esta lista que aparezca dentro del nombre del bioma.
     *
     * Es EXACTAMENTE el orden en que el HashMap recorria las claves por defecto hasta la 1.5.14,
     * congelado a mano para que el juego no cambie: con los 232 biomas de 0.6.8 el multiplicador
     * sale identico al de antes, con los valores por defecto y con el archivo cargado. Antes ese
     * orden dependia del hash de las claves (y del orden en que se metian), y meter una clave
     * nueva podia cambiar el resultado de otros biomas. Ahora las claves que no estan en esta
     * lista (las que agregue un admin en BiomeModifiers.json) van DESPUES, en el orden del
     * archivo: solo deciden en los biomas donde no coincidia ninguna de estas.
     *
     * Para cambiar el multiplicador de un bioma concreto, lo seguro es agregar su nombre exacto:
     * la coincidencia exacta se mira antes que todo esto.
     */
    private static final java.util.List<String> HUNGER_PRIORITY = java.util.List.of(
            "Glacier", "Cold", "Savannah", "Lava", "Plains", "Lake", "Frozen", "Scrub", "Volcano",
            "Forest", "River", "Desert", "Wastes", "Tundra");
    private static final java.util.List<String> THIRST_PRIORITY = java.util.List.of(
            "Burned", "Swamp", "Lake", "Scrub", "Reef", "Mountain", "Ash", "River", "Ocean", "Kelp",
            "Plateau", "Trench", "Lava", "Savannah", "Glacier", "Cold", "Hotsprings", "Plains", "Oasis",
            "Frozen", "Island", "Volcano", "Caldera", "Canyon", "Forest", "Desert", "Wastes", "Tundra",
            "Dunes");

    /** Copia de {@code src} con las claves de {@code priority} primero (en ese orden) y el resto detras, en el orden de {@code src}. */
    private static Map<String, Float> inPriorityOrder(Map<String, Float> src, java.util.List<String> priority) {
        Map<String, Float> out = new java.util.LinkedHashMap<>();
        for (String key : priority) {
            if (src.containsKey(key)) out.put(key, src.get(key));
        }
        for (Map.Entry<String, Float> e : src.entrySet()) {
            if (!out.containsKey(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    // MapCodec entrega un mapa inmutable al cargar: se copia (en orden de prioridad) para que put
    // no lance UnsupportedOperationException. Misma regla que PlayerPrefsConfig.
    public static final BuilderCodec<BiomeModifiersConfig> CODEC = BuilderCodec.builder(BiomeModifiersConfig.class, BiomeModifiersConfig::new)
            .append(ENABLED, (c, v) -> c.enabled = v, BiomeModifiersConfig::isEnabled).add()
            .append(DEFAULT_HUNGER_MULTIPLIER, (c, v) -> c.defaultHungerMultiplier = v, BiomeModifiersConfig::getDefaultHungerMultiplier).add()
            .append(DEFAULT_THIRST_MULTIPLIER, (c, v) -> c.defaultThirstMultiplier = v, BiomeModifiersConfig::getDefaultThirstMultiplier).add()
            .append(HUNGER_MODIFIERS, (c, v) -> c.hungerModifiers = (v == null) ? null : inPriorityOrder(v, HUNGER_PRIORITY), BiomeModifiersConfig::getHungerModifiers).add()
            .append(THIRST_MODIFIERS, (c, v) -> c.thirstModifiers = (v == null) ? null : inPriorityOrder(v, THIRST_PRIORITY), BiomeModifiersConfig::getThirstModifiers).add()
            .build();
    
    // Fields with defaults
    private boolean enabled = true;
    private float defaultHungerMultiplier = 1.0f;
    private float defaultThirstMultiplier = 1.0f;
    private Map<String, Float> hungerModifiers;
    private Map<String, Float> thirstModifiers;

    // Memoized biomeName -> resolved multiplier. The partial-match scan below allocates
    // (toLowerCase) and is O(n) per lookup; modifiers are immutable after load, so each
    // biome name only needs resolving once.
    private final Map<String, Float> resolvedHungerCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Float> resolvedThirstCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    public BiomeModifiersConfig() {
        // Default hunger modifiers - Keywords match partial biome names
        // Cold = more hunger, Hot = normal hunger
        hungerModifiers = new HashMap<>();
        // Cold biomes (more hunger to stay warm)
        hungerModifiers.put("Frozen", 1.5f);       // Valley_Forest_Frozen, Lake_Forest_Frozen, etc
        hungerModifiers.put("Glacier", 1.6f);      // Glacier, Valley_Glacier, Lake_Glacier
        hungerModifiers.put("Tundra", 1.4f);       // Valley_Forest_Tundra, Cliffs_Tundra
        hungerModifiers.put("Cold", 1.3f);         // Cold_Kelp, Cold_Reef, Cold_Trench, Island_Cold
        // Normal biomes
        hungerModifiers.put("Forest", 1.0f);       // All forest variants
        hungerModifiers.put("Plains", 1.0f);       // All plains variants
        hungerModifiers.put("River", 1.0f);        // All river variants
        hungerModifiers.put("Lake", 1.0f);         // All lake variants
        hungerModifiers.put("Desert", 1.0f);       // All desert variants
        hungerModifiers.put("Savannah", 1.0f);     // Plateau_Savannah_*
        hungerModifiers.put("Scrub", 1.0f);        // Scrub_Tar_Pits
        // Hot biomes (less hunger needed)
        hungerModifiers.put("Lava", 0.9f);         // *_Lava, Volcano_*
        hungerModifiers.put("Volcano", 0.9f);      // Volcano_Wastes_Lava
        hungerModifiers.put("Wastes", 1.0f);       // All wastes variants
        
        // Default thirst modifiers - Keywords match partial biome names
        // Hot = more thirst, Cold = less thirst, Water nearby = less thirst
        thirstModifiers = new HashMap<>();
        // Hot/Dry biomes (more thirst)
        thirstModifiers.put("Desert", 2.0f);       // Desert_*, Dunes_Desert_*, Plateau_Desert_*
        thirstModifiers.put("Dunes", 2.0f);        // Dunes_Desert_*
        thirstModifiers.put("Lava", 2.0f);         // *_Lava variants (extremely hot)
        thirstModifiers.put("Volcano", 1.8f);      // Volcano_Wastes_Lava
        thirstModifiers.put("Wastes", 1.5f);       // Canyon_Wastes_*, Mountain_Wastes_*
        thirstModifiers.put("Caldera", 1.8f);      // Caldera_Forest_Ghost, Caldera_Wastes_*
        thirstModifiers.put("Savannah", 1.4f);     // Plateau_Savannah_* (dry grasslands)
        thirstModifiers.put("Scrub", 1.3f);        // Scrub_Tar_Pits (arid)
        thirstModifiers.put("Ash", 1.4f);          // *_Ash variants (dry volcanic)
        thirstModifiers.put("Burned", 1.3f);       // Canyon_Forest_Burned (dry)
        // Normal biomes
        thirstModifiers.put("Forest", 1.0f);       // All forest variants
        thirstModifiers.put("Plains", 1.0f);       // All plains variants
        thirstModifiers.put("Mountain", 1.1f);     // Mountain_* (drier at altitude)
        thirstModifiers.put("Canyon", 1.2f);       // Canyon_* (dry canyons)
        thirstModifiers.put("Plateau", 1.1f);      // Plateau_* (except desert)
        // Cold/Wet biomes (less thirst)
        thirstModifiers.put("Frozen", 0.7f);       // Cold = less thirst
        thirstModifiers.put("Glacier", 0.6f);      // Ice = less thirst
        thirstModifiers.put("Tundra", 0.8f);       // Cold tundra
        thirstModifiers.put("Cold", 0.8f);         // Cold_* ocean variants
        thirstModifiers.put("Swamp", 0.7f);        // Canyon_Forest_Swamp, Lake_Swamp (wet)
        // Water biomes (much less thirst)
        thirstModifiers.put("River", 0.6f);        // River_* (near fresh water)
        thirstModifiers.put("Lake", 0.6f);         // Lake_* (near fresh water)
        thirstModifiers.put("Ocean", 0.8f);        // Ocean biomes (salty but cool)
        thirstModifiers.put("Kelp", 0.8f);         // *_Kelp (underwater)
        thirstModifiers.put("Reef", 0.8f);         // *_Reef (underwater)
        thirstModifiers.put("Trench", 0.8f);       // *_Trench (deep water)
        thirstModifiers.put("Island", 0.9f);       // Island_* (near water)
        thirstModifiers.put("Oasis", 0.5f);        // *_Oasis (water in desert!)
        thirstModifiers.put("Hotsprings", 0.7f);   // Desert_Hotsprings, *_Hotsprings (water)

        // El orden de los put de arriba no decide nada: la prioridad es HUNGER_PRIORITY /
        // THIRST_PRIORITY, y aqui queda un LinkedHashMap en ese orden.
        hungerModifiers = inPriorityOrder(hungerModifiers, HUNGER_PRIORITY);
        thirstModifiers = inPriorityOrder(thirstModifiers, THIRST_PRIORITY);
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public float getDefaultHungerMultiplier() { return defaultHungerMultiplier; }
    public float getDefaultThirstMultiplier() { return defaultThirstMultiplier; }
    public Map<String, Float> getHungerModifiers() { return hungerModifiers; }
    public Map<String, Float> getThirstModifiers() { return thirstModifiers; }
    
    /**
     * Get hunger multiplier for a specific biome.
     * @param biomeName Name of the biome
     * @return Multiplier (1.0 = normal, 2.0 = double drain, 0.5 = half drain)
     */
    public float getHungerMultiplier(String biomeName) {
        if (!enabled || biomeName == null) return 1.0f;
        return resolvedHungerCache.computeIfAbsent(biomeName,
                name -> resolveMultiplier(name, hungerModifiers, defaultHungerMultiplier));
    }
    
    /**
     * Get thirst multiplier for a specific biome.
     * @param biomeName Name of the biome
     * @return Multiplier (1.0 = normal, 2.0 = double drain, 0.5 = half drain)
     */
    public float getThirstMultiplier(String biomeName) {
        if (!enabled || biomeName == null) return 1.0f;
        return resolvedThirstCache.computeIfAbsent(biomeName,
                name -> resolveMultiplier(name, thirstModifiers, defaultThirstMultiplier));
    }

    private static float resolveMultiplier(String biomeName, Map<String, Float> modifiers, float defaultValue) {
        // Check for exact match first
        Float modifier = modifiers.get(biomeName);
        if (modifier != null) return modifier;

        // Check for partial match (e.g., "Snow_Forest" contains "Snow"). Gana la primera en el
        // orden del mapa, que es el de HUNGER_PRIORITY / THIRST_PRIORITY (ver arriba).
        String lower = biomeName.toLowerCase();
        for (Map.Entry<String, Float> entry : modifiers.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return defaultValue;
    }
}
