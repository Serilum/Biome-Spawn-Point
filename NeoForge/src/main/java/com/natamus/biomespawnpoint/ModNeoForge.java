package com.natamus.biomespawnpoint;

import com.natamus.biomespawnpoint.neoforge.events.NeoForgeBiomeSpawnEvent;
import com.natamus.biomespawnpoint.util.Reference;
import com.natamus.collective.check.RegisterMod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

@Mod(Reference.MOD_ID)
public class ModNeoForge {
	
	public ModNeoForge(IEventBus modEventBus) {
		modEventBus.addListener(this::loadComplete);

		setGlobalConstants();
		ModCommon.init();

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadComplete(final FMLLoadCompleteEvent event) {
		NeoForge.EVENT_BUS.register(NeoForgeBiomeSpawnEvent.class);
	}

	private static void setGlobalConstants() {

	}
}