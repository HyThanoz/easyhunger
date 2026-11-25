package com.haas.easyhunger.config;


import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration file for drink thirst values.
 * Stored in: config/Drinks.json
 */
public class DrinksConfig {
    
    private static final KeyedCodec<Map<String, Float>> DRINK_VALUES = new KeyedCodec<>("DrinkValues", new MapCodec<>(Codec.FLOAT, LinkedHashMap::new));
    
    // MapCodec entrega un mapa inmutable al cargar: se copia (en orden) para que put no lance
    // UnsupportedOperationException. Misma regla que PlayerPrefsConfig.
    // null (JSON "DrinkValues": null) keeps the defaults instead of leaving a null map (1.5.31).
    public static final BuilderCodec<DrinksConfig> CODEC = BuilderCodec.builder(DrinksConfig.class, DrinksConfig::new)
            .append(DRINK_VALUES, (c, v) -> { if (v != null) c.drinkValues = publicar(new LinkedHashMap<>(v)); }, DrinksConfig::getDrinkValues).add()
            .build();

    /**
     * 1.5.31 (trap D4): copy-on-write, same as FoodsConfig. The published map is never modified;
     * writes copy it and publish the copy through this volatile field.
     */
    private volatile Map<String, Float> drinkValues;

    public DrinksConfig() {
        drinkValues = new LinkedHashMap<>();
        initializeDefaults();
        drinkValues = publicar(drinkValues);
    }

    private static Map<String, Float> publicar(Map<String, Float> nuevo) {
        return java.util.Collections.unmodifiableMap(nuevo);
    }
    
