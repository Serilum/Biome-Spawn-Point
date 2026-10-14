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
import com.serilum.biomespawnpoint.data.Constants;
import com.serilum.biomespawnpoint.events.BiomeSpawnEvent;
import com.serilum.biomespawnpoint.util.Reference;
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
			.then(Commands.literal("reload")
			.executes((command) -> {
				CommandSourceStack source = command.getSource();

				Registry<Biome> biomeRegistry = source.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
				try {
					Util.loadSpawnBiomeConfig(biomeRegistry);
				}
				catch (Exception ex) {
					MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.somethingwentwrong", true, ChatFormatting.RED);
					Constants.logger.warn("[Biome Spawn Point] Unable to reload spawnbiomes.txt: " + ex.getMessage());
					return 0;
				}

				MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.reloaded", true, ChatFormatting.DARK_GREEN);
				showSpawnBiomes(source);
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
		ResourceOrTagKeyArgument.Result<Biome> biome = ResourceOrTagKeyArgument.getResourceOrTagKey(command, "biome", Registries.BIOME, invalidBiomeException);
		String biomeName = biome.asPrintable();

		Registry<Biome> biomeRegistry = source.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
		if (!existsInWorld(biomeRegistry, biome)) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.unknownbiome", true, ChatFormatting.RED, biomeName);
			return 0;
		}

		try {
			Util.loadSpawnBiomeConfig(biomeRegistry);

			boolean isEnabled = Util.getSpawnBiomes().contains(biomeName);
			if (isEnabled && enable) {
				MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.alreadyenabled", true, ChatFormatting.YELLOW, biomeName);
				return 0;
			}
			if (!isEnabled && !enable) {
				MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.alreadydisabled", true, ChatFormatting.YELLOW, biomeName);
				return 0;
			}

			Util.setSpawnBiomeEnabled(biomeName, enable);
			Util.loadSpawnBiomeConfig(biomeRegistry);
		}
		catch (Exception ex) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.somethingwentwrongediting", true, ChatFormatting.RED);
			Constants.logger.warn("[Biome Spawn Point] Unable to edit spawnbiomes.txt: " + ex.getMessage());
			return 0;
		}

		if (enable) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.enabledbiome", true, ChatFormatting.DARK_GREEN, biomeName);
		}
		else {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.disabledbiome", true, ChatFormatting.DARK_GREEN, biomeName);
		}

		showSpawnBiomes(source);
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestBiomesToEnable(CommandContext<CommandSourceStack> command, SuggestionsBuilder builder) {
		Registry<Biome> biomeRegistry = command.getSource().getServer().registryAccess().lookupOrThrow(Registries.BIOME);
		List<String> enabledBiomes = getEnabledBiomes(biomeRegistry);

		List<Identifier> biomesToEnable = new ArrayList<>();
		for (Identifier biomeIdentifier : biomeRegistry.keySet()) {
			if (!enabledBiomes.contains(biomeIdentifier.toString())) {
				biomesToEnable.add(biomeIdentifier);
			}
		}

		return SharedSuggestionProvider.suggestResource(biomesToEnable, builder);
	}

	private static CompletableFuture<Suggestions> suggestEnabledBiomes(CommandContext<CommandSourceStack> command, SuggestionsBuilder builder) {
		Registry<Biome> biomeRegistry = command.getSource().getServer().registryAccess().lookupOrThrow(Registries.BIOME);

		List<Identifier> enabledBiomeIdentifiers = new ArrayList<>();
		List<Identifier> enabledTagIdentifiers = new ArrayList<>();
		for (String enabledBiome : getEnabledBiomes(biomeRegistry)) {
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

	private static List<String> getEnabledBiomes(Registry<Biome> biomeRegistry) {
		try {
			Util.loadSpawnBiomeConfig(biomeRegistry);
		}
		catch (Exception ignored) { }

		return Util.getSpawnBiomes();
	}

	private static boolean existsInWorld(Registry<Biome> biomeRegistry, ResourceOrTagKeyArgument.Result<Biome> biome) {
		Optional<ResourceKey<Biome>> optionalBiomeKey = biome.unwrap().left();
		if (optionalBiomeKey.isPresent()) {
			return biomeRegistry.containsKey(optionalBiomeKey.get());
		}

		Optional<TagKey<Biome>> optionalBiomeTag = biome.unwrap().right();
		return optionalBiomeTag.isPresent() && biomeRegistry.get(optionalBiomeTag.get()).isPresent();
	}

	private static void showSpawnBiomes(CommandSourceStack source) {
		List<String> spawnBiomes = Util.getSpawnBiomes();
		if (spawnBiomes.isEmpty()) {
			MessageFunctions.sendMessage(source, " N/A", ChatFormatting.RED);
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.enablebiomehint", ChatFormatting.GRAY);
			return;
		}

		for (String spawnBiome : spawnBiomes) {
			MessageFunctions.sendMessage(source, " " + spawnBiome, ChatFormatting.GRAY);
		}
	}

	private static int moveWorldSpawn(CommandSourceStack source, Random random, BlockPos searchCenter) {
		ServerLevel serverLevel = source.getServer().overworld();

		BlockPos spawnPos = BiomeSpawnEvent.findSpawnPos(serverLevel, random, searchCenter);
		if (spawnPos == null) {
			MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.nospawnfound", true, ChatFormatting.RED);
			return 0;
		}

		BiomeSpawnEvent.setWorldSpawn(serverLevel, spawnPos);

		MessageFunctions.sendTranslatableMessage(source, "collective.biomespawnpoint.message.movedspawn", true, ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, " ", "collective.biomespawnpoint.message.spawnposition", ChatFormatting.GRAY, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
		MessageFunctions.sendTranslatableMessage(source, " ", "collective.biomespawnpoint.message.spawnbiome", ChatFormatting.GRAY, getBiomeName(serverLevel, spawnPos));
		return 1;
	}

	private static void showUsage(CommandSourceStack source, String commandName) {
		MessageFunctions.sendTranslatableMessage(source, "collective.shared.message.usage", true, ChatFormatting.DARK_GREEN, Reference.NAME);
		MessageFunctions.sendMessage(source, " /" + commandName + " usage", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.shared.message.showmessage", ChatFormatting.GRAY);
		MessageFunctions.sendMessage(source, " /" + commandName + " reload", ChatFormatting.DARK_GREEN);
		MessageFunctions.sendTranslatableMessage(source, "  ", "collective.biomespawnpoint.message.reloadsconfigfile", ChatFormatting.GRAY);
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
