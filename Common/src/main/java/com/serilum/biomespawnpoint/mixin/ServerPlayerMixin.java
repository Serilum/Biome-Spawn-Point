package com.serilum.biomespawnpoint.mixin;

import com.serilum.biomespawnpoint.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class, priority = 1001)
public class ServerPlayerMixin {
	@Inject(method = "findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;", at = @At(value = "RETURN"), cancellable = true)
	public void findRespawnPositionAndUseSpawnBlock(boolean keepInventory, DimensionTransition.PostDimensionTransition postDimensionTransition, CallbackInfoReturnable<DimensionTransition> cir) {
		DimensionTransition dimensionSpawnTransition = Util.getDimensionSpawnTransition((ServerPlayer)(Object)this, cir.getReturnValue(), postDimensionTransition);
		if (dimensionSpawnTransition != null) {
			cir.setReturnValue(dimensionSpawnTransition);
		}
	}
}
