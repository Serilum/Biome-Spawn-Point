package com.serilum.biomespawnpoint.util;

import com.natamus.collective.config.DuskConfig;
import com.natamus.collective.functions.DataFunctions;
import com.natamus.collective.functions.WorldFunctions;
import com.serilum.biomespawnpoint.config.ConfigHandler;
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.data.Variables;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class SpawnBiomeFiles {
	private static final String dirPath = DataFunctions.getConfigDirectory() + File.separator + "biomespawnpoint";
	private static final File oldSpawnBiomesFile = new File(dirPath + File.separator + "spawnbiomes.txt");
	private static final File movedSpawnBiomesFile = new File(dirPath + File.separator + "spawnbiomes_old.txt");

	public static void moveOldSpawnBiomesFile() throws IOException {
		if (!oldSpawnBiomesFile.isFile()) {
			return;
		}

		if (ConfigHandler.spawnBiomes.isBlank()) {
			List<String> enabledBiomes = readEnabledSpawnBiomes(oldSpawnBiomesFile);
			if (!enabledBiomes.isEmpty()) {
				ConfigHandler.spawnBiomes = String.join(",", enabledBiomes);
				DuskConfig.write(Reference.MOD_ID);
			}
		}

		Files.move(oldSpawnBiomesFile.toPath(), movedSpawnBiomesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		Constants.logger.info("[Biome Spawn Point] Moved the enabled biomes of spawnbiomes.txt into the 'spawnBiomes' config option, the file is now " + movedSpawnBiomesFile.getName() + ".");
	}

	public static String getSpawnBiomesForWorld(MinecraftServer server) {
		File worldSpawnBiomesFile = getWorldSpawnBiomesFile(server);
		if (!worldSpawnBiomesFile.isFile()) {
			return ConfigHandler.spawnBiomes;
		}

		try {
			return Files.readString(worldSpawnBiomesFile.toPath()).strip();
		}
		catch (IOException ex) {
			Constants.logger.warn("[Biome Spawn Point] Unable to read the world's spawn biomes: " + ex.getMessage());
			return ConfigHandler.spawnBiomes;
		}
	}

	public static String getWorldCreationSpawnBiomes() {
		if (!ConfigHandler.spawnBiomes.equals(Variables.spawnBiomesConfigWhenPicked)) {
			Variables.worldCreationSpawnBiomes = null;
		}
		return Variables.worldCreationSpawnBiomes;
	}

	public static void setWorldCreationSpawnBiomes(String spawnBiomes) {
		Variables.worldCreationSpawnBiomes = spawnBiomes;
		Variables.spawnBiomesConfigWhenPicked = ConfigHandler.spawnBiomes;
	}

	public static void saveWorldCreationSpawnBiomes(MinecraftServer server) {
		String worldCreationSpawnBiomes = getWorldCreationSpawnBiomes();
		if (worldCreationSpawnBiomes != null) {
			writeWorldSpawnBiomesFile(server, worldCreationSpawnBiomes);
		}
	}

	public static void saveSpawnBiomesForWorld(MinecraftServer server, String spawnBiomes) {
		if (getWorldSpawnBiomesFile(server).isFile()) {
			writeWorldSpawnBiomesFile(server, spawnBiomes);
			return;
		}

		ConfigHandler.spawnBiomes = spawnBiomes;
		DuskConfig.write(Reference.MOD_ID);
	}

	private static void writeWorldSpawnBiomesFile(MinecraftServer server, String spawnBiomes) {
		File worldSpawnBiomesFile = getWorldSpawnBiomesFile(server);
		boolean ignored = worldSpawnBiomesFile.getParentFile().mkdirs();

		try {
			PrintWriter writer = new PrintWriter(worldSpawnBiomesFile, StandardCharsets.UTF_8);
			writer.println(spawnBiomes);
			writer.close();
		}
		catch (IOException ex) {
			Constants.logger.warn("[Biome Spawn Point] Unable to save the world's spawn biomes: " + ex.getMessage());
		}
	}

	private static File getWorldSpawnBiomesFile(MinecraftServer server) {
		return new File(WorldFunctions.getWorldPath(server) + File.separator + "data" + File.separator + "biomespawnpoint" + File.separator + "spawnbiomes.txt");
	}

	private static List<String> readEnabledSpawnBiomes(File file) throws IOException {
		List<String> enabledBiomes = new ArrayList<>();
		for (String rawLine : Files.readAllLines(file.toPath())) {
			String line = rawLine.strip();
			if (line.startsWith("//")) {
				continue;
			}

			for (String rawEntry : line.split(",")) {
				String entry = rawEntry.strip();
				if (entry.contains(":") && !entry.startsWith("!") && !enabledBiomes.contains(entry)) {
					enabledBiomes.add(entry);
				}
			}
		}
		return enabledBiomes;
	}
}
