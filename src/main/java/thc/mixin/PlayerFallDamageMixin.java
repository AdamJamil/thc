package thc.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.THCAttachments;

@Mixin(LivingEntity.class)
public abstract class PlayerFallDamageMixin {

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void thc$negateFallDamage(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;

		// Only process for server players
		if (!(self instanceof ServerPlayer player)) {
			return;
		}

		AttachmentTarget target = (AttachmentTarget) player;
		Boolean boosted = target.getAttached(THCAttachments.WIND_CHARGE_BOOSTED);
		if (boosted != null && boosted) {
			// Clear the flag - one-time use
			target.setAttached(THCAttachments.WIND_CHARGE_BOOSTED, false);
			// Return true to indicate damage was "handled" (negated)
			cir.setReturnValue(true);
		}
	}
}
