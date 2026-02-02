package thc.mixin;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import thc.THCAttachments;
import thc.downed.DownedState;
import thc.playerclass.PlayerClass;
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
		// Read class from oldPlayer since attachment copy may not have completed
		AttachmentTarget oldTarget = (AttachmentTarget) oldPlayer;
		String className = oldTarget.getAttached(THCAttachments.PLAYER_CLASS);
		if (className != null) {
			PlayerClass playerClass = PlayerClass.fromString(className);
			if (playerClass != null) {
				LivingEntity self = (LivingEntity) (Object) this;
				double maxHealth = THC_DEFAULT_MAX_HEALTH + playerClass.getHealthBonus();
				AttributeInstance instance = self.getAttribute(Attributes.MAX_HEALTH);
				if (instance != null) {
					instance.setBaseValue(maxHealth);
					// Cap health if above new max (respawning sets health to 20)
					if (self.getHealth() > maxHealth) {
						self.setHealth((float) maxHealth);
					}
				}
			}
		}
		this.thcAppliedMaxHealth = true;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$ensureMaxHealthApplied(CallbackInfo ci) {
		if (!this.thcAppliedMaxHealth) {
			this.thcAppliedMaxHealth = this.thc$applyMaxHealth();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$enforceTether(CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		Vec3 downedLoc = DownedState.getDownedLocation(self);

		// Only enforce tether for downed players
		if (downedLoc == null) {
			return;
		}

		// 50 block tether radius (squared to avoid sqrt)
		double distSq = self.position().distanceToSqr(downedLoc);
		if (distSq > 2500.0) { // 50 * 50 = 2500
			self.teleportTo(downedLoc.x, downedLoc.y, downedLoc.z);
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
		// Calculate max health from class, not from cached attachment
		double value = THC_DEFAULT_MAX_HEALTH;

		AttachmentTarget target = (AttachmentTarget) this;
		String className = target.getAttached(THCAttachments.PLAYER_CLASS);
		if (className != null) {
			PlayerClass playerClass = PlayerClass.fromString(className);
			if (playerClass != null) {
				value = THC_DEFAULT_MAX_HEALTH + playerClass.getHealthBonus();
			}
		}

		return value;
	}

	@Override
	public double thc$getMaxHealth() {
		return this.thc$getStoredMaxHealth();
	}

	@Override
	public void thc$setMaxHealth(double maxHealth) {
		// Health is now derived from PLAYER_CLASS, so just trigger an update
		this.thcAppliedMaxHealth = this.thc$applyMaxHealth();
	}
}
