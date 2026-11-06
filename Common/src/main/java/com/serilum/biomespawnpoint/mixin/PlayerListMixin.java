package com.serilum.biomespawnpoint.mixin;

import com.serilum.biomespawnpoint.util.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(value = PlayerList.class, priority = 1001)
public class PlayerListMixin {
	@Shadow @Final private MinecraftServer server;

	@ModifyVariable(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;", at = @At(value = "STORE"), ordinal = 0)
	public Optional<Vec3> respawn_respawnBlockPosition(Optional<Vec3> respawnBlockPosition) {
		Util.prepareRespawn(server, respawnBlockPosition.isPresent());
		return respawnBlockPosition;
	}

	@ModifyVariable(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;", at = @At(value = "STORE"), ordinal = 1)
	public ServerLevel respawn_respawnLevel(ServerLevel respawnLevel) {
		return Util.getRespawnLevel(server, respawnLevel);
	}
}
