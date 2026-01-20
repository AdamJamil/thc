package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.threat.ThreatManager;

/**
 * Mixin to propagate threat when player deals damage to a mob.
 * Targets LivingEntity.hurtServer and filters for Mob instances.
 */
@Mixin(LivingEntity.class)
public abstract class MobDamageThreatMixin {
	private static final double THC_THREAT_RADIUS = 15.0;

	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void thc$propagateThreatOnDamage(
		ServerLevel level,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		// Only run for Mob instances
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Mob mob)) {
			return;
		}

		// Only propagate if damage was actually dealt
		if (!cir.getReturnValue() || amount <= 0) {
			return;
		}

		// Get the player who dealt damage
		Entity attacker = source.getEntity();
		if (!(attacker instanceof ServerPlayer player)) {
			return;
		}

		// Add threat to all hostile/neutral mobs within 15 blocks
		AABB area = mob.getBoundingBox().inflate(THC_THREAT_RADIUS);
		for (Mob nearby : level.getEntitiesOfClass(Mob.class, area, MobDamageThreatMixin::thc$isHostileOrNeutral)) {
			ThreatManager.addThreat(nearby, player.getUUID(), amount);
		}
	}

	@Unique
	private static boolean thc$isHostileOrNeutral(Mob mob) {
		MobCategory category = mob.getType().getCategory();
		return category == MobCategory.MONSTER || category == MobCategory.CREATURE;
		// Note: CREATURE includes wolves, iron golems which are neutral
		// MONSTER is hostile mobs
	}
}
