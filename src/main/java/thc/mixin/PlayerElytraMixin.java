package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies pitch-based velocity multipliers during elytra flight.
 * <p>
 * Amplifies natural glide physics instead of overriding them:
 * - Diving (positive pitch, looking down): 2x multiplier on velocity delta
 * - Ascending (negative pitch, looking up): 1.8x multiplier on velocity delta
 * <p>
 * The multiplier is applied to the delta (change per tick), not absolute velocity.
 * This rewards skill-based diving while making ascent cost more speed.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerElytraMixin {

	private static final double DIVING_MULTIPLIER = 2.0;
	private static final double ASCENDING_MULTIPLIER = 1.8;

	@Unique
	private double thc$velocityXBefore;
	@Unique
	private double thc$velocityYBefore;
	@Unique
	private double thc$velocityZBefore;
	@Unique
	private boolean thc$wasCapturing;

	/**
	 * Capture velocity before travel() runs if entity is a gliding ServerPlayer.
	 */
	@Inject(method = "travel", at = @At("HEAD"))
	private void thc$captureVelocityBefore(Vec3 movementInput, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;

		// Only process for ServerPlayer who is elytra gliding
		if (!(self instanceof ServerPlayer player) || !player.isFallFlying()) {
			thc$wasCapturing = false;
			return;
		}

		Vec3 vel = player.getDeltaMovement();
		thc$velocityXBefore = vel.x;
		thc$velocityYBefore = vel.y;
		thc$velocityZBefore = vel.z;
		thc$wasCapturing = true;
	}

	/**
	 * After travel() completes, apply pitch-based multiplier to velocity delta.
	 */
	@Inject(method = "travel", at = @At("TAIL"))
	private void thc$applyPitchMultipliers(Vec3 movementInput, CallbackInfo ci) {
		// Only process if we were capturing (was a gliding ServerPlayer at HEAD)
		if (!thc$wasCapturing) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof ServerPlayer player) || !player.isFallFlying()) {
			return;
		}

		Vec3 velAfter = player.getDeltaMovement();
		double deltaX = velAfter.x - thc$velocityXBefore;
		double deltaY = velAfter.y - thc$velocityYBefore;
		double deltaZ = velAfter.z - thc$velocityZBefore;

		// Get pitch: positive = looking down (diving), negative = looking up (ascending)
		float pitch = player.getXRot();

		// Determine multiplier based on pitch sign
		// Pitch >= 0 (diving or neutral): gains speed faster
		// Pitch < 0 (ascending): loses speed faster
		double multiplier = pitch >= 0 ? DIVING_MULTIPLIER : ASCENDING_MULTIPLIER;

		// Apply multiplier to delta and compute new velocity
		double newX = thc$velocityXBefore + deltaX * multiplier;
		double newY = thc$velocityYBefore + deltaY * multiplier;
		double newZ = thc$velocityZBefore + deltaZ * multiplier;

		player.setDeltaMovement(newX, newY, newZ);

		// CRITICAL: Set hurtMarked to sync velocity to client
		player.hurtMarked = true;
	}
}
