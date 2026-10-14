package com.serilum.biomespawnpoint;


import com.serilum.biomespawnpoint.config.ConfigHandler;
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.util.Util;

public class ModCommon {

	public static void init() {
		ConfigHandler.initConfig();
		load();
	}

	private static void load() {
		try {
			Util.loadSpawnBiomeConfig(null);
		} catch (Exception ex) {
			Constants.logger.warn("[Biome Spawn Point] Error: Unable to generate spawn biome list.");
		}
	}
}