    private void initializeDefaults() {
        // EasyHunger containers (prefix matching)
        drinkValues.put("EasyHunger_Odre", 15.0f);
        drinkValues.put("EasyHunger_WaterBowl", 5.0f);
        
        // Vanilla/Other containers
        drinkValues.put("Container_Bucket_State_Filled_Water", 10.0f);
        drinkValues.put("Deco_Mug_State_Filled_Water", 5.0f);
        
        // NoCube Orchard - Fruit Juices
        drinkValues.put("NoCube_Drink_Fruit_Juice_Apple", 16.0f);
        drinkValues.put("NoCube_Drink_Fruit_Juice_Azure", 17.0f);
        drinkValues.put("NoCube_Drink_Fruit_Juice_Lemon", 15.0f);
        drinkValues.put("NoCube_Drink_Fruit_Juice_Mandarin", 18.0f);
        drinkValues.put("NoCube_Drink_Fruit_Juice_Multifruit", 20.0f);
        drinkValues.put("NoCube_Drink_Fruit_Juice_Persimmon", 16.0f);
        drinkValues.put("NoCube_Drink_Fruit_Juice_Pomegranate", 19.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Apricot", 17.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Berries_Red", 15.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Mango", 18.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Orange", 16.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Peach", 17.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Pear", 15.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Pinkberry", 18.0f);
        drinkValues.put("NoCube_Drink_Juice_Fruit_Plum", 19.0f);
        
        // NoCube Tavern - Alcoholic Drinks
        drinkValues.put("NoCube_Drink_Aged_Wine", 20.0f);
        drinkValues.put("NoCube_Drink_Ale", 16.0f);
        drinkValues.put("NoCube_Drink_Apple_Cider", 17.0f);
        drinkValues.put("NoCube_Drink_Beer", 15.0f);
        drinkValues.put("NoCube_Drink_Chicha", 18.0f);
        drinkValues.put("NoCube_Drink_Kvass", 16.0f);
        drinkValues.put("NoCube_Drink_Pale_Ale", 17.0f);
        drinkValues.put("NoCube_Drink_Peasant_Beer", 15.0f);
        drinkValues.put("NoCube_Drink_Rough_Wine", 16.0f);
        drinkValues.put("NoCube_Drink_Sake", 19.0f);
        drinkValues.put("NoCube_Drink_Stout", 18.0f);
        drinkValues.put("NoCube_Drink_Wine", 18.0f);
        
        // AleAndHearth - Brewery Drinks
        drinkValues.put("Brewery_Ale_Mug", 16.0f);
        drinkValues.put("Brewery_Beer_Mug", 15.0f);
        drinkValues.put("Brewery_Berry_Cider_Mug", 18.0f);
        drinkValues.put("Brewery_Caramel_Beer_Mug", 19.0f);
        drinkValues.put("Brewery_Chili_Beer_Mug", 17.0f);
        drinkValues.put("Brewery_Cider_Mug", 16.0f);
        drinkValues.put("Brewery_Kvas_Mug", 15.0f);
        drinkValues.put("Brewery_Pumpkin_Beer_Mug", 18.0f);
        drinkValues.put("Brewery_Vodka_Mug", 20.0f);
        drinkValues.put("Brewery_Wine_Mug", 19.0f);
        
        // Food Galore - Drinks
        drinkValues.put("CoffeeCup", 17.0f);
        drinkValues.put("MilkBox", 16.0f);
        drinkValues.put("SpeedCup", 18.0f);
        
        // HiddensHarvestDelights - Drinks
        drinkValues.put("Mango_Juice", 22.0f);
        
        // Connor's More Food Stuff - Drinks
        drinkValues.put("Food_Drink_Apple", 22.0f);
        drinkValues.put("Food_Drink_Berry", 21.0f);
        drinkValues.put("Food_Drink_Berry_Shake", 23.0f);
        drinkValues.put("Food_Drink_Coconut_Milk", 22.0f);
        drinkValues.put("Food_Drink_Mango", 21.0f);
        drinkValues.put("Food_Drink_Milk", 20.0f);
        
        // SNIP3_FoodPack - Juices (Drinks)
        // Rare (25-30)
        drinkValues.put("Food_Juice_Azure", 27.0f);
        drinkValues.put("Food_Juice_Pinkberry", 26.0f);
        drinkValues.put("Food_Juice_PoisonTree", 26.0f);
        drinkValues.put("Food_Juice_SpiralTree", 25.0f);
        drinkValues.put("Food_Juice_WindWillow", 25.0f);
        // Uncommon (20-24)
        drinkValues.put("Food_Juice_Apple", 22.0f);
        drinkValues.put("Food_Juice_Berry", 21.0f);
        drinkValues.put("Food_Juice_Carrot", 21.0f);
        drinkValues.put("Food_Juice_Mango", 20.0f);
        
        // AndiemgCheff - Drinks
        drinkValues.put("AndiemgCheff_Food_Sake", 35.0f);
        drinkValues.put("AndiemgCheff_Food_BubbleTea", 22.0f);
        
        // Caba's Artisan Harvest - Wines
        drinkValues.put("Azure_Fruit_Wine", 35.0f);
        drinkValues.put("Pink_Berry_Wine", 32.0f);
        drinkValues.put("Wild_Berry_Wine", 30.0f);
        drinkValues.put("Wine_Mango", 40.0f);
        
        // ZC Gaia - Juices & Drinks
        // Rare (25-30)
        drinkValues.put("ZC_AppleJuice", 17.0f);
        drinkValues.put("ZC_AzureJuice", 17.0f);
        drinkValues.put("ZC_BeetJuice", 17.0f);
        // Uncommon (20-24)
        drinkValues.put("ZC_BerryJuice", 17.0f);
        drinkValues.put("ZC_CocoMilk", 17.0f);
        
        // Jams (Moved from FoodsConfig)
        drinkValues.put("ZC_MangoHabJam", 17.0f);
        drinkValues.put("ZC_AzureJam", 17.0f);
        drinkValues.put("ZC_BerryJam", 17.0f);
        
        // Other Mods Jams
        drinkValues.put("Food_Jam_Azure", 17.0f);
        drinkValues.put("Food_Jam_PinkBerry", 17.0f);
        drinkValues.put("Food_Jam_PoisonTree", 17.0f);
        drinkValues.put("Food_Jam_SpiralTree", 17.0f);
        drinkValues.put("Food_Jam_WindWillow", 17.0f);
        drinkValues.put("Food_Jam_Mango", 17.0f);
        drinkValues.put("Food_Jam_Berry", 17.0f);
        drinkValues.put("Food_Jam_Apple", 17.0f);
        
        // Hatsuya Exploration Beers
        drinkValues.put("Hat_Beer_Glow", 18.0f);
        drinkValues.put("Hat_Beer_Health", 20.0f);
        drinkValues.put("Hat_Beer_Recall", 19.0f);
        drinkValues.put("Hat_Beer_Return", 19.0f);
        drinkValues.put("Hat_Beer_Surface", 17.0f);
    }
    
    public Map<String, Float> getDrinkValues() { return drinkValues; }
    
    public Float getDrinkValue(String drinkId) {
        Map<String, Float> mapa = drinkValues; // one snapshot for the whole lookup
        // First try exact match
        Float value = mapa.get(drinkId);
        if (value != null && value > 0) {
            return value;
        }

        // Then try partial match (config key is prefix of actual ID)
        for (Map.Entry<String, Float> entry : mapa.entrySet()) {
            if (drinkId != null && drinkId.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return 0.0f;
    }

    /**
     * Like {@link #getDrinkValue} but tells "not configured" (null) apart from "configured to 0".
     * Used where 0 must mean "this drink restores nothing" instead of "use the default" (1.5.30).
     */
    public Float findDrinkValue(String drinkId) {
        if (drinkId == null) return null;
        Map<String, Float> mapa = drinkValues; // one snapshot for the whole lookup
        Float value = mapa.get(drinkId);
        if (value != null) return value;
        for (Map.Entry<String, Float> entry : mapa.entrySet()) {
            if (drinkId.startsWith(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    /** @return false (and nothing is stored) if the value is NaN, infinite or negative. */
    public synchronized boolean setDrinkValue(String drinkId, float value) {
        if (!Float.isFinite(value) || value < 0.0f) return false;
        Map<String, Float> copia = new LinkedHashMap<>(drinkValues);
        copia.put(drinkId, value);
        drinkValues = publicar(copia);
        return true;
    }

    public synchronized boolean mergeDefaults() {
        DrinksConfig defaultConfig = new DrinksConfig();
        Map<String, Float> defaults = defaultConfig.getDrinkValues();
        
        // Create mutable copy if current map is unmodifiable
        Map<String, Float> mutableValues = new LinkedHashMap<>(drinkValues);
        
        boolean changed = false;
        
        // Legacy Config Cleanup: Forcefully remove old Keys that were replaced by Hytale Action-States.
        // This ensures old users don't see duplicated or broken entries in UIs when updating the Mod.
        if (mutableValues.containsKey("Deco_Mug")) {
            mutableValues.remove("Deco_Mug");
            changed = true;
        }
        if (mutableValues.containsKey("Container_Bucket")) {
            mutableValues.remove("Container_Bucket");
            changed = true;
        }

        for (Map.Entry<String, Float> entry : defaults.entrySet()) {
            if (!mutableValues.containsKey(entry.getKey())) {
                mutableValues.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        
        if (changed) {
            drinkValues = publicar(mutableValues);
        }
        return changed;
    }
}
