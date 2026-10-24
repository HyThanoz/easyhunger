package com.haas.easyhunger.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.haas.easyhunger.utils.Lang;
import com.haas.easyhunger.utils.Msg;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EasyHungerValuesPage - Interactive UI for viewing and editing food/drink values.
 * Features tabs, search, category filters, and display names.
 */
public class EasyHungerValuesPage extends InteractiveCustomUIPage<EasyHungerValuesPage.ConfigEventData> {

    private String activeTab = "foods";
    private String searchQuery = "";
    private String category = "all"; // all, vanilla, nocube, ale, cheff, other
    private final List<String> itemIds = new ArrayList<>();
    /** Ultimo valor mostrado o aplicado por clave de la pestana Config (ver handleSetConfig). */
    private final Map<String, String> shownConfigValues = new HashMap<>();

    public static class ConfigEventData {
        public String action;
        public String index;
        public String value;
        public String newItemId;
        public String newValue;
        public String searchQuery;
        public String configKey;  // For config tab

        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec
                .builder(ConfigEventData.class, ConfigEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Index", Codec.STRING), (o, v) -> o.index = v, o -> o.index).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING), (o, v) -> o.value = v, o -> o.value).add()
                .append(new KeyedCodec<>("@NewItemId", Codec.STRING), (o, v) -> o.newItemId = v, o -> o.newItemId).add()
                .append(new KeyedCodec<>("@NewValue", Codec.STRING), (o, v) -> o.newValue = v, o -> o.newValue).add()
                .append(new KeyedCodec<>("@SearchQuery", Codec.STRING), (o, v) -> o.searchQuery = v, o -> o.searchQuery).add()
                .append(new KeyedCodec<>("ConfigKey", Codec.STRING), (o, v) -> o.configKey = v, o -> o.configKey).add()
                .build();
    }

