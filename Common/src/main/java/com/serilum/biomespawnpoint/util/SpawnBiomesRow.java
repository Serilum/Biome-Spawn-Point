package com.serilum.biomespawnpoint.util;

import com.natamus.collective.config.screen.DuskConfigRegistryIds;
import com.natamus.collective.functions.ConfigFunctions;
import com.natamus.collective.objects.IdAmount;
import com.natamus.collective.worldcreation.WorldCreationRows;
import com.natamus.collective.worldcreation.WorldCreationTab;
import com.serilum.biomespawnpoint.config.ConfigHandler;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SpawnBiomesRow {
	public static void register() {
		Component label = Component.translatable("collective.biomespawnpoint.gui.spawninbiomes");
		Component description = Component.translatable("collective.biomespawnpoint.gui.spawninbiomesdescription");
		WorldCreationRows.addButtonRow(WorldCreationTab.WORLD, label, description, SpawnBiomesRow::getCountText, SpawnBiomesRow::getTooltip, SpawnBiomesRow::openPicker);
	}

	private static String getShownSpawnBiomes() {
		String worldCreationSpawnBiomes = SpawnBiomeFiles.getWorldCreationSpawnBiomes();
		if (worldCreationSpawnBiomes == null) {
			return ConfigHandler.spawnBiomes;
		}
		return worldCreationSpawnBiomes;
	}

	private static Component getCountText() {
		return Component.literal(String.valueOf(Util.getSpawnBiomeEntries(getShownSpawnBiomes()).size()));
	}

	private static Component getTooltip() {
		List<String> spawnBiomeLines = new ArrayList<>();
		for (IdAmount spawnBiome : ConfigFunctions.getIdAmounts(getShownSpawnBiomes())) {
			spawnBiomeLines.add(getBiomeName(spawnBiome.id) + ": " + spawnBiome.maximum);
		}

		if (spawnBiomeLines.isEmpty()) {
			return Component.translatable("gui.none");
		}
		return Component.literal(String.join("\n", spawnBiomeLines));
	}

	private static String getBiomeName(String biomeId) {
		String biomeName = DuskConfigRegistryIds.getName("worldgen/biome", biomeId);
		if (biomeName.isEmpty()) {
			return biomeId;
		}
		return biomeName;
	}

	private static void openPicker(CreateWorldScreen screen) {
		String title = Component.translatable("collective.biomespawnpoint.gui.spawnbiomes").getString();
		WorldCreationRows.openRegistryPicker(screen, title, "worldgen/biome", true, getShownSpawnBiomes(), SpawnBiomeFiles::setWorldCreationSpawnBiomes);
	}
}
