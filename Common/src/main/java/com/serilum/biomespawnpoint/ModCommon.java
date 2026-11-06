package com.serilum.biomespawnpoint;


import com.natamus.collective.services.Services;
import com.serilum.biomespawnpoint.config.ConfigHandler;
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.util.SpawnBiomeFiles;
import com.serilum.biomespawnpoint.util.SpawnBiomesRow;

public class ModCommon {

	public static void init() {
		ConfigHandler.initConfig();
		load();
	}

	private static void load() {
		try {
			SpawnBiomeFiles.moveOldSpawnBiomesFile();
		} catch (Exception ex) {
			Constants.logger.warn("[Biome Spawn Point] Error: Unable to move spawnbiomes.txt into the config.");
		}

		if (Services.MODLOADER.isClientSide()) {
			SpawnBiomesRow.register();
		}
	}
}