    public EasyHungerValuesPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigEventData.CODEC);
    }

    public EasyHungerValuesPage(@Nonnull PlayerRef playerRef, String tab, String search, String cat) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigEventData.CODEC);
        this.activeTab = tab;
        this.searchQuery = search != null ? search : "";
        this.category = cat != null ? cat : "all";
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        
        cmd.append("Pages/EasyHungerValuesUI.ui");

        String language = playerRef.getLanguage();

        // Textos fijos del .ui en el idioma del jugador (Lang), sobre los mismos id ya declarados
        // (TextButton y Label). El texto del .ui queda de respaldo; en espanol sale el mismo.
        UUID uuid = playerRef.getUuid();
        cmd.set("#TabFoods.Text", Lang.t(uuid, "panel.tab_comidas"));
        cmd.set("#TabDrinks.Text", Lang.t(uuid, "panel.tab_bebidas"));
        cmd.set("#TabConfig.Text", Lang.t(uuid, "panel.tab_config"));
        cmd.set("#CatAll.Text", Lang.t(uuid, "panel.cat_todo"));
        cmd.set("#CatOther.Text", Lang.t(uuid, "panel.cat_otros"));
        cmd.set("#SearchButton.Text", Lang.t(uuid, "panel.btn_buscar"));
        cmd.set("#AddButton.Text", Lang.t(uuid, "panel.btn_agregar"));
        cmd.set("#InventoryTitle.Text", Lang.t(uuid, "panel.inventario"));
        // Ids nuevos en 1.5.18 (Label, y el Label de la plantilla $C.@Title, como #PageTitle de
        // Fixtale). Los PlaceholderText van sobre TextField ya declarados, como #WikiSearchField
        // de la Wiki.
        cmd.set("#PanelTitle.Text", Lang.t(uuid, "panel.titulo"));
        cmd.set("#FilterLabel.Text", Lang.t(uuid, "panel.filtro"));
        cmd.set("#SearchLabel.Text", Lang.t(uuid, "panel.buscar_etiqueta"));
        cmd.set("#SearchInput.PlaceholderText", Lang.t(uuid, "panel.buscar_ayuda"));
        cmd.set("#ColNameLabel.Text", Lang.t(uuid, "panel.col_nombre"));
        cmd.set("#ColValueLabel.Text", Lang.t(uuid, "panel.col_valor"));
        cmd.set("#NewItemId.PlaceholderText", Lang.t(uuid, "panel.id_ayuda"));
        cmd.set("#HotbarTitle.Text", Lang.t(uuid, "panel.barra_rapida"));

        // Show/hide sections based on active tab
        boolean isConfigTab = activeTab.equals("config");
        cmd.set("#CategoryRow.Visible", !isConfigTab);
        cmd.set("#SearchRow.Visible", !isConfigTab);
        cmd.set("#TotalLabel.Visible", !isConfigTab);
        cmd.set("#ColumnHeaders.Visible", !isConfigTab);
        cmd.set("#ItemList.Visible", !isConfigTab);
        cmd.set("#AddSection.Visible", !isConfigTab);
        cmd.set("#AddRow.Visible", !isConfigTab);
        cmd.set("#InvPanel.Visible", !isConfigTab);
        cmd.set("#ConfigList.Visible", isConfigTab);

        if (isConfigTab) {
            buildConfigSection(cmd, evt);
            // Only bind tabs for config, rest not needed
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabFoods", EventData.of("Action", "tab:foods"), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabDrinks", EventData.of("Action", "tab:drinks"), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabConfig", EventData.of("Action", "tab:config"), false);
            return;
        }

        // Render player inventory
        renderInventory(cmd, evt, ref, store);

        Map<String, Float> allItems = activeTab.equals("foods") 
            ? EasyHunger.get().getFoodsConfig().getFoodValues()
            : EasyHunger.get().getDrinksConfig().getDrinkValues();

        // Show/hide category buttons based on installed mods.
        // La lista de instalados se calcula una vez (antes eran 2 busquedas de asset por item y
        // por cada uno de los 14 botones).
        List<String> installedIds = installedItemIds(allItems);
        cmd.set("#CatVanilla.Visible", hasCategoryItems("vanilla", installedIds));
        cmd.set("#CatNoCube.Visible", hasCategoryItems("nocube", installedIds));
        cmd.set("#CatSnip3.Visible", hasCategoryItems("snip3", installedIds));
        cmd.set("#CatHiddens.Visible", hasCategoryItems("hiddens", installedIds));
        cmd.set("#CatGalore.Visible", hasCategoryItems("galore", installedIds));
        cmd.set("#CatConnor.Visible", hasCategoryItems("connor", installedIds));
        cmd.set("#CatCaba.Visible", hasCategoryItems("caba", installedIds));
        cmd.set("#CatZcGaia.Visible", hasCategoryItems("zcgaia", installedIds));
        cmd.set("#CatHatsuya.Visible", hasCategoryItems("hatsuya", installedIds));
        cmd.set("#CatEndgame.Visible", hasCategoryItems("endgame", installedIds));
        cmd.set("#CatSpice.Visible", hasCategoryItems("spice", installedIds));
        cmd.set("#CatAle.Visible", hasCategoryItems("ale", installedIds));
        cmd.set("#CatCheff.Visible", hasCategoryItems("cheff", installedIds));
        cmd.set("#CatOther.Visible", hasCategoryItems("other", installedIds));

        renderItemList(cmd, evt, language, searchQuery.toLowerCase().trim(), allItems);

        // Bind tabs
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabFoods", EventData.of("Action", "tab:foods"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabDrinks", EventData.of("Action", "tab:drinks"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabConfig", EventData.of("Action", "tab:config"), false);

        // Bind category filters
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatAll", EventData.of("Action", "cat:all"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatVanilla", EventData.of("Action", "cat:vanilla"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatNoCube", EventData.of("Action", "cat:nocube"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatSnip3", EventData.of("Action", "cat:snip3"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatHiddens", EventData.of("Action", "cat:hiddens"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatGalore", EventData.of("Action", "cat:galore"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatConnor", EventData.of("Action", "cat:connor"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatCaba", EventData.of("Action", "cat:caba"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatZcGaia", EventData.of("Action", "cat:zcgaia"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatHatsuya", EventData.of("Action", "cat:hatsuya"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatEndgame", EventData.of("Action", "cat:endgame"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatSpice", EventData.of("Action", "cat:spice"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatAle", EventData.of("Action", "cat:ale"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatCheff", EventData.of("Action", "cat:cheff"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CatOther", EventData.of("Action", "cat:other"), false);

        // Bind search (button click)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SearchButton",
            new EventData().append("Action", "search").append("@SearchQuery", "#SearchInput.Value"));
        
        // Bind search (live search when typing)
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
            new EventData().append("Action", "search").append("@SearchQuery", "#SearchInput.Value"), false);

        // Bind add
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton",
            new EventData().append("Action", "add").append("@NewItemId", "#NewItemId.Value").append("@NewValue", "#NewValue.Value"));
    }

    /**
     * Gets the display name for an item, using translation if available, 
     * otherwise formatting the item ID.
     */
    private String getDisplayName(String itemId, String language) {
        if (itemId == null || itemId.isEmpty()) {
            return "Unknown";
        }

        // Try to get the Item object and its translation
        try {
            Item item = (Item) Item.getAssetMap().getAsset(itemId);
            if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
            if (item != null) {
                String key = item.getTranslationKey();
                if (key != null) {
                    String translated = I18nModule.get().getMessage(language, key);
                    if (translated != null && !translated.isEmpty()) {
                        return translated;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to formatting the ID
        }

        // Fallback: format the item ID as a readable name
        return formatItemId(itemId);
    }

    /**
     * Formats an item ID into a readable display name.
     * Example: "Food_Beef_Raw" -> "Beef Raw"
     */
    private String formatItemId(String itemId) {
        if (itemId == null) return "Unknown";
        
        // Special display name mappings
        if (itemId.equals("EasyHunger_Odre")) return "EasyHunger WaterSkin";
        if (itemId.equals("EasyHunger_WaterBowl")) return "EasyHunger Water Bowl";
        
        String id = itemId;
        
        // Remove namespace prefix if present (e.g., "hytale:Food_Beef")
        if (id.contains(":")) {
            id = id.substring(id.indexOf(":") + 1);
        }
        
        // Remove common prefixes
        String[] prefixesToRemove = {
            "Food_", "Plant_", "Ingredient_", "Halloween_", "Christmas_",
            "NoCube_", "Bowl_", "Brewery_", "AndiemgCheff_", "Drink_"
        };
        
        for (String prefix : prefixesToRemove) {
            if (id.startsWith(prefix)) {
                id = id.substring(prefix.length());
                break;
            }
        }
        
        // Replace underscores with spaces
        return id.replace("_", " ");
    }

    /**
     * Maps config IDs to the actual item IDs for icon display.
     * EasyHunger items use prefixes in config but full IDs for assets.
     */
    private String getIconItemId(String configId) {
        // EasyHunger items: config uses short form, actual item has _Empty suffix
        if (configId.equals("EasyHunger_Odre")) {
            return "EasyHunger_Odre_Empty";
        }
        if (configId.equals("EasyHunger_WaterBowl")) {
            return "EasyHunger_WaterBowl_Empty";
        }
        Item itemWithStar = (Item) Item.getAssetMap().getAsset("*" + configId);
        if (itemWithStar != null) {
            return "*" + configId;
        }
        // Default: use as-is
        return configId;
    }

    private boolean matchesCategory(String itemId) {
        return ItemCategoryFilter.matchesCategory(itemId, category);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull ConfigEventData data) {
        
        if (data == null || data.action == null) return;
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (data.action.startsWith("tab:")) {
            activeTab = data.action.substring(4);
            searchQuery = "";
            category = "all";
            rebuild(ref, store);
        } else if (data.action.startsWith("cat:")) {
            category = data.action.substring(4);
            updateItemList(ref, store);
        } else if (data.action.equals("search")) {
            searchQuery = data.searchQuery != null ? data.searchQuery : "";
            updateItemList(ref, store);
        } else if (data.action.equals("add")) {
            handleAdd(data);
            rebuild(ref, store);
         } else if (data.action.startsWith("invSlot:")) {
            int slot = Integer.parseInt(data.action.substring(8));
            ItemStack stack;
            if (slot < 36) {
                InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
                stack = storageComp != null ? storageComp.getInventory().getItemStack((short) slot) : null;
            } else {
                InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
                stack = hotbarComp != null ? hotbarComp.getInventory().getItemStack((short)(slot - 36)) : null;
            }
            if (stack != null && !stack.isEmpty()) {
                UICommandBuilder cmd = new UICommandBuilder();
                String itemId = stack.getItemId();
                if (itemId != null && itemId.startsWith("*")) {
                    itemId = itemId.substring(1);
                }
                cmd.set("#NewItemId.Value", itemId);
                cmd.set("#InventoryFeedback.Text", Lang.t(playerRef.getUuid(), "panel.seleccionado", "item", itemId));
                sendUpdate(cmd, new UIEventBuilder(), false);
            }
        } else if (data.action.equals("setValue")) {
            handleSetValue(data);
        } else if (data.action.equals("setConfig")) {
            handleSetConfig(data);
        }
    }
    
    /**
     * Updates only the item list without rebuilding the entire UI.
     * This preserves the search input field value.
     */
    private void updateItemList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        
        String language = playerRef.getLanguage();

        Map<String, Float> allItems = activeTab.equals("foods")
            ? EasyHunger.get().getFoodsConfig().getFoodValues()
            : EasyHunger.get().getDrinksConfig().getDrinkValues();

        // Clear existing list container
        cmd.clear("#ItemList");

        // Aqui la busqueda no se recorta (sin trim), igual que antes.
        renderItemList(cmd, evt, language, searchQuery.toLowerCase(), allItems);

        sendUpdate(cmd, evt, false);
    }

    /**
     * Filtra y pinta la lista de items y el contador. Lo comparten build() y updateItemList(),
     * que antes lo tenian copiado casi linea a linea; la unica diferencia (el trim de la
     * busqueda) la decide quien llama con {@code query}.
     *
     * Filter by:
     * 1. Item exists in game (installed mods only) OR is from EasyHunger mod
     * 2. Search query (searches both ID and display name)
     * 3. Category filter
     */
    private void renderItemList(UICommandBuilder cmd, UIEventBuilder evt, String language,
                                String query, Map<String, Float> allItems) {
        Map<String, Float> items = allItems.entrySet().stream()
            .filter(e -> {
                String itemId = e.getKey();
                // Always show EasyHunger mod items (our own mod)
                if (itemId.startsWith("EasyHunger_")) return true;
                // Always show items that would be categorized as OTHER (custom entries)
                if (ItemCategoryFilter.getCategory(itemId).equals("other")) return true;
                // Check if item exists in game for other mods
                Item item = (Item) Item.getAssetMap().getAsset(itemId);
                if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
                return item != null;
            })
            .filter(e -> {
                if (query.isEmpty()) return true;
                String id = e.getKey().toLowerCase();
                String displayName = getDisplayName(e.getKey(), language).toLowerCase();
                return id.contains(query) || displayName.contains(query);
            })
            .filter(e -> matchesCategory(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        itemIds.clear();

        int index = 0;
        for (Map.Entry<String, Float> entry : items.entrySet()) {
            String itemId = entry.getKey();
            float value = entry.getValue();

            itemIds.add(itemId);

            String selector = "#ItemList[" + index + "]";
            cmd.append("#ItemList", "Pages/ItemEntry.ui");

            // Set item icon - map EasyHunger config IDs to real item IDs
            String iconId = getIconItemId(itemId);
            cmd.set(selector + " #ItemIcon.ItemId", iconId);

            // Show display name with item ID in tooltip
            String displayName = getDisplayName(itemId, language);
            cmd.set(selector + " #ItemName.Text", displayName);
            cmd.set(selector + " #ItemName.TooltipTextSpans", Message.raw(itemId));
            cmd.set(selector + " #ValueInput.Value", formatValue(value));

            evt.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                selector + " #ValueInput",
                new EventData()
                    .append("Action", "setValue")
                    .append("Index", String.valueOf(index))
                    .append("@Value", selector + " #ValueInput.Value"),
                false
            );

            index++;
        }

        // Total label
        boolean foods = activeTab.equals("foods");
        UUID uuid = playerRef.getUuid();
        if (query.isEmpty() && category.equals("all")) {
            cmd.set("#TotalLabel.Text", Lang.t(uuid, foods ? "panel.total_comidas" : "panel.total_bebidas",
                    "n", index));
        } else {
            cmd.set("#TotalLabel.Text", Lang.t(uuid, foods ? "panel.mostrando_comidas" : "panel.mostrando_bebidas",
                    "n", index, "total", allItems.size()));
        }
    }

    /**
     * Builds the config section with all editable settings
     */
    private void buildConfigSection(UICommandBuilder cmd, UIEventBuilder evt) {
        var config = EasyHunger.get().getConfig();
        
        // Define all config entries: key, label (clave de lang), value, tooltip (clave de lang)
        Object[][] configs = {
            // Hunger configs
            {"MaxHunger", "cfg.max_hunger", String.valueOf(config.getMaxHunger()), "tip.max_hunger"},
            {"StarvationTickRate", "cfg.starvation_tick_rate", String.valueOf(config.getStarvationTickRate()), "tip.starvation_tick_rate"},
            {"StarvationPerTick", "cfg.starvation_per_tick", String.valueOf(config.getStarvationPerTick()), "tip.starvation_per_tick"},
            {"StarvationDamage", "cfg.starvation_damage", String.valueOf(config.getStarvationDamage()), "tip.starvation_damage"},
            {"HungryThreshold", "cfg.hungry_threshold", String.valueOf(config.getHungryThreshold()), "tip.hungry_threshold"},
            {"BlockBreakHungerCost", "cfg.block_break_hunger_cost", String.valueOf(config.getBlockBreakHungerCost()), "tip.block_break_hunger_cost"},
            {"JumpHungerCost", "cfg.jump_hunger_cost", String.valueOf(config.getJumpHungerCost()), "tip.jump_hunger_cost"},
            // Thirst configs
            {"MaxThirst", "cfg.max_thirst", String.valueOf(config.getMaxThirst()), "tip.max_thirst"},
            {"ThirstDecayRate", "cfg.thirst_decay_rate", String.valueOf(config.getThirstDecayRate()), "tip.thirst_decay_rate"},
            {"SprintThirstMultiplier", "cfg.sprint_thirst_multiplier", String.valueOf(config.getSprintThirstMultiplier()), "tip.sprint_thirst_multiplier"},
            {"ThirstDamage", "cfg.thirst_damage", String.valueOf(config.getThirstDamage()), "tip.thirst_damage"},
            {"ThirstyThreshold", "cfg.thirsty_threshold", String.valueOf(config.getThirstyThreshold()), "tip.thirsty_threshold"},
            {"BlockBreakThirstCost", "cfg.block_break_thirst_cost", String.valueOf(config.getBlockBreakThirstCost()), "tip.block_break_thirst_cost"},
            {"JumpThirstCost", "cfg.jump_thirst_cost", String.valueOf(config.getJumpThirstCost()), "tip.jump_thirst_cost"},
            {"StarvationStaminaModifier", "cfg.starvation_stamina_modifier", String.valueOf(config.getStarvationStaminaModifier()), "tip.starvation_stamina_modifier"},

            // Well Fed configs
            {"WellFedEnabled", "cfg.well_fed_enabled", String.valueOf(config.isWellFedEnabled()), "tip.well_fed_enabled"},
            {"WellFedThreshold", "cfg.well_fed_threshold", String.valueOf(config.getWellFedThreshold()), "tip.well_fed_threshold"},

            // Sleep + Idle Pause configs
            {"PauseWhileSleeping", "cfg.pause_while_sleeping", String.valueOf(config.isPauseWhileSleeping()), "tip.pause_while_sleeping"},
            {"IdlePauseEnabled", "cfg.idle_pause_enabled", String.valueOf(config.isIdlePauseEnabled()), "tip.idle_pause_enabled"},
            {"IdlePauseSeconds", "cfg.idle_pause_seconds", String.valueOf(config.getIdlePauseSeconds()), "tip.idle_pause_seconds"},

            // HUD position
            {"HudEstilo", "cfg.hud_estilo", config.getHudEstilo(), "tip.hud_estilo"},
            {"HudOffsetY", "cfg.hud_offset_y", String.valueOf(config.getHudOffsetY()), "tip.hud_offset_y"},
            {"HudInitDelayMs", "cfg.hud_init_delay_ms", String.valueOf(config.getHudInitDelayMs()), "tip.hud_init_delay_ms"},

            // Mundos donde el mod NO actua (lista separada por comas; admite "prefijo*")
            {"DisabledWorlds", "cfg.disabled_worlds", String.join(", ", config.getDisabledWorlds()), "tip.disabled_worlds"}
        };

        UUID uuid = playerRef.getUuid();
        for (int i = 0; i < configs.length; i++) {
            String key = (String) configs[i][0];
            String label = Lang.t(uuid, (String) configs[i][1]);
            String value = (String) configs[i][2];
            String tooltip = Lang.t(uuid, (String) configs[i][3]);
            
            String selector = "#ConfigList[" + i + "]";
            cmd.append("#ConfigList", "Pages/ConfigEntry.ui");
            
            cmd.set(selector + " #ConfigLabel.Text", label);
            cmd.set(selector + " #ConfigValue.Value", value);
            cmd.set(selector + ".TooltipText", tooltip);
            if (key.equals("DisabledWorlds")) {
                // Una lista de nombres no cabe en 120 px. Se ensancha SOLO esta fila desde Java;
                // el .ui compartido no se toca (hay un espaciador flexible entre etiqueta y campo).
                com.hypixel.hytale.server.core.ui.Anchor wide = new com.hypixel.hytale.server.core.ui.Anchor();
                wide.setWidth(com.hypixel.hytale.server.core.ui.Value.of(320));
                wide.setHeight(com.hypixel.hytale.server.core.ui.Value.of(26));
                wide.setVertical(com.hypixel.hytale.server.core.ui.Value.of(0));
                cmd.setObject(selector + " #ConfigValue.Anchor", wide);
            }
            
            shownConfigValues.put(key, value);

            // FocusLost, no ValueChanged: ValueChanged aplicaba cada tecla, asi que escribir
            // MaxHunger=100 pasaba por 1 y recortaba a 1 el hambre guardada de todos. Al perder el
            // foco solo llega el valor final. Es el patron de la pagina vanilla
            // TriggerVolumeInspectorPage (TextField #SelectedNameField + FocusLost + .Value).
            evt.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                selector + " #ConfigValue",
                new EventData()
                    .append("Action", "setConfig")
                    .append("ConfigKey", key)
                    .append("@Value", selector + " #ConfigValue.Value"),
                false
            );
        }
    }

    private void handleAdd(ConfigEventData data) {
        if (data.newItemId == null || data.newItemId.trim().isEmpty()) {
            playerRef.sendMessage(Msg.error(playerRef.getUuid(), "panel.id_vacio"));
            return;
        }

        String itemId = data.newItemId.trim();
        float value = 5.0f;

        if (data.newValue != null && !data.newValue.trim().isEmpty()) {
            try {
                value = Float.parseFloat(data.newValue.trim());
            } catch (NumberFormatException e) {
                playerRef.sendMessage(Msg.error(playerRef.getUuid(), "panel.valor_no_numero"));
                return;
            }
        }

        if (activeTab.equals("foods")) {
            EasyHunger.get().getFoodsConfig().setFoodValue(itemId, value);
            EasyHunger.get().saveFoodsConfig();
        } else {
            EasyHunger.get().getDrinksConfig().setDrinkValue(itemId, value);
            EasyHunger.get().saveDrinksConfig();
        }
        playerRef.sendMessage(Msg.info(playerRef.getUuid(), "panel.agregado",
                "item", itemId, "valor", formatValue(value)));
    }

    private void handleSetValue(ConfigEventData data) {
        if (data.index == null || data.value == null) return;
        
        try {
            int idx = Integer.parseInt(data.index);
            if (idx < 0 || idx >= itemIds.size()) return;
            
            String itemId = itemIds.get(idx);
            float newValue;
            
            try {
                newValue = Float.parseFloat(data.value.trim());
                newValue = Math.max(0, newValue);
            } catch (NumberFormatException e) {
                playerRef.sendMessage(Msg.error(playerRef.getUuid(), "panel.valor_item_invalido", "item", itemId));
                return;
            }
            
            if (activeTab.equals("foods")) {
                EasyHunger.get().getFoodsConfig().setFoodValue(itemId, newValue);
                EasyHunger.get().saveFoodsConfig();
            } else {
                EasyHunger.get().getDrinksConfig().setDrinkValue(itemId, newValue);
                EasyHunger.get().saveDrinksConfig();
            }
            playerRef.sendMessage(Msg.info(playerRef.getUuid(), "panel.actualizado",
                    "item", itemId, "valor", formatValue(newValue)));
            
        } catch (NumberFormatException e) {
            // Ignore invalid index
        }
    }

    /**
     * Por que no se aplico un valor, en el idioma de quien lo lee. Los rechazos de rango traen su
     * clave de lang; cualquier otra excepcion sale con su mensaje tal cual.
     */
    static String motivo(UUID uuid, IllegalArgumentException e) {
        if (e instanceof EasyHungerConfig.ValorRechazado r) {
            return Lang.t(uuid, r.clave, r.args);
        }
        return String.valueOf(e.getMessage());
    }

    /** 1.5.20: ANILLOS o BARRAS; otra cosa se rechaza con el mensaje en espanol. */
    private static String parseHudEstilo(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase();
        if (v.equals("ANILLOS") || v.equals("BARRAS")) return v;
        throw new EasyHungerConfig.ValorRechazado(
                "HudEstilo debe ser ANILLOS o BARRAS (llegó " + raw + ")",
                "rango.hud_estilo", "valor", raw);
    }

    private void handleSetConfig(ConfigEventData data) {
        if (data.configKey == null || data.value == null) return;
        
        var config = EasyHunger.get().getConfig();
        String key = data.configKey;
        String valueStr = data.value.trim();

        // FocusLost llega tambien al entrar y salir del campo sin tocarlo: si el valor es el
        // mismo que ya se aplico, no se guarda ni se avisa otra vez.
        if (valueStr.equals(shownConfigValues.get(key))) return;
        
        try {
            switch (key) {
                // Integer configs
                case "MaxHunger":
                    config.setMaxHunger(Integer.parseInt(valueStr));
                    break;
                case "MaxThirst":
                    config.setMaxThirst(Integer.parseInt(valueStr));
                    break;
                    
                // Float configs
                case "StarvationTickRate":
                    config.setStarvationTickRate(Float.parseFloat(valueStr));
                    break;
                case "StarvationPerTick":
                    config.setStarvationPerTick(Float.parseFloat(valueStr));
                    break;
                case "StarvationDamage":
                    config.setStarvationDamage(Float.parseFloat(valueStr));
                    break;
                case "HungryThreshold":
                    config.setHungryThreshold(Float.parseFloat(valueStr));
                    break;
                case "BlockBreakHungerCost":
                    config.setBlockBreakHungerCost(Float.parseFloat(valueStr));
                    break;
                case "JumpHungerCost":
                    config.setJumpHungerCost(Float.parseFloat(valueStr));
                    break;
                case "ThirstDecayRate":
                    config.setThirstDecayRate(Float.parseFloat(valueStr));
                    break;
                case "SprintThirstMultiplier":
                    config.setSprintThirstMultiplier(Float.parseFloat(valueStr));
                    break;
                case "ThirstDamage":
                    config.setThirstDamage(Float.parseFloat(valueStr));
                    break;
                case "ThirstyThreshold":
                    config.setThirstyThreshold(Float.parseFloat(valueStr));
                    break;
                case "StarvationStaminaModifier":
                    config.setStarvationStaminaModifier(Float.parseFloat(valueStr));
                    break;
                case "WellFedThreshold":
                    config.setWellFedThreshold(Float.parseFloat(valueStr));
                    break;
                case "WellFedEnabled":
                    config.setWellFedEnabled(Boolean.parseBoolean(valueStr));
                    break;

                // New thirst drain costs (parity with hunger)
                case "BlockBreakThirstCost":
                    config.setBlockBreakThirstCost(Float.parseFloat(valueStr));
                    break;
                case "JumpThirstCost":
                    config.setJumpThirstCost(Float.parseFloat(valueStr));
                    break;

                // Sleep + Idle Pause
                case "PauseWhileSleeping":
                    config.setPauseWhileSleeping(Boolean.parseBoolean(valueStr));
                    break;
                case "IdlePauseEnabled":
                    config.setIdlePauseEnabled(Boolean.parseBoolean(valueStr));
                    break;
                case "IdlePauseSeconds":
                    config.setIdlePauseSeconds(Float.parseFloat(valueStr));
                    break;

                // HUD position
                case "HudEstilo":
                    config.setHudEstilo(parseHudEstilo(valueStr));
                    break;
                case "HudOffsetY":
                    config.setHudOffsetY(Integer.parseInt(valueStr));
                    break;
                case "HudInitDelayMs":
                    config.setHudInitDelayMs(Integer.parseInt(valueStr));
                    break;
                case "DisabledWorlds":
                    // "Hub, Coliseo, instance-Hauldron*" -> entradas sin vacios. Vacio = el mod
                    // actua en todos los mundos.
                    config.setDisabledWorlds(java.util.Arrays.stream(valueStr.split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));
                    break;

                default:
                    playerRef.sendMessage(Msg.error(playerRef.getUuid(), "panel.clave_desconocida", "clave", key));
                    return;
            }
            
            // Save config
            EasyHunger.get().saveConfig();
            shownConfigValues.put(key, valueStr);
            playerRef.sendMessage(Msg.info(playerRef.getUuid(), "panel.config_actualizada",
                    "clave", key, "valor", valueStr));

        } catch (NumberFormatException e) {
            playerRef.sendMessage(Msg.error(playerRef.getUuid(), "panel.valor_config_invalido",
                    "clave", key, "valor", valueStr));
        } catch (IllegalArgumentException e) {
            // Fuera de rango (setters de EasyHungerConfig): no se aplica ni se guarda nada.
            playerRef.sendMessage(Msg.error(playerRef.getUuid(), "panel.no_aplicado",
                    "motivo", motivo(playerRef.getUuid(), e)));
        }
    }

    /**
     * Renders the player's inventory grid (4 rows of 9 slots + hotbar).
     */
    private void renderInventory(UICommandBuilder cmd, UIEventBuilder evt, Ref<EntityStore> ref, Store<EntityStore> store) {
        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());

        // Main inventory: 4 rows of 9 slots (slots 0-35)
        for (int row = 0; row < 4; row++) {
            String rowId = "#InvRow" + row;
            cmd.clear(rowId);
            for (int i = 0; i < 9; i++) {
                int slot = row * 9 + i;
                cmd.append(rowId, "Pages/EasyHungerInventorySlot.ui");
                String sel = rowId + "[" + i + "]";
                ItemStack stack = storageComp != null ? storageComp.getInventory().getItemStack((short) slot) : null;
                if (stack != null && !stack.isEmpty()) {
                    cmd.set(sel + " #ItemIcon.ItemId", stack.getItemId());
                    if (stack.getQuantity() > 1) {
                        cmd.set(sel + " #Quantity.Text", String.valueOf(stack.getQuantity()));
                        cmd.set(sel + " #Quantity.Visible", true);
                    }
                }
                evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                    EventData.of("Action", "invSlot:" + slot), false);
            }
        }
        // Hotbar: 9 slots (slots 36-44)
        cmd.clear("#HotbarRow");
        for (int i = 0; i < 9; i++) {
            cmd.append("#HotbarRow", "Pages/EasyHungerInventorySlot.ui");
            String sel = "#HotbarRow[" + i + "]";
            ItemStack stack = hotbarComp != null ? hotbarComp.getInventory().getItemStack((short) i) : null;
            if (stack != null && !stack.isEmpty()) {
                cmd.set(sel + " #ItemIcon.ItemId", stack.getItemId());
                if (stack.getQuantity() > 1) {
                    cmd.set(sel + " #Quantity.Text", String.valueOf(stack.getQuantity()));
                    cmd.set(sel + " #Quantity.Visible", true);
                }
            }
            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #SlotButton",
                EventData.of("Action", "invSlot:" + (36 + i)), false);
        }
    }

    private void rebuild(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new EasyHungerValuesPage(playerRef, activeTab, searchQuery, category));
        }
    }

    private String formatValue(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }

    /**
     * Ids de allItems que existen en el juego (o son de EasyHunger), en el orden del mapa.
     */
    private static List<String> installedItemIds(Map<String, Float> allItems) {
        List<String> installed = new ArrayList<>();
        for (String itemId : allItems.keySet()) {
            // Check if item exists in game (or is from EasyHunger)
            if (!itemId.startsWith("EasyHunger_")) {
                Item item = (Item) Item.getAssetMap().getAsset(itemId);
                if (item == null) item = (Item) Item.getAssetMap().getAsset("*" + itemId);
                if (item == null) continue;
            }
            installed.add(itemId);
        }
        return installed;
    }

    /**
     * Checks if there are any installed items for a specific category.
     * Used to hide category buttons when no items from that mod are installed.
     */
    private static boolean hasCategoryItems(String categoryName, List<String> installedIds) {
        // Check category using the filter
        return installedIds.stream().anyMatch(itemId -> ItemCategoryFilter.matchesCategory(itemId, categoryName));
    }
}
