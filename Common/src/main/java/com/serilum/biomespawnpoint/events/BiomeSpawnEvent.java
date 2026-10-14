package com.serilum.biomespawnpoint.events;

import com.serilum.biomespawnpoint.config.ConfigHandler;
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.util.Util;
import com.natamus.collective.functions.BlockPosFunctions;
import com.natamus.collective.functions.FeatureFunctions;
import com.natamus.collective.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.Random;

public class BiomeSpawnEvent {
	public static boolean onWorldLoad(ServerLevel serverLevel, ServerLevelData serverLevelData) {
		BlockPos spawnPos = findSpawnPos(serverLevel, new Random(serverLevel.getSeed()), new BlockPos(0, 0, 0));
		if (spawnPos == null) {
			Constants.logger.info("[Biome Spawn Point] Unable to find custom spawn point.");
			return false;
		}

		Constants.logger.info("[Biome Spawn Point] The world will now generate.");

		setWorldSpawn(serverLevel, spawnPos);

		if (serverLevel.getServer().getWorldData().worldGenOptions().generateBonusChest()) {
			FeatureFunctions.placeBonusChest(serverLevel, spawnPos);
		}

		return true;
	}

	public static BlockPos findSpawnPos(ServerLevel serverLevel, Random random, BlockPos searchCenter) {
		BlockPos spawnPos = null;

		try {
			Registry<Biome> biomeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.BIOME);
			Util.loadSpawnBiomeConfig(biomeRegistry);

			if (Util.spawnBiomeListSize() == 0) {
				Constants.logger.info("[Biome Spawn Point] No spawn biome specified in the spawnbiomes.txt config.");
			}
			else {
				String spawnBiome = Util.getSpawnBiome(random);
				if (spawnBiome.strip().equals("")) {
					Constants.logger.info("[Biome Spawn Point] Received spawn point biome name is empty.");
				}
				else {
					Constants.logger.info("[Biome Spawn Point] Finding the nearest '" + spawnBiome + "' biome. This might take a few seconds.");
					spawnPos = BlockPosFunctions.getNearbyBiome(serverLevel, searchCenter, spawnBiome, ConfigHandler.biomeSearchRadius, ConfigHandler.minimumDistanceFromBiomeEdge, ConfigHandler.preferSpawningOnLand);
					if (spawnPos != null) {
						Constants.logger.info("[Biome Spawn Point] Biome found!");
					}
				}
			}
		}
		catch (Exception ex) {
			Constants.logger.info("[Biome Spawn Point] Unable to access Biome Registry on level load.");
		}

		WorldOptions generatorsettings = serverLevel.getServer().getWorldData().worldGenOptions();

		if (Services.MODLOADER.isModLoaded("villagespawnpoint") && generatorsettings.generateStructures()) {
			BlockPos villagePos;
			if (spawnPos == null) {
				Constants.logger.info("[Biome Spawn Point] Unable to find biome, but Village Spawn Point installed, finding village near x=" + searchCenter.getX() + ", z=" + searchCenter.getZ() + ".");
				villagePos = BlockPosFunctions.getNearbyVillage(serverLevel, searchCenter);
			}
			else {
				Constants.logger.info("[Biome Spawn Point] Village Spawn Point installed, finding a village in the biome. This might take a few seconds.");
				villagePos = BlockPosFunctions.getNearbyVillageInSameBiome(serverLevel, spawnPos);
			}

			if (villagePos != null) {
				Constants.logger.info("[Biome Spawn Point] Nearby village found.");
				spawnPos = villagePos.immutable();
			}
		}

		return spawnPos;
	}

	public static void setWorldSpawn(ServerLevel serverLevel, BlockPos spawnPos) {
		serverLevel.setDefaultSpawnPos(spawnPos, 1.0f);
	}
}
