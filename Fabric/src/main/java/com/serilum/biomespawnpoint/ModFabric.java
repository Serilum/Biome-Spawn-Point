package com.serilum.biomespawnpoint;

import com.serilum.biomespawnpoint.cmds.CommandBiomeSpawnPoint;
import com.serilum.biomespawnpoint.events.BiomeSpawnEvent;
import com.serilum.biomespawnpoint.util.Reference;
import com.natamus.collective.check.RegisterMod;
import com.natamus.collective.check.ShouldLoadCheck;
import com.natamus.collective.fabric.callbacks.CollectiveMinecraftServerEvents;
import com.natamus.collective.fabric.callbacks.CollectivePlayerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;

public class ModFabric implements ModInitializer {
	
	@Override
	public void onInitialize() {
		if (!ShouldLoadCheck.shouldLoad(Reference.MOD_ID)) {
			return;
		}

		setGlobalConstants();
		ModCommon.init();

		loadEvents();

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadEvents() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandBiomeSpawnPoint.register(dispatcher);
		});

		CollectiveMinecraftServerEvents.WORLD_CREATE_SPAWN.register((ServerLevel serverLevel, ServerLevelData serverLevelData) -> {
			return BiomeSpawnEvent.onWorldLoad(serverLevel, serverLevelData);
		});

		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			BiomeSpawnEvent.onServerStarted(server);
		});

		CollectivePlayerEvents.PLAYER_LOGGED_IN.register((Level level, Player player) -> {
			BiomeSpawnEvent.onPlayerLoggedIn(level, player);
		});
	}

	private static void setGlobalConstants() {

	}
}
