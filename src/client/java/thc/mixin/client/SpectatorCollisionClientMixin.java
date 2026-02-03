package thc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.client.DownedPlayersClientState;

import java.util.UUID;

/**
 * Client-side collision enforcement for downed spectator players.
 *
 * Phase A: Forces noPhysics=false to attempt using vanilla collision.
 * Phase B: If still inside a block after tick, clamps position to last valid.
 */
@Mixin(LocalPlayer.class)
public abstract class SpectatorCollisionClientMixin {
	@Unique
	private Vec3 thc$lastValidPos = null;

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$forceCollisionHead(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;

		// Only apply to downed players
		if (!thc$isLocalPlayerDowned(self)) {
			thc$lastValidPos = null;
			return;
		}

		// Phase A: Force noPhysics=false to enable collision checks
		((Entity) self).noPhysics = false;

		// Store last valid position before movement (for Phase B fallback)
		if (!thc$isInsideSolidBlock(self)) {
			thc$lastValidPos = self.position();
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void thc$forceCollisionTail(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;

		// Only apply to downed players
		if (!thc$isLocalPlayerDowned(self)) {
			return;
		}

		// Re-apply noPhysics=false (spectator code may have flipped it)
		((Entity) self).noPhysics = false;

		// Phase B: If player is inside a solid block, Phase A failed - clamp position
		if (thc$isInsideSolidBlock(self)) {
			if (thc$lastValidPos != null) {
				self.setPos(thc$lastValidPos.x, thc$lastValidPos.y, thc$lastValidPos.z);
			}
		}
	}

	/**
	 * Checks if the local player is downed using client state.
	 */
	@Unique
	private boolean thc$isLocalPlayerDowned(LocalPlayer player) {
		UUID uuid = player.getUUID();
		return DownedPlayersClientState.getDownedPlayers().containsKey(uuid);
	}

	/**
	 * Checks if the player's bounding box overlaps with any solid block.
	 */
	@Unique
	private boolean thc$isInsideSolidBlock(LocalPlayer player) {
		AABB box = player.getBoundingBox().deflate(0.001);
		Level level = player.level();
		CollisionContext context = CollisionContext.of(player);

		BlockPos minPos = BlockPos.containing(box.minX, box.minY, box.minZ);
		BlockPos maxPos = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

		for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				continue;
			}
			VoxelShape shape = state.getCollisionShape(level, pos, context);
			if (shape.isEmpty()) {
				continue;
			}
			VoxelShape movedShape = shape.move(pos.getX(), pos.getY(), pos.getZ());
			if (movedShape.toAabbs().stream().anyMatch(aabb -> aabb.intersects(box))) {
				return true;
			}
		}
		return false;
	}
}
