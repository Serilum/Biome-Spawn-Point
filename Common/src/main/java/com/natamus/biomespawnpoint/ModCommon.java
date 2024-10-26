package com.natamus.biomespawnpoint;


import com.natamus.biomespawnpoint.data.Constants;
import com.natamus.biomespawnpoint.util.Util;

public class ModCommon {

	public static void init() {
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