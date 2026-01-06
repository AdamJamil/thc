package thc.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import thc.THCAttachments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.access.ServerPlayerHealthAccess;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerPlayerHealthAccess {
	@Unique
	private static final double THC_DEFAULT_MAX_HEALTH = 8.0D;

	@Unique
	private boolean thcAppliedMaxHealth;

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void thc$restoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		this.thcAppliedMaxHealth = this.thc$applyMaxHealth();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$ensureMaxHealthApplied(CallbackInfo ci) {
		if (!this.thcAppliedMaxHealth) {
			this.thcAppliedMaxHealth = this.thc$applyMaxHealth();
		}
	}

	@Unique
	private boolean thc$applyMaxHealth() {
		LivingEntity self = (LivingEntity) (Object) this;
		double maxHealth = this.thc$getStoredMaxHealth();
		AttributeInstance instance = self.getAttribute(Attributes.MAX_HEALTH);
		if (instance == null) {
			return false;
		}

		instance.setBaseValue(maxHealth);
		if (self.getHealth() > maxHealth) {
			self.setHealth((float) maxHealth);
		}
		return true;
	}

	@Unique
	private double thc$getStoredMaxHealth() {
		AttachmentTarget target = (AttachmentTarget) this;
		Double stored = target.getAttachedOrCreate(THCAttachments.MAX_HEALTH);
		double value = stored == null ? THC_DEFAULT_MAX_HEALTH : stored;
		if (!Double.isFinite(value)) {
			value = THC_DEFAULT_MAX_HEALTH;
		}
		value = Math.max(1.0D, value);
		if (stored == null || stored.doubleValue() != value) {
			target.setAttached(THCAttachments.MAX_HEALTH, value);
		}
		return value;
	}

	@Override
	public double thc$getMaxHealth() {
		return this.thc$getStoredMaxHealth();
	}

	@Override
	public void thc$setMaxHealth(double maxHealth) {
		double value = Math.max(1.0D, maxHealth);
		if (!Double.isFinite(value)) {
			value = THC_DEFAULT_MAX_HEALTH;
		}
		((AttachmentTarget) this).setAttached(THCAttachments.MAX_HEALTH, value);
		this.thcAppliedMaxHealth = this.thc$applyMaxHealth();
	}
}
