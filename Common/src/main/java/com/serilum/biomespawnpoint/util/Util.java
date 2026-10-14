package com.serilum.biomespawnpoint.util;

import com.natamus.collective.functions.DataFunctions;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Util {
	private static final List<String> spawnBiomes = new ArrayList<String>();

	private static final String dirpath = DataFunctions.getConfigDirectory() + File.separator + "biomespawnpoint";
	private static final File dir = new File(dirpath);
	private static final File file = new File(dirpath + File.separator + "spawnbiomes.txt");

	public static void loadSpawnBiomeConfig(Registry<Biome> biomeRegistry) throws Exception {
		spawnBiomes.clear();

		List<String> listedBiomes = new ArrayList<>();
		String configContent = "";

		PrintWriter writer = null;
		if (!file.isFile()) {
			boolean ignored = dir.mkdirs();

			writer = new PrintWriter(dirpath + File.separator + "spawnbiomes.txt", StandardCharsets.UTF_8);
			writer.println("// To generate missing and modded biomes in this list; create a new world once,");
			writer.println("// Any biome listed below without an ! at the start will be randomly chosen to spawn in,");
		}
		else {
			configContent = new String(Files.readAllBytes(Paths.get(dirpath + File.separator + "spawnbiomes.txt")));
			for (String rawLine : configContent.split("\n")) {
				String line = rawLine.strip();
				if (line.startsWith("//")) {
					continue;
				}

				for (String rawEntry : line.split(",")) {
					String entry = rawEntry.strip();
					if (!entry.contains(":")) {
						continue;
					}

					String biomeName = entry.replace("!", "").strip();
					if (!entry.startsWith("!") && !spawnBiomes.contains(biomeName)) {
						spawnBiomes.add(biomeName);
					}

					listedBiomes.add(biomeName);
				}
			}
		}

		List<String> biomeNamesToAdd = new ArrayList<>();
		if (biomeRegistry != null) {
			for (Biome biome : biomeRegistry) {
				Identifier biomeIdentifier = biomeRegistry.getKey(biome);
				if (biomeIdentifier == null) {
					continue;
				}

				String biomeName = biomeIdentifier.toString();
				if (!listedBiomes.contains(biomeName)) {
					biomeNamesToAdd.add(biomeName);
				}
			}
		}

		if (biomeNamesToAdd.size() > 0) {
			if (writer == null) {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(dirpath + File.separator + "spawnbiomes.txt", true)));
				if (!configContent.isEmpty() && !configContent.endsWith("\n")) {
					writer.println();
				}
			}

			for (String biomeName : biomeNamesToAdd) {
				writer.println("!" + biomeName + ",");
			}
		}

		if (writer != null) {
			writer.close();
		}
	}

	public static int spawnBiomeListSize() {
		return spawnBiomes.size();
	}

	public static List<String> getSpawnBiomes() {
		return new ArrayList<String>(spawnBiomes);
	}

	public static String getSpawnBiome(Random random) {
		return getSpawnBiomes().get(random.nextInt(spawnBiomeListSize()));
	}

	public static void setSpawnBiomeEnabled(String biomeName, boolean enabled) throws IOException {
		String newEntry = biomeName;
		if (!enabled) {
			newEntry = "!" + biomeName;
		}

		Path configPath = Paths.get(dirpath + File.separator + "spawnbiomes.txt");
		List<String> lines = Files.readAllLines(configPath);
		boolean foundEntry = false;

		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			String line = lines.get(lineIndex);
			if (line.strip().startsWith("//")) {
				continue;
			}

			String[] entries = line.split(",", -1);
			boolean lineChanged = false;
			for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
				if (entries[entryIndex].replace("!", "").strip().equals(biomeName)) {
					entries[entryIndex] = newEntry;
					lineChanged = true;
				}
			}

			if (lineChanged) {
				lines.set(lineIndex, String.join(",", entries));
				foundEntry = true;
			}
		}

		if (!foundEntry) {
			lines.add(newEntry + ",");
		}

		Files.write(configPath, lines);
	}
}
