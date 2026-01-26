package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cancels firework rocket boost for players during elytra flight.
 * <p>
 * Fireworks still fire visually and explode normally, but the velocity
 * addition to gliding players is prevented. This removes trivial elytra
 * propulsion (firework spam) in favor of skill-based diving mechanics.
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {

	@Shadow
	private LivingEntity attachedToEntity;

	/**
	 * Intercepts the setDeltaMovement call that would boost an attached gliding player.
	 * If the target is a player, the boost is cancelled. Non-player entities still receive boost.
	 */
	@Redirect(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
		)
	)
	private void thc$cancelPlayerElytraBoost(LivingEntity entity, Vec3 boostedVelocity) {
		// Only cancel boost for players - other entities (if any) still get boosted
		if (entity instanceof ServerPlayer) {
			// Do nothing - cancel the velocity boost
			// The firework still fires visually and explodes, just no velocity change
			return;
		}
		// For non-players, apply the boost normally
		entity.setDeltaMovement(boostedVelocity);
	}
}
