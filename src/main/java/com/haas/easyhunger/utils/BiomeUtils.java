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
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<Long, String>> BIOME_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

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

            java.util.concurrent.ConcurrentHashMap<Long, String> worldCache =
                    BIOME_CACHE.computeIfAbsent(world.getName(), k -> new java.util.concurrent.ConcurrentHashMap<>());
            String cached = worldCache.get(chunkKey);
            if (cached != null) return cached;

            IWorldGen worldGen = world.getChunkStore().getGenerator();
            if (!(worldGen instanceof ChunkGenerator)) return null;

            ChunkGenerator generator = (ChunkGenerator) worldGen;
            int seed = (int) world.getWorldConfig().getSeed();

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
