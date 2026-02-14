package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import thc.bow.BowTypeTagAccess;
import thc.playerclass.ClassManager;
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

	@Unique
	private boolean thc$blazeFireApplied = false;

	/**
	 * Make blaze_bow arrows appear flaming in flight.
	 * One-time check: once applied, the arrow burns visually for its entire lifetime.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$applyBlazeFireVisual(CallbackInfo ci) {
		if (thc$blazeFireApplied) {
			return;
		}
		AbstractArrow self = (AbstractArrow) (Object) this;
		String bowTypeTag = ((BowTypeTagAccess) self).thc$getBowTypeTag();
		if ("blaze_bow".equals(bowTypeTag)) {
			self.setRemainingFireTicks(2000);
			thc$blazeFireApplied = true;
		}
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void thc$applyArrowHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		Entity owner = self.getOwner();

		// Reduce Pillager arrow damage by ~33% (5-7 -> 3-5)
		if (owner != null && owner.getType() == EntityType.PILLAGER) {
			baseDamage = baseDamage * 0.667;
			return;
		}

		// Reduce Stray arrow damage by 50% (4-8 -> 2-4)
		if (owner != null && owner.getType() == EntityType.STRAY) {
			baseDamage = baseDamage * 0.5;
			return;
		}

		if (!(owner instanceof ServerPlayer player)) {
			return;
		}

		// Store original damage and reduce to 13% for player-shot arrows (0.65 * 0.2)
		thc$originalBaseDamage = baseDamage;

		// Apply base reduction then class multiplier
		double reducedDamage = baseDamage * 0.13;

		// Apply class-based ranged multiplier
		reducedDamage *= ClassManager.getEffectiveRangedMultiplier(player);

		// Apply bow-type damage multiplier
		double bowDamageMultiplier = 1.0;
		String bowTypeTag = ((BowTypeTagAccess) self).thc$getBowTypeTag();
		if ("wooden_bow".equals(bowTypeTag)) {
			bowDamageMultiplier = 0.5;  // Wooden bow: 50% additional reduction
		} else if ("breeze_bow".equals(bowTypeTag)) {
			bowDamageMultiplier = 0.75; // Breeze bow: 75% of final damage (DMG-02)
		}
		baseDamage = reducedDamage * bowDamageMultiplier;

		Entity hitEntity = entityHitResult.getEntity();
		if (!(hitEntity instanceof LivingEntity target)) {
			return;
		}

		// Blaze Bow: set target on fire for 3 seconds (0.5 damage/second = 1.5 HP total)
		if ("blaze_bow".equals(bowTypeTag)) {
			target.setRemainingFireTicks(60);
		}

		// Apply Speed 3 for 6 seconds (Glowing removed per DMG-05)
		target.addEffect(new MobEffectInstance(MobEffects.SPEED, THC_EFFECT_DURATION_TICKS, 2), player);

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

		// Breeze Bow arrows preserve vanilla knockback on monsters (DMG-04)
		String bowTypeTag = ((BowTypeTagAccess) self).thc$getBowTypeTag();
		if ("breeze_bow".equals(bowTypeTag)) {
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
