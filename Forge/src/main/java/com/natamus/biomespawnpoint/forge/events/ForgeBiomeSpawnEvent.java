package com.natamus.biomespawnpoint.forge.events;

import com.mojang.logging.LogUtils;
import com.natamus.biomespawnpoint.events.BiomeSpawnEvent;
import com.natamus.collective.functions.WorldFunctions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber
public class ForgeBiomeSpawnEvent {
	private static final Logger logger = LogUtils.getLogger();

	@SubscribeEvent(receiveCanceled = true)
	public void onWorldLoad(WorldEvent.CreateSpawnPosition e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getWorld());
		if (level == null) {
			return;
		}

		if (BiomeSpawnEvent.onWorldLoad((ServerLevel)level, (ServerLevelData)level.getLevelData())) {
			e.setCanceled(true);
		}
	}
}
