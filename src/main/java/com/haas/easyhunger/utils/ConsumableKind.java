package com.haas.easyhunger.utils;

import com.haas.easyhunger.config.DrinksConfig;
import com.haas.easyhunger.config.FoodsConfig;

/**
 * Decide si un item que llega por la cadena de BEBER es en realidad comida.
 *
 * Por que hace falta: la cadena nativa de beber (Root_Secondary_Consume_Drink) es la unica que
 * gasta durabilidad en vez de la cantidad, y algunos packs la usan para cosas que NO son bebida
 * (los platos de AleAndHearth: durabilidad 1 y al comerlos dejan el plato vacio). EasyHunger
 * sobreescribe esa cadena y termina en EasyHunger_DrinkWater, que antes trataba TODO lo que pasaba
 * por ahi como bebida y daba 15 de sed por defecto: el plato subia agua y no la barra de comida.
 *
 * Regla (la misma que usa el resto del mod, sin inventar prioridades nuevas):
 *  - esta en Drinks (> 0)            -> bebida, como siempre.
 *  - NO esta en Drinks y SI en Foods -> comida.
 *  - en ninguno de los dos           -> sigue siendo bebida con el valor por defecto, para no
 *                                       romper bebidas que nadie ha dado de alta.
 *
 * Recibe los config por parametro y no los lee del singleton del plugin: asi se puede probar
 * sin arrancar el servidor.
 */
public final class ConsumableKind {

    private ConsumableKind() {
    }

    /** Quita el asterisco de variante de estado que Hytale antepone a algunos ids. */
    public static String normalize(String itemId) {
        if (itemId == null) return null;
        return itemId.startsWith("*") ? itemId.substring(1) : itemId;
    }

    /** Id sin el sufijo de estado (":Filled_Water"). */
    private static String baseId(String itemId) {
        int i = itemId.indexOf(':');
        return i >= 0 ? itemId.substring(0, i) : itemId;
    }

    /** Valor como bebida: exacto/prefijo con el id completo y, si no, sin el sufijo de estado. */
    public static float drinkValue(DrinksConfig drinks, String itemId) {
        String id = normalize(itemId);
        if (id == null) return 0f;
        Float v = drinks.getDrinkValue(id);
        if ((v == null || v <= 0) && id.contains(":")) v = drinks.getDrinkValue(baseId(id));
        return v == null ? 0f : v;
    }

    /** Valor como comida (busqueda exacta, igual que ConsumeFoodInteraction). */
    public static float foodValue(FoodsConfig foods, String itemId) {
        String id = normalize(itemId);
        if (id == null) return 0f;
        Float v = foods.getFoodValue(id);
        if ((v == null || v <= 0) && id.contains(":")) v = foods.getFoodValue(baseId(id));
        return v == null ? 0f : v;
    }

    /**
     * Palabras que delatan una bebida cuando aparecen en CUALQUIER sitio del id
     * (Mango_Juice, Food_Juice_Berry, NoCube_Drink_Fruit_Juice_Apple, Lemonade...).
     */
    private static final java.util.Set<String> DRINK_ANYWHERE = java.util.Set.of(
            "juice", "drink", "drinks", "lemonade", "smoothie", "milkshake", "cider");

    /**
     * Palabras que delatan una bebida SOLO como ultima palabra del id (Fruit_Tea, Goat_Milk,
     * Red_Wine). Como primera o intermedia suelen ser un ingrediente de comida: Tea_Cake,
     * Milk_Chocolate, Coffee_Cake, Wine_Sauce. Por eso no cuentan en cualquier posicion.
     */
    private static final java.util.Set<String> DRINK_AS_LAST = java.util.Set.of(
            "milk", "tea", "soda", "wine", "beer", "ale", "coffee", "cocoa", "tonic", "mead", "brew");

    /**
     * Si aparece cualquiera de estas palabras, NO es bebida aunque lleve otra que lo parezca.
     * Salio de probarlo con el Foods.json real: Ingredient_Fresh_Milk, Ingredient_Soy_Milk y
     * SOL_Cookies_Milk (galletas con leche) se habrian mandado a bebidas.
     */
    private static final java.util.Set<String> FOOD_VETO = java.util.Set.of(
            "ingredient", "cookie", "cookies", "cake", "bread", "toast", "soup", "stew", "sauce",
            "pie", "cheese", "butter", "pudding", "cereal");

    /** Separa por '_', por '-' y por cambio de minuscula a mayuscula (LemonJuice -> lemon, juice). */
    private static java.util.List<String> words(String itemId) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (String chunk : normalize(itemId).split("[_\\-:\\s]+")) {
            if (chunk.isEmpty()) continue;
            for (String w : chunk.split("(?<=[a-z0-9])(?=[A-Z])")) {
                if (!w.isEmpty()) out.add(w.toLowerCase());
            }
        }
        return out;
    }

    /**
     * true si el NOMBRE del item dice que es una bebida. Lo usa /eh addall, que hasta ahora solo
     * miraba el nombre de la interaccion y mandaba a Foods todo lo que usara la cadena de comer,
     * aunque fuera un jugo (los jugos de Hidden's Harvest Delights definen Consume_Food).
     *
     * Compara por PALABRA completa y no por subcadena: un filtro por texto suelto marca como
     * bebida Steak ("tea") y Salt_Water_Taffy ("ale", "water"); esos falsos positivos se
     * comprobaron al disenarlo. Tampoco incluye "water": Salt_Water_Taffy es un caramelo.
     */
    public static boolean looksLikeDrink(String itemId) {
        if (itemId == null) return false;
        java.util.List<String> ws = words(itemId);
        if (ws.isEmpty()) return false;
        for (String w : ws) {
            if (FOOD_VETO.contains(w)) return false;
        }
        for (String w : ws) {
            if (DRINK_ANYWHERE.contains(w)) return true;
        }
        return DRINK_AS_LAST.contains(ws.get(ws.size() - 1));
    }

    /** true si el item es SOLO comida (ni en Drinks ni por prefijo, y si en Foods). */
    public static boolean isFoodOnly(DrinksConfig drinks, FoodsConfig foods, String itemId) {
        if (itemId == null) return false;
        if (drinkValue(drinks, itemId) > 0f) return false;
        return foodValue(foods, itemId) > 0f;
    }
}
