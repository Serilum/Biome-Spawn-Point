package com.serilum.biomespawnpoint.mixin;

import com.serilum.biomespawnpoint.util.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MinecraftServer.class, priority = 1001)
public class MinecraftServerMixin {
	@ModifyVariable(method = "setInitialSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/storage/ServerLevelData;ZZLnet/minecraft/server/level/progress/LevelLoadListener;)V", at = @At(value = "HEAD"), argsOnly = true, ordinal = 0)
	private static boolean setInitialSpawn_spawnBonusChest(boolean spawnBonusChest, ServerLevel serverLevel) {
		if (!spawnBonusChest) {
			return false;
		}
		return Util.isNewWorldSpawnInOverworld(serverLevel);
	}
}
