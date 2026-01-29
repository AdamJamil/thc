package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.playerclass.ClassManager;
import thc.playerclass.PlayerClass;
import thc.threat.ThreatManager;

/**
 * Mixin for AbstractArrow to handle arrow-specific hit effects.
 * AbstractArrow overrides Projectile.onHitEntity and doesn't call super,
 * so we need a separate mixin to intercept arrow hits.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	private static final int THC_EFFECT_DURATION_TICKS = 120;

	@Shadow
	private double baseDamage;

	@Unique
	private double thc$originalBaseDamage;

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void thc$applyArrowHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		Entity owner = self.getOwner();

		if (!(owner instanceof ServerPlayer player)) {
			return;
		}

		// Store original damage and reduce to 13% for player-shot arrows (0.65 * 0.2)
		thc$originalBaseDamage = baseDamage;

		// Apply base reduction then class multiplier
		double reducedDamage = baseDamage * 0.13;

		// Apply class-based ranged multiplier
		PlayerClass playerClass = ClassManager.getClass(player);
		if (playerClass != null) {
			reducedDamage *= playerClass.getRangedMultiplier();
		}

		baseDamage = reducedDamage;

		Entity hitEntity = entityHitResult.getEntity();
		if (!(hitEntity instanceof LivingEntity target)) {
			return;
		}

		// Apply Speed 3 and Glowing for 6 seconds
		target.addEffect(new MobEffectInstance(MobEffects.SPEED, THC_EFFECT_DURATION_TICKS, 2), player);
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, THC_EFFECT_DURATION_TICKS, 0), player);

		if (target instanceof Mob mob) {
			mob.setTarget(player);
			// Add +10 bonus threat to struck mob (THREAT-04)
			ThreatManager.addThreat(mob, player.getUUID(), 10.0);
		}
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void thc$removeArrowKnockback(EntityHitResult entityHitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		Entity owner = self.getOwner();

		// Restore original damage after hit processing
		if (owner instanceof ServerPlayer) {
			baseDamage = thc$originalBaseDamage;
		}

		if (!(owner instanceof ServerPlayer)) {
			return;
		}

		Entity hitEntity = entityHitResult.getEntity();
		if (!(hitEntity instanceof Mob mob)) {
			return;
		}

		// Only remove knockback from enemy mobs (monsters)
		if (mob.getType().getCategory() != MobCategory.MONSTER) {
			return;
		}

		// Reset velocity to cancel knockback (preserve Y for gravity)
		Vec3 velocity = mob.getDeltaMovement();
		mob.setDeltaMovement(0, velocity.y, 0);
		mob.hurtMarked = true;
	}
}
