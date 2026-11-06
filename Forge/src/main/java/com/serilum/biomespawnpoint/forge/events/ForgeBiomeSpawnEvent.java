package com.serilum.biomespawnpoint.forge.events;

import com.mojang.logging.LogUtils;
import com.serilum.biomespawnpoint.cmds.CommandBiomeSpawnPoint;
import com.serilum.biomespawnpoint.events.BiomeSpawnEvent;
import com.natamus.collective.functions.WorldFunctions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class ForgeBiomeSpawnEvent {
	private static final Logger logger = LogUtils.getLogger();

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent e) {
		CommandBiomeSpawnPoint.register(e.getDispatcher());
	}

	@SubscribeEvent(receiveCanceled = true)
	public static void onWorldLoad(LevelEvent.CreateSpawnPosition e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		if (BiomeSpawnEvent.onWorldLoad((ServerLevel)level, (ServerLevelData)level.getLevelData())) {
			e.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent e) {
		BiomeSpawnEvent.onServerStarted(e.getServer());
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
		Player player = e.getEntity();
		BiomeSpawnEvent.onPlayerLoggedIn(player.level(), player);
	}
}
