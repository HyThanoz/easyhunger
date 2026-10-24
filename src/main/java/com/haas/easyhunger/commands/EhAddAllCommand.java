package com.haas.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.protocol.InteractionType;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.utils.ChatStyle;
import com.haas.easyhunger.utils.Msg;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public class EhAddAllCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.command.addall";

    // Rangos del valor que se asigna segun la rareza (ItemQuality) del item.
    private static final float COMMON_MIN = 5.0f, COMMON_MAX = 8.0f;
    private static final float UNCOMMON_MIN = 10.0f, UNCOMMON_MAX = 15.0f;
    private static final float RARE_MIN = 15.0f, RARE_MAX = 25.0f;
    private static final float EPIC_MIN = 25.0f, EPIC_MAX = 35.0f;
    private static final float LEGENDARY_VALUE = 45.0f;

    /** Valor al azar en [min, max), con la misma formula de siempre: min + r * (max - min). */
    private static float randomIn(java.util.concurrent.ThreadLocalRandom rand, float min, float max) {
        return min + rand.nextFloat() * (max - min);
    }

    /** "(valor: X)" con valor fijo; "(al azar por rareza: de A a B)" sin el, con lo que de verdad se aplico. */
    private static com.hypixel.hytale.server.core.Message appliedRange(UUID uuid, boolean fixedValue, float min, float max) {
        if (fixedValue || min == max) {
            return com.hypixel.hytale.server.core.Message.join(Msg.partes(uuid, "addall.rango_fijo", "valor", min));
        }
        return com.hypixel.hytale.server.core.Message.join(Msg.partes(uuid, "addall.rango_azar", "min", min, "max", max));
    }

    /** Linea del informe: los trozos de la clave y, al final, el rango aplicado (anidado, como antes). */
    private static com.hypixel.hytale.server.core.Message reportLine(com.hypixel.hytale.server.core.Message[] partes,
                                                                     com.hypixel.hytale.server.core.Message rango) {
        com.hypixel.hytale.server.core.Message[] linea = java.util.Arrays.copyOf(partes, partes.length + 1);
        linea[partes.length] = rango;
        return ChatStyle.plain(linea);
    }

    private final OptionalArg<Float> valueArg = this.withOptionalArg("value", "Fixed value for every new item (omit: random by rarity)", ArgTypes.FLOAT);

    public EhAddAllCommand() {
        super("addall", "Auto-detect and add all food/drink items", false);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Float valueOpt = this.valueArg.get(context);
        // Con valor: se usa ese valor fijo para todos los items nuevos. Sin valor: al azar segun la
        // rareza, como siempre. Antes el valor se aceptaba pero la tabla por rareza lo pisaba
        // casi siempre, y el informe decia que se habia aplicado.
        boolean fixedValue = valueOpt != null;
        UUID uuid = playerRef.getUuid();
        if (fixedValue && !(valueOpt > 0.0f && !valueOpt.isInfinite())) {
            context.sendMessage(Msg.error(uuid, "addall.valor_invalido", "valor", valueOpt));
            return;
        }
        float defaultValue = fixedValue ? valueOpt : 5.0f;
        // Rango real de lo aplicado, para el informe.
        float foodMin = Float.MAX_VALUE, foodMax = -Float.MAX_VALUE;
        float drinkMin = Float.MAX_VALUE, drinkMax = -Float.MAX_VALUE;

        context.sendMessage(Msg.info(uuid, "addall.revisando"));

        int foodCount = 0;
        int drinkCount = 0;
        int skippedCount = 0;
        int totalScanned = 0;

        try {
            Map<?, ?> allItems = Item.getAssetMap().getAssetMap();
            totalScanned = allItems.size();

            for (Map.Entry<?, ?> entry : allItems.entrySet()) {
                String itemId = (String) entry.getKey();
                Item item = (Item) entry.getValue();
                if (item == null || itemId == null) continue;

                // Clean up item ID - remove leading asterisk (Hytale state variant prefix)
                if (itemId.startsWith("*")) {
                    itemId = itemId.substring(1);
                }

                // Skip template items
                if (itemId.contains("Template")) continue;

                // Check interactions for food or drink
                boolean isFood = false;
                boolean isDrink = false;

                Map<InteractionType, String> interactions = item.getInteractions();
                if (interactions != null) {
                    for (String interactionId : interactions.values()) {
                        if (interactionId == null) continue;
                        String lower = interactionId.toLowerCase();
                        if (lower.contains("consume_food")) {
                            isFood = true;
                        }
                        if (lower.contains("consume_drink") || lower.contains("startdrinking")) {
                            isDrink = true;
                        }
                    }
                }

                // Skip items without food/drink interaction
                if (!isFood && !isDrink) continue;

                // Un jugo, un te o una leche pueden venir con la cadena de COMER (los jugos de
                // Hidden's Harvest Delights usan Consume_Food_T2), y por la interaccion sola irian a
                // Foods y subirian hambre en vez de sed. Si el NOMBRE dice que es bebida, gana el
                // nombre: en este metodo, isDrink manda a Drinks. Ver ConsumableKind.looksLikeDrink.
                if (isFood && !isDrink && com.haas.easyhunger.utils.ConsumableKind.looksLikeDrink(itemId)) {
                    isDrink = true;
                }

                // Calculate custom value based on Item Quality (Rarity)
                float calculatedValue = defaultValue;
                java.util.concurrent.ThreadLocalRandom rand = java.util.concurrent.ThreadLocalRandom.current();

                if (!fixedValue) try {
                    com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality quality = 
                        com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
                    
                    if (quality != null) {
                        String qId = quality.getId();
                        if (qId != null) {
                            String lowerQ = qId.toLowerCase();
                            
                            if (lowerQ.contains("uncommon")) {
                                calculatedValue = randomIn(rand, UNCOMMON_MIN, UNCOMMON_MAX);
                            } else if (lowerQ.contains("rare")) {
                                calculatedValue = randomIn(rand, RARE_MIN, RARE_MAX);
                            } else if (lowerQ.contains("epic")) {
                                calculatedValue = randomIn(rand, EPIC_MIN, EPIC_MAX);
                            } else if (lowerQ.contains("legendary") || lowerQ.contains("mythic")) {
                                calculatedValue = LEGENDARY_VALUE;
                            } else {
                                // Explicitly Common/Default (fallback)
                                calculatedValue = randomIn(rand, COMMON_MIN, COMMON_MAX);
                            }
                        }
                    } else {
                        // Without any Rarity
                        calculatedValue = randomIn(rand, COMMON_MIN, COMMON_MAX);
                    }
                } catch (Exception e) {
                    // Fallback to default if quality fetch fails
                    calculatedValue = randomIn(rand, COMMON_MIN, COMMON_MAX);
                }
                
                // Arredonda para 1 casa decimal (solo el aleatorio: el valor fijo se usa tal cual)
                if (!fixedValue) calculatedValue = Math.round(calculatedValue * 10.0f) / 10.0f;

                // Add to config, skip if already exists in EITHER config
                boolean alreadyInFood = EasyHunger.get().getFoodsConfig().getFoodValue(itemId) > 0;
                boolean alreadyInDrink = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId) > 0;

                if (alreadyInFood || alreadyInDrink) {
                    skippedCount++;
                    continue;
                }

                if (isDrink) {
                    EasyHunger.get().getDrinksConfig().setDrinkValue(itemId, calculatedValue);
                    drinkCount++;
                    drinkMin = Math.min(drinkMin, calculatedValue);
                    drinkMax = Math.max(drinkMax, calculatedValue);
                    EasyHunger.logInfo("[AddAll] Added drink: " + itemId + " (value: " + calculatedValue + ")");
                } else {
                    EasyHunger.get().getFoodsConfig().setFoodValue(itemId, calculatedValue);
                    foodCount++;
                    foodMin = Math.min(foodMin, calculatedValue);
                    foodMax = Math.max(foodMax, calculatedValue);
                    EasyHunger.logInfo("[AddAll] Added food: " + itemId + " (value: " + calculatedValue + ")");
                }
            }

            // Save configs
            if (foodCount > 0) EasyHunger.get().saveFoodsConfig();
            if (drinkCount > 0) EasyHunger.get().saveDrinksConfig();

            // Report
            context.sendMessage(Msg.info(uuid, "addall.terminada", "total", totalScanned));
            if (foodCount > 0) context.sendMessage(reportLine(Msg.partes(uuid, "addall.comidas", "cantidad", foodCount),
                    appliedRange(uuid, fixedValue, foodMin, foodMax)));
            if (drinkCount > 0) context.sendMessage(reportLine(Msg.partes(uuid, "addall.bebidas", "cantidad", drinkCount),
                    appliedRange(uuid, fixedValue, drinkMin, drinkMax)));
            if (skippedCount > 0) context.sendMessage(Msg.suelta(uuid, "addall.omitidos", "cantidad", skippedCount));
            if (foodCount == 0 && drinkCount == 0 && skippedCount == 0) {
                context.sendMessage(Msg.suelta(uuid, "addall.nada"));
            }

        } catch (Exception e) {
            context.sendMessage(Msg.error(uuid, "addall.error", "error", e.getMessage()));
            EasyHunger.logInfo("EhAddAllCommand error: " + e.toString());
        }
    }
}
