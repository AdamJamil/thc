package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

		target.addEffect(new MobEffectInstance(MobEffects.SPEED, THC_EFFECT_DURATION_TICKS, 1), player);
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, THC_EFFECT_DURATION_TICKS, 0), player);

		if (target instanceof Mob mob) {
			mob.setTarget(player);
		}
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
}
