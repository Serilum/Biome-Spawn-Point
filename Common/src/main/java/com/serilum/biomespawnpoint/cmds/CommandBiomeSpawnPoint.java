package com.serilum.biomespawnpoint.cmds;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.natamus.collective.data.GlobalVariables;
import com.natamus.collective.functions.MessageFunctions;
import com.serilum.biomespawnpoint.config.ConfigHandler;
import com.serilum.biomespawnpoint.util.Reference;
import com.serilum.biomespawnpoint.util.SpawnBiomeFiles;
import com.serilum.biomespawnpoint.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class CommandBiomeSpawnPoint {
	private static final DynamicCommandExceptionType invalidBiomeException = new DynamicCommandExceptionType(biome -> Component.translatable("collective.biomespawnpoint.message.unknownbiome", biome));

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(getCommand("biomespawnpoint"));
		dispatcher.register(getCommand("bsp"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> getCommand(String commandName) {
		return Commands.literal(commandName).requires((iCommandSender) -> iCommandSender.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
			.executes((command) -> {
				showUsage(command.getSource(), commandName);
				return 1;
			})
			.then(Commands.literal("usage")
			.executes((command) -> {
				showUsage(command.getSource(), commandName);
				return 1;
			}))
			.then(Commands.literal("enable")
			.then(Commands.argument("biome", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BIOME))
			.suggests(CommandBiomeSpawnPoint::suggestBiomesToEnable)
			.executes((command) -> setBiomeEnabled(command, true))))
			.then(Commands.literal("disable")
			.then(Commands.argument("biome", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BIOME))
			.suggests(CommandBiomeSpawnPoint::suggestEnabledBiomes)
			.executes((command) -> setBiomeEnabled(command, false))))
			.then(Commands.literal("repick")
			.executes((command) -> {
				int searchRadius = ConfigHandler.biomeSearchRadius;
				int searchCenterX = GlobalVariables.random.nextInt(searchRadius * 2 + 1) - searchRadius;
				int searchCenterZ = GlobalVariables.random.nextInt(searchRadius * 2 + 1) - searchRadius;

				return moveWorldSpawn(command.getSource(), GlobalVariables.random, new BlockPos(searchCenterX, 0, searchCenterZ));
			}))
			.then(Commands.literal("reset")
			.executes((command) -> {
				ServerLevel serverLevel = command.getSource().getServer().overworld();
				return moveWorldSpawn(command.getSource(), new Random(serverLevel.getSeed()), new BlockPos(0, 0, 0));
			}));
	}

	private static int setBiomeEnabled(CommandContext<CommandSourceStack> command, boolean enable) throws CommandSyntaxException {
		CommandSourceStack source = command.getSource();
		MinecraftServer server = source.getServer();
		ResourceOrTagKeyArgument.Result<Biome> biome = ResourceOrTagKeyArgument.getResourceOrTagKey(command, "biome", Registries.BIOME, invalidBiomeException);
		String biomeName = biome.asPrintable();

		Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);
		if (!existsInWorld(biomeRegistry, biome)) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.unknownbiome", true, ChatFormatting.RED, biomeName);
			return 0;
		}

		String spawnBiomes = SpawnBiomeFiles.getSpawnBiomesForWorld(server);
		boolean isEnabled = Util.getEnabledSpawnBiomes(spawnBiomes).contains(biomeName);
		if (isEnabled && enable) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.alreadyenabled", true, ChatFormatting.YELLOW, biomeName);
			return 0;
		}
		if (!isEnabled && !enable) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.alreadydisabled", true, ChatFormatting.YELLOW, biomeName);
			return 0;
		}

		String newSpawnBiomes = Util.setSpawnBiomeEnabled(spawnBiomes, biomeName, enable);
		SpawnBiomeFiles.saveSpawnBiomesForWorld(server, newSpawnBiomes);

		if (enable) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.enabledbiome", true, ChatFormatting.DARK_GREEN, biomeName);
		}
		else {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.disabledbiome", true, ChatFormatting.DARK_GREEN, biomeName);
		}

		showSpawnBiomes(source, newSpawnBiomes);
		return 1;
	}

	private static List<String> getEnabledSpawnBiomesForWorld(CommandContext<CommandSourceStack> command) {
		String spawnBiomes = SpawnBiomeFiles.getSpawnBiomesForWorld(command.getSource().getServer());
		return Util.getEnabledSpawnBiomes(spawnBiomes);
	}

	private static CompletableFuture<Suggestions> suggestBiomesToEnable(CommandContext<CommandSourceStack> command, SuggestionsBuilder builder) {
		Registry<Biome> biomeRegistry = command.getSource().getServer().registryAccess().lookupOrThrow(Registries.BIOME);
		List<String> enabledBiomes = getEnabledSpawnBiomesForWorld(command);

		List<Identifier> biomesToEnable = new ArrayList<>();
		for (Identifier biomeIdentifier : biomeRegistry.keySet()) {
			if (!enabledBiomes.contains(biomeIdentifier.toString())) {
				biomesToEnable.add(biomeIdentifier);
			}
		}

		return SharedSuggestionProvider.suggestResource(biomesToEnable, builder);
	}

	private static CompletableFuture<Suggestions> suggestEnabledBiomes(CommandContext<CommandSourceStack> command, SuggestionsBuilder builder) {
		List<Identifier> enabledBiomeIdentifiers = new ArrayList<>();
		List<Identifier> enabledTagIdentifiers = new ArrayList<>();
		for (String enabledBiome : getEnabledSpawnBiomesForWorld(command)) {
			Identifier identifier = Identifier.tryParse(enabledBiome.replace("#", ""));
			if (identifier == null) {
				continue;
			}

			if (enabledBiome.startsWith("#")) {
				enabledTagIdentifiers.add(identifier);
			}
			else {
				enabledBiomeIdentifiers.add(identifier);
			}
		}

		SharedSuggestionProvider.suggestResource(enabledTagIdentifiers, builder, "#");
		return SharedSuggestionProvider.suggestResource(enabledBiomeIdentifiers, builder);
	}

	private static boolean existsInWorld(Registry<Biome> biomeRegistry, ResourceOrTagKeyArgument.Result<Biome> biome) {
		Optional<ResourceKey<Biome>> optionalBiomeKey = biome.unwrap().left();
		if (optionalBiomeKey.isPresent()) {
			return biomeRegistry.containsKey(optionalBiomeKey.get());
		}

		Optional<TagKey<Biome>> optionalBiomeTag = biome.unwrap().right();
		return optionalBiomeTag.isPresent() && biomeRegistry.get(optionalBiomeTag.get()).isPresent();
	}

	private static void showSpawnBiomes(CommandSourceStack source, String spawnBiomes) {
		List<String> spawnBiomeEntries = Util.getSpawnBiomeEntries(spawnBiomes);
		if (spawnBiomeEntries.isEmpty()) {
			MessageFunctions.sendMessage(source, " N/A", ChatFormatting.RED);
			return;
		}

		for (String spawnBiomeEntry : spawnBiomeEntries) {
			MessageFunctions.sendMessage(source, " " + spawnBiomeEntry, ChatFormatting.GRAY);
		}
	}

	private static int moveWorldSpawn(CommandSourceStack source, Random random, BlockPos searchCenter) {
		MinecraftServer server = source.getServer();
		String spawnBiome = Util.pickSpawnBiome(server.registryAccess(), SpawnBiomeFiles.getSpawnBiomesForWorld(server), random);
		ServerLevel serverLevel = Util.getSpawnLevel(server, spawnBiome);

		BlockPos spawnPos = Util.findSpawnPos(serverLevel, spawnBiome, searchCenter);
		if (spawnPos == null) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.nospawnfound", true, ChatFormatting.RED);
			return 0;
		}

		Util.setWorldSpawn(serverLevel, spawnPos);

		MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.movedspawn", true, ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, " ", "collective.biomespawnpoint.message.spawnposition", ChatFormatting.GRAY, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
		MessageFunctions.sendTranslatableMessage(source, " ", "collective.biomespawnpoint.message.spawnbiome", ChatFormatting.GRAY, getBiomeName(serverLevel, spawnPos));
		return 1;
	}

	private static void showUsage(CommandSourceStack source, String commandName) {
		MessageFunctions.sendTranslatableMessage(source, "collective.shared.message.usage", true, ChatFormatting.DARK_GREEN, Reference.NAME);
		MessageFunctions.sendMessage(source, " /" + commandName + " usage", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.shared.message.showmessage", ChatFormatting.GRAY);
		MessageFunctions.sendMessage(source, " /" + commandName + " enable <biome>", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.biomespawnpoint.message.enablesbiome", ChatFormatting.GRAY);
		MessageFunctions.sendMessage(source, " /" + commandName + " disable <biome>", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.biomespawnpoint.message.disablesbiome", ChatFormatting.GRAY);
		MessageFunctions.sendMessage(source, " /" + commandName + " repick", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.biomespawnpoint.message.repicksspawn", ChatFormatting.GRAY);
		MessageFunctions.sendMessage(source, " /" + commandName + " reset", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.biomespawnpoint.message.resetsspawn", ChatFormatting.GRAY);
	}

	private static String getBiomeName(ServerLevel serverLevel, BlockPos pos) {
		Optional<ResourceKey<Biome>> optionalBiomeKey = serverLevel.getBiome(pos).unwrapKey();
		if (!optionalBiomeKey.isPresent()) {
			return "?";
		}

		return optionalBiomeKey.get().identifier().toString();
	}
}
