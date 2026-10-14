package com.serilum.biomespawnpoint.config;

import com.natamus.collective.config.DuskConfig;
import com.serilum.biomespawnpoint.util.Reference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigHandler extends DuskConfig {
	public static HashMap<String, List<String>> configMetaData = new HashMap<>();

	@Entry(min = 256, max = 30000) public static int biomeSearchRadius = 6400;
	@Entry(min = 0, max = 128) public static int minimumDistanceFromBiomeEdge = 16;
	@Entry public static boolean preferSpawningOnLand = true;

	public static void initConfig() {
		configMetaData.put("biomeSearchRadius", Arrays.asList(
			"How far from the center of the world (x=0, z=0), in blocks, the mod searches for one of the spawn biomes. A larger radius finds rarer biomes, but makes creating a new world take longer."
		));
		configMetaData.put("minimumDistanceFromBiomeEdge", Arrays.asList(
			"How far inside the biome, in blocks, the spawn point should be, so players don't spawn just outside of it. When no spot that far inside the biome is found, a spot closer to the edge is used."
		));
		configMetaData.put("preferSpawningOnLand", Arrays.asList(
			"Whether the spawn point should be on land when the chosen biome also exists under water, like beaches that continue into the sea. A spot in water is only used when the biome has no land nearby."
		));

		DuskConfig.init(Reference.NAME, Reference.MOD_ID, ConfigHandler.class);
	}
}
