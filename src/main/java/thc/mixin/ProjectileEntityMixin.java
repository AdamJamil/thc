package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.threat.ThreatManager;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {
	private static final int THC_EFFECT_DURATION_TICKS = 120;

	@Unique private double thc$spawnX = Double.NaN;
	@Unique private double thc$spawnY = Double.NaN;
	@Unique private double thc$spawnZ = Double.NaN;
	@Unique private boolean thc$spawnRecorded = false;

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void thc$applyHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;
		Entity owner = self.getOwner();

		if (!(owner instanceof ServerPlayer player)) {
			return;
		}

		Entity hitEntity = entityHitResult.getEntity();
		if (!(hitEntity instanceof LivingEntity target)) {
			return;
		}

		target.addEffect(new MobEffectInstance(MobEffects.SPEED, THC_EFFECT_DURATION_TICKS, 3), player);
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, THC_EFFECT_DURATION_TICKS, 0), player);

		if (target instanceof Mob mob) {
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
	private void thc$applyVelocityBoost(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;

		// Only boost player-shot projectiles
		if (!(self.getOwner() instanceof ServerPlayer)) {
			return;
		}

		// Record spawn position for gravity calculations
		if (!thc$spawnRecorded) {
			thc$spawnX = self.getX();
			thc$spawnY = self.getY();
			thc$spawnZ = self.getZ();
			thc$spawnRecorded = true;
		}

		// Apply 20% velocity boost
		Vec3 velocity = self.getDeltaMovement();
		self.setDeltaMovement(velocity.scale(1.2));
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void thc$applyEnhancedGravity(CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;

		// Only affect player-shot projectiles
		if (!(self.getOwner() instanceof ServerPlayer)) {
			return;
		}

		// Skip if spawn not recorded yet
		if (!thc$spawnRecorded || Double.isNaN(thc$spawnX)) {
			return;
		}

		// Calculate distance from spawn
		double dx = self.getX() - thc$spawnX;
		double dy = self.getY() - thc$spawnY;
		double dz = self.getZ() - thc$spawnZ;
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

		// After 4 blocks, apply additional downward velocity
		if (distance >= 4.0) {
			double extraBlocks = distance - 4.0;
			// Enhanced gravity: 0.6 * (extraBlocks)^1.5
			double gravityMultiplier = 0.6 * Math.pow(extraBlocks, 1.5);
			// Cap at reasonable maximum
			gravityMultiplier = Math.min(gravityMultiplier, 0.1);

			Vec3 velocity = self.getDeltaMovement();
			self.setDeltaMovement(velocity.x, velocity.y - gravityMultiplier, velocity.z);
		}
	}
}
