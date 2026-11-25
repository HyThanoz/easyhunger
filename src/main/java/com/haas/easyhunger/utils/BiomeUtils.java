package com.haas.easyhunger.utils;

import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;

import javax.annotation.Nullable;

/**
 * Utility class for detecting biome at player's position.
 * Uses the same approach as BetterMap mod.
 */
public class BiomeUtils {

    /**
     * Biomes are deterministic per world seed and never change at runtime, but
     * generateZoneBiomeResultAt() re-runs worldgen noise sampling on every call — by far the
     * most expensive thing this mod does per tick. Cache results per chunk (16x16 blocks),
     * per world. Chunk granularity trades exactness at biome borders (irrelevant for a drain
     * multiplier) for a cache that stays small and hits almost always.
     */
    private static final int MAX_CACHED_CHUNKS_PER_WORLD = 65536;

    /**
     * 1.5.31 (R7): worlds were never removed from the cache, so every dungeon instance
     * ("instance-...-<uuid>") left up to 65 536 entries in memory until the next restart.
     * Now: the world's entry is dropped when the world is removed or its worldgen is cleared
     * (see {@link #forgetWorld}), and there is a hard cap on the number of cached worlds.
     */
    private static final int MAX_CACHED_WORLDS = 32;

    /** One world's cache. The seed is kept so a world regenerated under the same name is not mixed up. */
    private static final class WorldCache {
        final long seed;
        final java.util.concurrent.ConcurrentHashMap<Long, String> chunks = new java.util.concurrent.ConcurrentHashMap<>();
        WorldCache(long seed) { this.seed = seed; }
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, WorldCache> BIOME_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Drops a world's cached biomes. Called on RemoveWorldEvent and WorldGenChunksClearedEvent. */
    public static void forgetWorld(String worldName) {
        if (worldName != null) BIOME_CACHE.remove(worldName);
    }

    /** Drops everything (plugin shutdown). */
    public static void forgetAll() {
        BIOME_CACHE.clear();
    }

    private static WorldCache cacheFor(World world) {
        String name = world.getName();
        long seed = world.getWorldConfig().getSeed();
        WorldCache wc = BIOME_CACHE.get(name);
        if (wc != null && wc.seed == seed) return wc;

        // New world, or same name with another seed: start a fresh cache for it.
        if (wc == null && BIOME_CACHE.size() >= MAX_CACHED_WORLDS) {
            BIOME_CACHE.clear(); // rare (more than 32 worlds at once): just rebuild what is used
        }
        WorldCache nuevo = new WorldCache(seed);
        BIOME_CACHE.put(name, nuevo);
        return nuevo;
    }

    /**
     * Get the biome name at the player's current position.
     * @param player The player entity
     * @param transform The player's TransformComponent (Update 5: obtained directly from the chunk;
     *                  EntityUtils.toHolder was removed)
     * @return Biome name, or null if unable to detect
     */
    @Nullable
    public static String getPlayerBiomeName(Player player, TransformComponent transform) {
        if (player == null || transform == null) return null;

        World world = player.getWorld();
        if (world == null) return null;

        Vector3d position = transform.getPosition();
        if (position == null) return null;

        return getBiomeNameAt(world, position);
    }

    /**
     * Get the biome name at a specific position in the world.
     * @param world The world
     * @param position The position to check
     * @return Biome name, or null if unable to detect
     */
    @Nullable
    public static String getBiomeNameAt(World world, Vector3d position) {
        if (world == null || position == null) return null;

        try {
            int x = (int) position.x();
            int z = (int) position.z();
            long chunkKey = (((long) (x >> 4)) << 32) | ((z >> 4) & 0xFFFFFFFFL);

            WorldCache wc = cacheFor(world);
            java.util.concurrent.ConcurrentHashMap<Long, String> worldCache = wc.chunks;
            String cached = worldCache.get(chunkKey);
            if (cached != null) return cached;

            IWorldGen worldGen = world.getChunkStore().getGenerator();
            if (!(worldGen instanceof ChunkGenerator)) return null;

            ChunkGenerator generator = (ChunkGenerator) worldGen;
            int seed = (int) wc.seed;

            ZoneBiomeResult result = generator.generateZoneBiomeResultAt(seed, x, z);
            if (result == null) return null;

            Biome biome = result.getBiome();
            if (biome == null) return null;

            if (worldCache.size() >= MAX_CACHED_CHUNKS_PER_WORLD) {
                worldCache.clear();
            }
            String name = biome.getName();
            worldCache.put(chunkKey, name);
            return name;
        } catch (Exception e) {
            // Silently fail - biome detection is optional
            return null;
        }
    }
}
