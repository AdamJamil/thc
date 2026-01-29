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

	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void thc$propagateThreatOnDamage(
		ServerLevel level,
		DamageSource source,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		// Only run for Mob instances
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Mob damagedMob)) {
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

		// Calculate proximity threat: ceil(damage / 4)
		double proximityThreat = Math.ceil(amount / 4.0);

		// Find mobs within 5 blocks of PLAYER (not target)
		AABB area = player.getBoundingBox().inflate(5.0);
		for (Mob nearby : level.getEntitiesOfClass(Mob.class, area, MobDamageThreatMixin::thc$isHostileOrNeutral)) {
			// THRT-02: Skip the direct damage target
			if (nearby == damagedMob) {
				continue;
			}
			ThreatManager.addThreat(nearby, player.getUUID(), proximityThreat);
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
