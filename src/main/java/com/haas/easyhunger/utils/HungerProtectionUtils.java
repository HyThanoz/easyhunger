package com.haas.easyhunger.utils;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.joml.Vector3d;
import java.lang.reflect.Method;
import java.util.UUID;

public class HungerProtectionUtils {

    private static Object regionManager;
    private static Method getApiMethod;
    private static Method getRegionMethod;
    private static Method getRegionsAtMethod;
    
    // Essentials fields
    private static Object essentialsInstance;
    private static Object spawnProtectionManager;
    private static Method isInProtectedAreaMethod;
    private static boolean essentialsChecked = false;
    private static boolean safeAreasChecked = false;

    // Cache for protection checks to improve performance
    private static final java.util.Map<String, CachedSafeStatus> safeCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 1000; // Check once per second

    private static class CachedSafeStatus {
        long timestamp;
        boolean isSafe;
        CachedSafeStatus(boolean isSafe, long timestamp) { 
            this.isSafe = isSafe; 
            this.timestamp = timestamp; 
        }
    }

    /**
     * Removes the cached safe-zone status for a player. Call this when the player disconnects
     * so the cache doesn't accumulate stale entries forever.
     */
    public static void invalidateCache(UUID uuid) {
        if (uuid != null) safeCache.remove(uuid.toString());
    }

    // PlayerRef has no getWorld() in current server builds — probe for it ONCE instead of
    // throwing+swallowing NoSuchMethodException on every call (isSafe runs per block break).
    private static Method playerGetWorldMethod;
    private static Method worldGetNameMethod;
    private static boolean worldMethodProbed = false;

