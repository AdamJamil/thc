package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.bow.BowType;
import thc.threat.ThreatManager;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {
	@Unique private String thc$bowTypeTag = null;
	@Unique private double thc$bowDragFactor = 0.0;
	@Unique private int thc$ticksInFlight = 0;

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void thc$applyHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;

		// Skip arrows - they have their own handler in AbstractArrowMixin
		if (self instanceof AbstractArrow) {
			return;
		}

		Entity owner = self.getOwner();

		if (!(owner instanceof ServerPlayer player)) {
			return;
		}

		Entity hitEntity = entityHitResult.getEntity();

		// Speed and Glowing effects are arrow-only (see AbstractArrowMixin)
		// Non-arrow projectiles (snowballs, eggs, etc.) do not apply these effects

		if (hitEntity instanceof Mob mob) {
			mob.setTarget(player);
			// Add +10 bonus threat to struck mob (THREAT-04)
			ThreatManager.addThreat(mob, player.getUUID(), 10.0);
		}
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void thc$removeArrowKnockback(EntityHitResult entityHitResult, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;
		Entity owner = self.getOwner();

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

	@Inject(method = "shoot", at = @At("TAIL"))
	private void thc$tagBowTypeOnShoot(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;

		// Only tag player-shot projectiles
		if (!(self.getOwner() instanceof ServerPlayer player)) {
			return;
		}

		// Determine bow type from the player's active use item (the bow being drawn)
		BowType bowType = BowType.fromBowItem(player.getUseItem());
		thc$bowTypeTag = bowType.getTag();
		thc$bowDragFactor = bowType.getDragFactor();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$applyHorizontalDrag(CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;

		// Only affect player-shot projectiles
		if (!(self.getOwner() instanceof ServerPlayer)) {
			return;
		}

		// Only apply drag if bow type was set (means this was shot from a bow)
		if (thc$bowDragFactor <= 0) {
			return;
		}

		thc$ticksInFlight++;

		// Calculate drag coefficient: max(0.8, 1.0 - dragFactor * ticksInFlight)
		double dragCoefficient = Math.max(0.8, 1.0 - thc$bowDragFactor * thc$ticksInFlight);

		// Apply drag to horizontal components only, leave vertical (y) untouched
		Vec3 velocity = self.getDeltaMovement();
		self.setDeltaMovement(velocity.x * dragCoefficient, velocity.y, velocity.z * dragCoefficient);
	}

	/**
	 * Returns the bow type tag for this projectile, or null if not shot from a tagged bow.
	 * Used by AbstractArrowMixin for damage multiplier lookup.
	 */
	@Unique
	public String thc$getBowTypeTag() {
		return thc$bowTypeTag;
	}
}
