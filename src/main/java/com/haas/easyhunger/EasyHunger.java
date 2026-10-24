package com.haas.easyhunger;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.haas.easyhunger.commands.SetHungerCommand;
import com.haas.easyhunger.components.HungerComponent;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.haas.easyhunger.config.FoodsConfig;
import com.haas.easyhunger.config.DrinksConfig;
import com.haas.easyhunger.config.BiomeModifiersConfig;
import com.haas.easyhunger.config.PlayerPrefsConfig;
import com.haas.easyhunger.events.GameModeChangeListener;
import com.haas.easyhunger.events.EasyHungerPlayerReady;
import com.haas.easyhunger.systems.OnDeathSystem;
import com.haas.easyhunger.systems.StarveSystem;
import com.haas.easyhunger.systems.WellFedSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public class EasyHunger extends JavaPlugin {
    private static EasyHunger instance;
    private final Config<EasyHungerConfig> config;
    private final Config<FoodsConfig> foodsConfig;
    private final Config<DrinksConfig> drinksConfig;
    private final Config<BiomeModifiersConfig> biomeConfig;
    private final Config<PlayerPrefsConfig> playerPrefsConfig;
    private ComponentType<EntityStore, HungerComponent> hungerComponentType;
    private ComponentType<EntityStore, com.haas.easyhunger.components.ThirstComponent> thirstComponentType;

    /** Used only to schedule delayed HUD re-sends after a player joins (see EasyHungerPlayerReady). */
    private final java.util.concurrent.ScheduledExecutorService hudRetryScheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "EasyHunger-HudRetry");
                t.setDaemon(true);
                return t;
            });

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public EasyHunger(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("HungerConfig", EasyHungerConfig.CODEC);
        this.foodsConfig = this.withConfig("Foods", FoodsConfig.CODEC);
        this.drinksConfig = this.withConfig("Drinks", DrinksConfig.CODEC);
        this.biomeConfig = this.withConfig("BiomeModifiers", BiomeModifiersConfig.CODEC);
        this.playerPrefsConfig = this.withConfig("PlayerPrefs", PlayerPrefsConfig.CODEC);
    }

    @Override
    protected void setup () {
        super.setup();

        this.config.save();
        
        // Merge new default values without overwriting user customizations
        this.foodsConfig.get().mergeDefaults();
        this.drinksConfig.get().mergeDefaults();
        
        this.foodsConfig.save();
        this.drinksConfig.save();
        this.biomeConfig.save();


        // register hunger component
        this.hungerComponentType = this.getEntityStoreRegistry()
                .registerComponent(HungerComponent.class, "HungerComponent", HungerComponent.CODEC);
        
        // register thirst component
        this.thirstComponentType = this.getEntityStoreRegistry()
                .registerComponent(com.haas.easyhunger.components.ThirstComponent.class, "ThirstComponent", com.haas.easyhunger.components.ThirstComponent.CODEC);

        // Estado efimero por entidad (cooldowns de salto y colocacion). Se registra con la
        // sobrecarga SIN codec: no tiene sentido persistir "hace cuanto saltaste".
        final var entityStoreRegistry = this.getEntityStoreRegistry();
        com.haas.easyhunger.components.PlayerActionTimes.setComponentType(
                entityStoreRegistry.registerComponent(
                        com.haas.easyhunger.components.PlayerActionTimes.class,
                        com.haas.easyhunger.components.PlayerActionTimes::new));
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.PlayerActionTimesProvisionSystem());

        // register starve system
        entityStoreRegistry.registerSystem(StarveSystem.create());
        entityStoreRegistry.registerSystem(new OnDeathSystem());
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyHungerBlockBreakSystem());
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyHungerJumpSystem());
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyHungerBlockPlaceSystem());

        // ENABLED: Food Handler (Used ONLY for Infinite Food Mods like Eternal Meat)
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyHungerFoodHandler(this.hungerComponentType));
        
        // Thirst Handler
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyThirstHandler(this.thirstComponentType));
        
        // register thirst system
        entityStoreRegistry.registerSystem(com.haas.easyhunger.systems.EasyThirstSystem.create());

        // register thirst drain systems for parity with hunger (jump + block break)
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyThirstJumpSystem());
        entityStoreRegistry.registerSystem(new com.haas.easyhunger.systems.EasyThirstBlockBreakSystem());
        
        // WellFed se registra SIEMPRE, incluso desactivado: Nourished/Hydrated son efectos
        // "Infinite" y este sistema es el unico que los retira. Registrarlo solo cuando la opcion
        // estaba activa hacia que apagarla NO quitara los buffs ya puestos (se quedaban para
        // siempre). Ver WellFedSystem.create().
        entityStoreRegistry.registerSystem(WellFedSystem.create());
        EasyHungerConfig cfg = this.config.get();
        if (cfg.isWellFedEnabled()) {
            // El umbral es ABSOLUTO contra MaxHunger/MaxThirst, no un porcentaje: escribirlo con
            // "%" hacia leer 40 como 40% cuando con MaxHunger=50 es el 80%.
            logInfo("WellFed activo (umbral: " + cfg.getWellFedThreshold()
                    + " de " + cfg.getMaxHunger() + " hambre / " + cfg.getMaxThirst() + " sed)");
        } else {
            logInfo("WellFed desactivado — el sistema queda registrado solo para retirar "
                    + "Nourished/Hydrated a quien los tenga puestos.");
        }

        // Interactions
        final var interactionRegistry = this.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("EasyHunger_DrinkWater", com.haas.easyhunger.interactions.DrinkWaterInteraction.class, com.haas.easyhunger.interactions.DrinkWaterInteraction.CODEC);
        interactionRegistry.register("EasyHunger_ConsumeFood", com.haas.easyhunger.interactions.ConsumeFoodInteraction.class, com.haas.easyhunger.interactions.ConsumeFoodInteraction.CODEC);
        interactionRegistry.register("EasyHunger_RefillWaterskin", com.haas.easyhunger.interactions.RefillWaterskinInteraction.class, com.haas.easyhunger.interactions.RefillWaterskinInteraction.CODEC);
        interactionRegistry.register("EasyHunger_StartFeeding", com.haas.easyhunger.interactions.StartFeedingInteraction.class, com.haas.easyhunger.interactions.StartFeedingInteraction.CODEC);
        interactionRegistry.register("EasyHunger_FailedFeeding", com.haas.easyhunger.interactions.FailedFeedingInteraction.class, com.haas.easyhunger.interactions.FailedFeedingInteraction.CODEC);
        interactionRegistry.register("EasyHunger_StartDrinking", com.haas.easyhunger.interactions.StartDrinkingInteraction.class, com.haas.easyhunger.interactions.StartDrinkingInteraction.CODEC);
        interactionRegistry.register("EasyHunger_FailedDrinking", com.haas.easyhunger.interactions.FailedDrinkingInteraction.class, com.haas.easyhunger.interactions.FailedDrinkingInteraction.CODEC);

        // setup hunger component and hud on player join
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, EasyHungerPlayerReady::handle);
        this.getEventRegistry().registerGlobal(
            com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class,
            com.haas.easyhunger.events.EasyHungerPlayerDisconnect::handle
        );

        // Barrido de auto-recuperacion del HUD: recrea el HUD SOLO a quien deberia tenerlo y no lo
        // tiene (Modo A). Nunca reenvia uno existente — seguro por construccion (ver
        // EasyHungerPlayerReady.recoverMissingHuds). Toggle en config.
        if (cfg.isHudAutoRecoverEnabled()) {
            long interval = Math.max(3000L, cfg.getHudAutoRecoverIntervalMs());
            this.hudRetryScheduler.scheduleAtFixedRate(
                () -> { try { EasyHungerPlayerReady.recoverMissingHuds(); } catch (Exception ignored) { } },
                interval, interval, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        // listen to gamemode changes
        PacketAdapters.registerOutbound(new GameModeChangeListener());

        // register admin commands
        this.getCommandRegistry().registerCommand(new SetHungerCommand());
        this.getCommandRegistry().registerCommand(new com.haas.easyhunger.commands.SetThirstCommand());
        
        // Register the main command (which handles subcommands like hide, show, config)
        this.getCommandRegistry().registerCommand(new com.haas.easyhunger.commands.EasyHungerCommand());
        
        // Register the global config command (/ehconfig) for compatibility
        this.getCommandRegistry().registerCommand(new com.haas.easyhunger.commands.EasyHungerValuesCommand());

        // Register version info command (/ehversion)
        this.getCommandRegistry().registerCommand(new com.haas.easyhunger.commands.EhVersionCommand());


        // Try to prune recipes immediately, but also plan for a delayed pruning if needed
        this.pruneRecipes();
    }

    // NOTE: /eh, /eh hide|show and /ehversion are now permissionless (canGeneratePermission
    // = false in each command) — the old start() auto-grant to "Adventure"/"Creative" groups
    // was removed: those group names don't exist on servers with custom permission systems,
    // which silently left players unable to use the toggles.

    public void pruneRecipes() {
        if (this.getConfig().isThirstEnabled()) {
            return;
        }

        try {
            java.lang.reflect.Field registriesField = com.hypixel.hytale.builtin.crafting.CraftingPlugin.class.getDeclaredField("registries");
            registriesField.setAccessible(true);
            java.util.Map<String, com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry> registries = 
                (java.util.Map<String, com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry>) registriesField.get(null);
            
            if (registries != null) {
                for (com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry registry : registries.values()) {
                    boolean changed = false;
                    for (com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe recipe : registry.getAllRecipes()) {
                        String outputId = recipe.getPrimaryOutput() != null ? recipe.getPrimaryOutput().getItemId() : null;
                        if ("EasyHunger_Odre_Empty".equals(outputId) || "EasyHunger_WaterBowl_Empty".equals(outputId)) {
                            registry.removeRecipe(recipe.getId());
                            changed = true;
                        }
                    }
                    if (changed) {
                        registry.recompute();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.at(java.util.logging.Level.SEVERE).withCause(e).log("Failed to robustly prune Waterskin recipes");
        }
    }

    @Override
    protected void shutdown() {
        this.hudRetryScheduler.shutdownNow();
        super.shutdown();
    }

    /**
     * Schedules a task to run on the given world thread after {@code delayMillis}. The world is an
     * Executor, so we hop back onto its thread before touching any player/packet state.
     */
    /**
     * Ejecuta la tarea tras el retardo SIN atarla a un mundo concreto.
     *
     * Para envios diferidos de HUD: el jugador puede haber cambiado de mundo durante la espera,
     * asi que la tarea debe resolver ella misma donde esta y saltar al hilo correcto. Usar
     * {@link #scheduleOnWorld} para eso fijaba el mundo de ORIGEN, que es de donde venia el bug
     * de las barras perdidas.
     */
    public void scheduleDetached(long delayMillis, Runnable task) {
        this.hudRetryScheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception ignored) {
                // La tarea ya loguea sus propios abortos.
            }
        }, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void scheduleOnWorld(World world, long delayMillis, Runnable task) {
        this.hudRetryScheduler.schedule(() -> {
            try {
                world.execute(task);
            } catch (Exception ignored) {
                // World may have unloaded (e.g. shutdown) — nothing to retry against.
            }
        }, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void saveConfig() {
        this.config.save();
    }

    public void saveFoodsConfig() {
        this.foodsConfig.save();
    }

    public void saveDrinksConfig() {
        this.drinksConfig.save();
    }

    public ComponentType<EntityStore, HungerComponent> getHungerComponentType() {
        return this.hungerComponentType;
    }

    public ComponentType<EntityStore, com.haas.easyhunger.components.ThirstComponent> getThirstComponentType() {
        return this.thirstComponentType;
    }

    public EasyHungerConfig getConfig() {
        return this.config.get();
    }

    public BiomeModifiersConfig getBiomeConfig() {
        return this.biomeConfig.get();
    }

    public FoodsConfig getFoodsConfig() {
        return this.foodsConfig.get();
    }

    public DrinksConfig getDrinksConfig() {
        return this.drinksConfig.get();
    }

    public PlayerPrefsConfig getPlayerPrefsConfig() {
        return this.playerPrefsConfig.get();
    }

    public void savePlayerPrefsConfig() {
        this.playerPrefsConfig.save();
    }

    public static EasyHunger get() {
        return instance;
    }

    public static void logInfo(String message) {
        LOGGER.at(Level.WARNING).log(message);
    }
}



