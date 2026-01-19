package thc.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.THCAttachments;

@Mixin(WindCharge.class)
public abstract class WindChargePlayerBoostMixin {
	private static final double THC_BOOST_MULTIPLIER = 1.5;
	private static final double THC_BOOST_RANGE_SQUARED = 4.0 * 4.0;

	@Inject(method = "explode", at = @At("TAIL"))
	private void thc$enhancePlayerBoost(Vec3 pos, CallbackInfo ci) {
		WindCharge self = (WindCharge) (Object) this;
		Entity owner = self.getOwner();

		if (!(owner instanceof ServerPlayer player)) {
			return;
		}

		// Check if player is within boost range of explosion
		double distSq = player.distanceToSqr(pos);
		if (distSq > THC_BOOST_RANGE_SQUARED) {
			return;
		}

		// Player was boosted by their own wind charge - enhance the Y velocity
		Vec3 velocity = player.getDeltaMovement();

		// Only boost if player has upward velocity (was actually affected by explosion)
		if (velocity.y <= 0.0) {
			return;
		}

		// Apply 50% Y velocity boost
		double boostedY = velocity.y * THC_BOOST_MULTIPLIER;
		player.setDeltaMovement(velocity.x, boostedY, velocity.z);
		player.hurtMarked = true;

		// Set fall damage negation flag
		((AttachmentTarget) player).setAttached(THCAttachments.WIND_CHARGE_BOOSTED, true);
	}
}
