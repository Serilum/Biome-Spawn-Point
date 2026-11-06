package com.serilum.biomespawnpoint.events;

import com.natamus.collective.functions.WorldFunctions;
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.data.Variables;
import com.serilum.biomespawnpoint.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;

public class BiomeSpawnEvent {
	public static boolean onWorldLoad(ServerLevel serverLevel, ServerLevelData serverLevelData) {
		MinecraftServer server = serverLevel.getServer();
		String spawnBiome = Util.pickNewWorldSpawnBiome(serverLevel);
		if (!Util.isSpawnBiomeInOverworld(serverLevel.registryAccess(), spawnBiome)) {
			Constants.logger.info("[Biome Spawn Point] The '" + spawnBiome + "' biome is in another dimension, its spawn point will be set once the world has loaded.");
			Variables.pendingSpawnBiome = spawnBiome;
			Variables.pendingSpawnWorldPath = WorldFunctions.getWorldPath(server);
			return false;
		}

		BlockPos spawnPos = Util.findSpawnPos(serverLevel, spawnBiome, new BlockPos(0, 0, 0));
		if (spawnPos == null) {
			Constants.logger.info("[Biome Spawn Point] Unable to find custom spawn point.");
			return false;
		}

		Constants.logger.info("[Biome Spawn Point] The world will now generate.");

		Util.setWorldSpawn(serverLevel, spawnPos);
		Util.placeBonusChestIfEnabled(serverLevel, spawnPos);
		return true;
	}

	public static void onServerStarted(MinecraftServer server) {
		String spawnBiome = Variables.pendingSpawnBiome;
		if (spawnBiome == null) {
			return;
		}

		boolean isPendingForThisWorld = WorldFunctions.getWorldPath(server).equals(Variables.pendingSpawnWorldPath);
		Variables.pendingSpawnBiome = null;
		Variables.pendingSpawnWorldPath = null;
		if (!isPendingForThisWorld) {
			return;
		}

		ServerLevel spawnLevel = Util.getSpawnLevel(server, spawnBiome);
		BlockPos spawnPos = Util.findSpawnPos(spawnLevel, spawnBiome, new BlockPos(0, 0, 0));
		if (spawnPos == null) {
			Constants.logger.info("[Biome Spawn Point] Unable to find custom spawn point.");
			Util.placeBonusChestAtOverworldSpawnIfEnabled(server);
			return;
		}

		Constants.logger.info("[Biome Spawn Point] Set the world spawn in " + spawnLevel.dimension().identifier() + ".");

		Util.setWorldSpawn(spawnLevel, spawnPos);
		Util.placeBonusChestIfEnabled(spawnLevel, spawnPos);
	}
}
