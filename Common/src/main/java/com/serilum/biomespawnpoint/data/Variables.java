package com.serilum.biomespawnpoint.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class Variables {
	public static String pendingSpawnBiome = null;
	public static String pendingSpawnWorldPath = null;
	public static String worldCreationSpawnBiomes = null;
	public static String spawnBiomesConfigWhenPicked = null;
	public static ResourceKey<Level> spawnDimension = null;
	public static BlockPos spawnDimensionPos = null;
	public static boolean respawningInDimensionSpawn = false;
}
