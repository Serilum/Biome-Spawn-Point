package com.serilum.biomespawnpoint.util;

import com.natamus.collective.config.DuskConfigModToggles;
import com.natamus.collective.functions.BlockPosFunctions;
import com.natamus.collective.functions.ConfigFunctions;
import com.natamus.collective.functions.FeatureFunctions;
import com.natamus.collective.functions.WorldFunctions;
import com.natamus.collective.objects.IdAmount;
import com.natamus.collective.services.Services;
import com.serilum.biomespawnpoint.config.ConfigHandler;
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.data.Variables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Util {
	public static List<String> getEnabledSpawnBiomes(String spawnBiomes) {
		List<String> enabledSpawnBiomes = new ArrayList<>();
		for (IdAmount spawnBiome : ConfigFunctions.getIdAmounts(spawnBiomes)) {
			if (spawnBiome.maximum > 0) {
				enabledSpawnBiomes.add(spawnBiome.id);
			}
		}
		return enabledSpawnBiomes;
	}

	public static List<String> getSpawnBiomeEntries(String spawnBiomes) {
		List<String> spawnBiomeEntries = new ArrayList<>();
		for (String rawEntry : spawnBiomes.split(",")) {
			String entry = rawEntry.strip();
			if (!entry.isEmpty()) {
				spawnBiomeEntries.add(entry);
			}
		}
		return spawnBiomeEntries;
	}

	public static String setSpawnBiomeEnabled(String spawnBiomes, String biomeName, boolean enabled) {
		List<String> spawnBiomeEntries = new ArrayList<>();
		for (String entry : getSpawnBiomeEntries(spawnBiomes)) {
			if (!IdAmount.getId(entry).equals(biomeName)) {
				spawnBiomeEntries.add(entry);
			}
		}

		if (enabled) {
			spawnBiomeEntries.add(biomeName);
		}
		return String.join(",", spawnBiomeEntries);
	}

	public static String pickSpawnBiome(RegistryAccess registryAccess, String spawnBiomes, Random random) {
		List<IdAmount> pickableBiomes = new ArrayList<>();
		int totalWeight = 0;

		for (IdAmount spawnBiome : ConfigFunctions.getIdAmounts(spawnBiomes)) {
			if (spawnBiome.maximum <= 0) {
				continue;
			}

			if (getSpawnDimension(registryAccess, spawnBiome.id) == null) {
				Constants.logger.info("[Biome Spawn Point] Skipping '" + spawnBiome.id + "', it does not generate in any dimension.");
				continue;
			}

			pickableBiomes.add(spawnBiome);
			totalWeight += spawnBiome.maximum;
		}

		if (totalWeight <= 0) {
			Constants.logger.info("[Biome Spawn Point] No spawn biome set for this world.");
			return "";
		}

		int roll = random.nextInt(totalWeight);
		for (IdAmount pickableBiome : pickableBiomes) {
			roll -= pickableBiome.maximum;
			if (roll < 0) {
				return pickableBiome.id;
			}
		}
		return "";
	}

	public static String pickNewWorldSpawnBiome(ServerLevel serverLevel) {
		MinecraftServer server = serverLevel.getServer();
		SpawnBiomeFiles.saveWorldCreationSpawnBiomes(server);

		String spawnBiomes = SpawnBiomeFiles.getSpawnBiomesForWorld(server);
		return pickSpawnBiome(serverLevel.registryAccess(), spawnBiomes, new Random(serverLevel.getSeed()));
	}

	public static boolean isNewWorldSpawnInOverworld(ServerLevel serverLevel) {
		return isSpawnBiomeInOverworld(serverLevel.registryAccess(), pickNewWorldSpawnBiome(serverLevel));
	}

	public static ResourceKey<LevelStem> getSpawnDimension(RegistryAccess registryAccess, String spawnBiome) {
		Registry<LevelStem> dimensionRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
		for (ResourceKey<LevelStem> dimensionKey : getDimensionKeysOverworldFirst(dimensionRegistry)) {
			LevelStem levelStem = dimensionRegistry.get(dimensionKey);
			if (levelStem != null && canGenerateBiome(levelStem, spawnBiome)) {
				return dimensionKey;
			}
		}
		return null;
	}

	private static List<ResourceKey<LevelStem>> getDimensionKeysOverworldFirst(Registry<LevelStem> dimensionRegistry) {
		List<ResourceKey<LevelStem>> dimensionKeys = new ArrayList<>();
		dimensionKeys.add(LevelStem.OVERWORLD);

		for (ResourceKey<LevelStem> dimensionKey : dimensionRegistry.registryKeySet()) {
			if (!dimensionKey.equals(LevelStem.OVERWORLD)) {
				dimensionKeys.add(dimensionKey);
			}
		}
		return dimensionKeys;
	}

	private static boolean canGenerateBiome(LevelStem levelStem, String spawnBiome) {
		for (Holder<Biome> possibleBiome : levelStem.generator().getBiomeSource().possibleBiomes()) {
			if (isSpawnBiome(possibleBiome, spawnBiome)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSpawnBiome(Holder<Biome> biome, String spawnBiome) {
		if (spawnBiome.startsWith("#")) {
			ResourceLocation tagLocation = ResourceLocation.tryParse(spawnBiome.substring(1));
			return tagLocation != null && biome.is(TagKey.create(Registries.BIOME, tagLocation));
		}

		ResourceLocation biomeLocation = ResourceLocation.tryParse(spawnBiome);
		return biomeLocation != null && biome.is(biomeLocation);
	}

	public static ServerLevel getSpawnLevel(MinecraftServer server, String spawnBiome) {
		ResourceKey<LevelStem> spawnDimension = getSpawnDimension(server.registryAccess(), spawnBiome);
		if (spawnDimension == null) {
			return server.overworld();
		}

		ServerLevel spawnLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, spawnDimension.location()));
		if (spawnLevel == null) {
			return server.overworld();
		}
		return spawnLevel;
	}

	public static boolean isSpawnBiomeInOverworld(RegistryAccess registryAccess, String spawnBiome) {
		ResourceKey<LevelStem> spawnDimension = getSpawnDimension(registryAccess, spawnBiome);
		return spawnDimension == null || spawnDimension.equals(LevelStem.OVERWORLD);
	}

	public static BlockPos findSpawnPos(ServerLevel serverLevel, String spawnBiome, BlockPos searchCenter) {
		BlockPos spawnPos = null;

		if (!spawnBiome.isBlank()) {
			Constants.logger.info("[Biome Spawn Point] Finding the nearest '" + spawnBiome + "' biome. This might take a few seconds.");
			spawnPos = BlockPosFunctions.getNearbyBiome(serverLevel, searchCenter, spawnBiome, ConfigHandler.biomeSearchRadius, ConfigHandler.minimumDistanceFromBiomeEdge, ConfigHandler.preferSpawningOnLand);
			if (spawnPos != null) {
				Constants.logger.info("[Biome Spawn Point] Biome found!");
			}
		}

		WorldOptions worldOptions = serverLevel.getServer().getWorldData().worldGenOptions();

		if (isVillageSpawnPointEnabled() && worldOptions.generateStructures() && serverLevel.dimension().equals(Level.OVERWORLD)) {
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
		if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
			setDimensionSpawn(serverLevel, spawnPos);
			return;
		}

		serverLevel.setDefaultSpawnPos(spawnPos, 1.0f);
		clearDimensionSpawn(serverLevel.getServer());
	}

	public static void placeBonusChestIfEnabled(ServerLevel serverLevel, BlockPos spawnPos) {
		if (serverLevel.getServer().getWorldData().worldGenOptions().generateBonusChest()) {
			FeatureFunctions.placeBonusChest(serverLevel, spawnPos);
		}
	}

	public static void placeBonusChestAtOverworldSpawnIfEnabled(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		placeBonusChestIfEnabled(overworld, overworld.getSharedSpawnPos());
	}

	public static boolean isVillageSpawnPointEnabled() {
		return Services.MODLOADER.isModLoaded("villagespawnpoint") && !DuskConfigModToggles.isDisabledSerilumMod("villagespawnpoint");
	}

	private static File getDimensionSpawnFile(MinecraftServer server) {
		return new File(WorldFunctions.getWorldPath(server) + File.separator + "data" + File.separator + "biomespawnpoint" + File.separator + "dimensionspawn.txt");
	}

	public static void loadDimensionSpawn(MinecraftServer server) {
		Variables.spawnDimension = null;
		Variables.spawnDimensionPos = null;

		File file = getDimensionSpawnFile(server);
		if (!file.isFile()) {
			return;
		}

		try {
			String[] values = Files.readString(file.toPath()).strip().split(",");
			ResourceLocation dimensionLocation = ResourceLocation.tryParse(values[0]);
			if (values.length != 4 || dimensionLocation == null) {
				return;
			}

			Variables.spawnDimension = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
			Variables.spawnDimensionPos = new BlockPos(Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3]));
		}
		catch (Exception ex) {
			Constants.logger.warn("[Biome Spawn Point] Unable to read " + file.getName() + ": " + ex.getMessage());
		}
	}

	public static void setDimensionSpawn(ServerLevel serverLevel, BlockPos spawnPos) {
		Variables.spawnDimension = serverLevel.dimension();
		Variables.spawnDimensionPos = spawnPos;

		File file = getDimensionSpawnFile(serverLevel.getServer());
		boolean ignored = file.getParentFile().mkdirs();

		try {
			PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8);
			writer.println(serverLevel.dimension().location() + "," + spawnPos.getX() + "," + spawnPos.getY() + "," + spawnPos.getZ());
			writer.close();
		}
		catch (IOException ex) {
			Constants.logger.warn("[Biome Spawn Point] Unable to save " + file.getName() + ": " + ex.getMessage());
		}
	}

	public static void clearDimensionSpawn(MinecraftServer server) {
		Variables.spawnDimension = null;
		Variables.spawnDimensionPos = null;

		try {
			Files.deleteIfExists(getDimensionSpawnFile(server).toPath());
		}
		catch (IOException ignored) { }
	}

	public static ServerLevel getDimensionSpawnLevel(MinecraftServer server) {
		if (Variables.spawnDimension == null) {
			return null;
		}
		return server.getLevel(Variables.spawnDimension);
	}

	public static void teleportToDimensionSpawn(ServerPlayer serverPlayer, ServerLevel spawnLevel) {
		BlockPos spawnPos = Variables.spawnDimensionPos;
		serverPlayer.teleportTo(spawnLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, serverPlayer.getYRot(), serverPlayer.getXRot());
	}

	public static DimensionTransition getDimensionSpawnTransition(ServerPlayer serverPlayer, DimensionTransition vanillaTransition, DimensionTransition.PostDimensionTransition postDimensionTransition) {
		boolean usesWorldSpawn = serverPlayer.getRespawnPosition() == null || vanillaTransition.missingRespawnBlock();
		if (!usesWorldSpawn) {
			return null;
		}

		ServerLevel spawnLevel = getDimensionSpawnLevel(serverPlayer.server);
		if (spawnLevel == null) {
			return null;
		}

		Vec3 spawnPos = Vec3.atBottomCenterOf(Variables.spawnDimensionPos);
		return new DimensionTransition(spawnLevel, spawnPos, Vec3.ZERO, vanillaTransition.yRot(), 0.0F, vanillaTransition.missingRespawnBlock(), postDimensionTransition);
	}
}
