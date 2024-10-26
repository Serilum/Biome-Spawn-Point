package com.natamus.biomespawnpoint.neoforge.events;

import com.mojang.logging.LogUtils;
import com.natamus.biomespawnpoint.events.BiomeSpawnEvent;
import com.natamus.collective.functions.WorldFunctions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber
public class NeoForgeBiomeSpawnEvent {
	private static final Logger logger = LogUtils.getLogger();

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
}
