package com.serilum.biomespawnpoint.mixin;

import com.serilum.biomespawnpoint.util.Util;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayer.class, priority = 1001)
public class ServerPlayerMixin {
	@Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At(value = "TAIL"))
	public void restoreFrom(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
		Util.moveRespawningPlayerToDimensionSpawn((ServerPlayer)(Object)this);
	}
}
