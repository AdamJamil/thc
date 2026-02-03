package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
import thc.downed.DownedState;

/**
 * Prevents downed spectator players from clipping through blocks.
 *
 * Phase A: Forces noPhysics=false to attempt using vanilla collision.
 * Phase B: If still inside a block after tick, clamps position to last valid.
 */
@Mixin(ServerPlayer.class)
public abstract class SpectatorCollisionMixin {
	@Unique
	private Vec3 thc$lastValidPos = null;

	@Unique
	private boolean thc$phaseAWorked = true;

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$forceCollisionHead(CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;

		// Only apply to downed players (spectators with downed location)
		if (!DownedState.isDowned(self)) {
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
		ServerPlayer self = (ServerPlayer) (Object) this;

		// Only apply to downed players
		if (!DownedState.isDowned(self)) {
			return;
		}

		// Re-apply noPhysics=false (spectator code may have flipped it)
		((Entity) self).noPhysics = false;

		// Phase B: If player is inside a solid block, Phase A failed - clamp position
		if (thc$isInsideSolidBlock(self)) {
			thc$phaseAWorked = false;
			if (thc$lastValidPos != null) {
				self.teleportTo(thc$lastValidPos.x, thc$lastValidPos.y, thc$lastValidPos.z);
			}
		}
	}

	/**
	 * Checks if the player's bounding box overlaps with any solid block.
	 */
	@Unique
	private boolean thc$isInsideSolidBlock(ServerPlayer player) {
		AABB box = player.getBoundingBox().deflate(0.001);
		ServerLevel level = (ServerLevel) player.level();
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