    public static boolean isSafe(PlayerRef player) {
        if (player == null) return false;

        UUID uuid = player.getUuid();
        if (uuid == null) return false;

        // Cache first — the world-name resolution below is only worth paying on a miss.
        String playerId = uuid.toString();
        long now = System.currentTimeMillis();
        CachedSafeStatus status = safeCache.get(playerId);
        if (status != null && now - status.timestamp < CACHE_DURATION_MS) {
            return status.isSafe;
        }

        try {
            boolean safe = checkSafety(player, resolveWorldName(player));
            safeCache.put(playerId, new CachedSafeStatus(safe, now));
            return safe;
        } catch (Throwable e) {
            log("Critical error in isSafe: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private static String resolveWorldName(PlayerRef player) {
        if (!worldMethodProbed) {
            worldMethodProbed = true;
            try {
                playerGetWorldMethod = player.getClass().getMethod("getWorld");
            } catch (Exception e) {
                // Not available — every lookup falls back to "default" (historic behavior).
            }
        }
        if (playerGetWorldMethod == null) return "default";

        try {
            Object world = playerGetWorldMethod.invoke(player);
            if (world != null) {
                if (worldGetNameMethod == null) {
                    worldGetNameMethod = world.getClass().getMethod("getName");
                }
                String name = (String) worldGetNameMethod.invoke(world);
                if (name != null) return name;
            }
        } catch (Exception ignored) {
        }
        return "default";
    }

    private static boolean checkSafety(PlayerRef player, String worldName) {
        if (isEssentialsSpawnProtected(player, worldName)) return true;
        if (isSafeAreasProtected(player, worldName)) return true;
        return false;
    }

    private static boolean isSafeAreasProtected(PlayerRef player, String worldName) {
        try {
            // Lazy init
            // Se prueba UNA sola vez. Sin este guard, cuando SafeAreas no esta instalado
            // -que es el caso en MyTale- el Class.forName de abajo lanzaba y se tragaba una
            // ClassNotFoundException (con su stack trace) en CADA llamada: una por jugador y
            // por segundo, indefinidamente. El campo safeAreasChecked llevaba declarado desde
            // siempre pero nunca se habia cableado; la ruta gemela de Essentials si lo usa.
            if (regionManager == null && !safeAreasChecked) {
                safeAreasChecked = true;
                try {
                    // Start Retry Logic for SafeAreas
                    Class<?> regionManagerClass = Class.forName("com.mcodelogic.safeareas.manager.RegionManager");
                    java.lang.reflect.Field instanceField = regionManagerClass.getDeclaredField("instance");
                    regionManager = instanceField.get(null);

                    if (regionManager != null) {
                         Method getApi = regionManagerClass.getMethod("getApi");
                         getApiMethod = getApi;
                         Object api = getApi.invoke(regionManager);
                         
                         getRegionsAtMethod = api.getClass().getMethod("getRegionsAt", String.class, double.class, double.class, double.class);
                         // log("SafeAreas integrated successfully!");
                    }
                } catch (Exception e) {
                    // log("SafeAreas init failed: " + e);
                }
            }

            if (regionManager == null || getApiMethod == null || getRegionsAtMethod == null) {
                return false;
            }

            Object api = getApiMethod.invoke(regionManager);
            if (api == null) return false;

            Vector3d pos = player.getTransform().getPosition();
            
            // Uses the passed worldName directly
            java.util.Set<?> regions = (java.util.Set<?>) getRegionsAtMethod.invoke(api, 
                worldName, 
                pos.x(),
                pos.y(),
                pos.z()
            );

            if (regions != null && !regions.isEmpty()) {
                for (Object region : regions) {
                    try {
                        Method getFlagMethod = region.getClass().getMethod("getFlag", Class.forName("com.mcodelogic.safeareas.model.enums.RegionFlag"));
                        Class<?> regionFlagEnum = Class.forName("com.mcodelogic.safeareas.model.enums.RegionFlag");
                        
                        // Check IMMORTAL
                        try {
                            Object immortalFlag = Enum.valueOf((Class<Enum>) regionFlagEnum, "IMMORTAL");
                            Object flagValue = getFlagMethod.invoke(region, immortalFlag);
                            boolean isActive = checkFlagValue(flagValue);
                            
                            if (isActive) {
                                // log("Protection active: IMMORTAL flag detected!");
                                return true;
                            }
                        } catch (Exception ie) {
                            // log("Error checking IMMORTAL: " + ie);
                        }
                        
                        // Check INVULNERABLE
                        try {
                             Object invulnerableFlag = Enum.valueOf((Class<Enum>) regionFlagEnum, "INVULNERABLE");
                             Object flagValue = getFlagMethod.invoke(region, invulnerableFlag);
                             if (checkFlagValue(flagValue)) {
                                 // log("Protection active: INVULNERABLE flag detected.");
                                 return true;
                             }
                        } catch (Exception ie) {}

                    } catch (Exception e) {
                        continue;
                    }
                }
            }

            return false;

        } catch (Throwable e) {
            // log("SafeAreas check error: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean checkFlagValue(Object flagValueObj) {
        if (flagValueObj == null) return false;
        try {
             Method getValueMethod = flagValueObj.getClass().getMethod("getValue");
             Object value = getValueMethod.invoke(flagValueObj);
             if (value instanceof Boolean) {
                 return (Boolean) value;
             }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // The spawn's world rarely changes; re-resolve at most every 30s instead of running the
    // full Class.forName + field + method reflection chain on every uncached protection check.
    private static String cachedSpawnWorldName;
    private static long spawnWorldCachedAt = 0;
    private static final long SPAWN_WORLD_TTL_MS = 30_000;

    private static String getSpawnWorldName(String fallback) {
        long now = System.currentTimeMillis();
        if (cachedSpawnWorldName != null && now - spawnWorldCachedAt < SPAWN_WORLD_TTL_MS) {
            return cachedSpawnWorldName;
        }

        String spawnWorldName = fallback;
        try {
            Class<?> essentialsClass = Class.forName("com.nhulston.essentials.Essentials");
            Method getInstanceMethod = essentialsClass.getMethod("getInstance");
            Object essentials = getInstanceMethod.invoke(null);

            java.lang.reflect.Field storageField = essentialsClass.getDeclaredField("storageManager");
            storageField.setAccessible(true);
            Object storageManager = storageField.get(essentials);

            Method getSpawnMethod = storageManager.getClass().getMethod("getSpawn");
            Object spawn = getSpawnMethod.invoke(storageManager);

            if (spawn != null) {
                Method getWorldMethod = spawn.getClass().getMethod("getWorld");
                String resolved = (String) getWorldMethod.invoke(spawn);
                if (resolved != null) spawnWorldName = resolved;
            }
        } catch (Exception e) {
            // Use fallback worldName
        }

        cachedSpawnWorldName = spawnWorldName;
        spawnWorldCachedAt = now;
        return spawnWorldName;
    }

    private static void log(String message) {
        System.out.println("[EasyHunger-Protection] " + message);
    }

    private static boolean isEssentialsSpawnProtected(PlayerRef player, String worldName) {
        try {
            if (!essentialsChecked) {
                try {
                    Class<?> essentialsClass = Class.forName("com.nhulston.essentials.Essentials");
                    Method getInstanceMethod = essentialsClass.getMethod("getInstance");
                    essentialsInstance = getInstanceMethod.invoke(null);
                    
                    if (essentialsInstance != null) {
                        // The field spawnProtectionManager is private, so we need to set accessible
                        java.lang.reflect.Field spmField = essentialsClass.getDeclaredField("spawnProtectionManager");
                        spmField.setAccessible(true);
                        spawnProtectionManager = spmField.get(essentialsInstance);
                        
                        if (spawnProtectionManager != null) {
                            // New signature in Essentials 1.8.0: boolean isInProtectedArea(String worldName, Vector3d entityPos)
                            isInProtectedAreaMethod = spawnProtectionManager.getClass().getMethod("isInProtectedArea", String.class, Vector3d.class);
                            log("Essentials SpawnProtectionManager found! (v1.8.0+ signature)");
                        } else {
                             log("Essentials SpawnProtectionManager is NULL");
                        }
                    } else {
                        log("Essentials Instance is NULL");
                    }
                } catch (ClassNotFoundException e) {
                    log("Essentials not found (ClassNotFound)");
                } catch (Exception e) {
                    log("Error initializing Essentials reflection: " + e.getMessage());
                    e.printStackTrace();
                }
                essentialsChecked = true;
            }

            if (spawnProtectionManager == null || isInProtectedAreaMethod == null) {
                return false;
            }

			// Get position
            Vector3d pos = player.getTransform().getPosition();

            String spawnWorldName = getSpawnWorldName(worldName);

            boolean result = (boolean) isInProtectedAreaMethod.invoke(spawnProtectionManager, spawnWorldName, pos);
            return result;

        } catch (Throwable e) {
            log("Error checking spawn protection: " + e.toString());
            e.printStackTrace();
            return false;
        }
    }
}
