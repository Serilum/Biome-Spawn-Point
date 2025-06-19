package com.natamus.biomespawnpoint.forge.events;

import com.mojang.logging.LogUtils;
import com.natamus.biomespawnpoint.events.BiomeSpawnEvent;
import com.natamus.collective.functions.WorldFunctions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import org.slf4j.Logger;

public class ForgeBiomeSpawnEvent {
	public static void registerEventsInBus() {
		// BusGroup.DEFAULT.register(MethodHandles.lookup(), ForgeBiomeSpawnEvent.class);

		LevelEvent.CreateSpawnPosition.BUS.addListener(ForgeBiomeSpawnEvent::onWorldLoad);
	}

	private static final Logger logger = LogUtils.getLogger();

	@SubscribeEvent
	public static boolean onWorldLoad(LevelEvent.CreateSpawnPosition e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return false;
		}

		if (BiomeSpawnEvent.onWorldLoad((ServerLevel)level, (ServerLevelData)level.getLevelData())) {
			return true;
		}
		return false;
	}
}
