package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.playerclass.ClassManager;
import thc.playerclass.PlayerClass;
import thc.stage.StageManager;

/**
 * Mixin for Snowball to implement enhanced snowball effects for Bastion class at Stage 4+.
 * Snowball overrides Projectile.onHitEntity and doesn't call super,
 * so we need a separate mixin to intercept snowball hits.
 *
 * Effects (Bastion Stage 4+ only):
 * - Slowness III (2s) to target mob
 * - Slowness III (2s) to hostile mobs within 1.5 blocks
 * - Knockback ~1 block away from thrower (target only)
 */
@Mixin(Snowball.class)
public abstract class SnowballHitMixin {
	private static final int THC_SLOWNESS_DURATION = 40;      // 2 seconds
	private static final int THC_SLOWNESS_AMPLIFIER = 2;      // Level III (0-indexed)
	private static final double THC_AOE_RADIUS = 1.5;
	private static final double THC_KNOCKBACK_STRENGTH = 0.4;
	private static final double THC_KNOCKBACK_VERTICAL = 0.2;

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void thc$applyEnhancedSnowballEffects(EntityHitResult result, CallbackInfo ci) {
		Snowball self = (Snowball) (Object) this;

		// Gate: server-side only
		if (!(self.level() instanceof ServerLevel level)) {
			return;
		}

		Entity owner = self.getOwner();

		// Gate: only player-thrown snowballs
		if (!(owner instanceof ServerPlayer player)) {
			return;
		}

		// Gate: Bastion class only
		PlayerClass playerClass = ClassManager.getClass(player);
		if (playerClass != PlayerClass.BASTION) {
			return;
		}

		// Gate: Stage 4+ (boon level >= 4)
		if (StageManager.getBoonLevel(player) < 4) {
			return;
		}

		// Gate: only affect mobs (SNOW-05: players unaffected)
		Entity hitEntity = result.getEntity();
		if (!(hitEntity instanceof Mob targetMob)) {
			return;
		}

		// Gate: only affect hostile mobs targeting a player
		if (!thc$isHostileMobTargetingPlayer(targetMob)) {
			return;
		}

		// Apply effects to target
		thc$applySlowness(targetMob, player);
		thc$applyKnockback(targetMob, player);

		// Apply AoE slowness to nearby hostile mobs
		thc$applyAoESlowness(level, targetMob, player);
	}

	/**
	 * Check if a mob is a hostile monster currently targeting a player.
	 * Per CONTEXT.md: only affect hostile mobs (entities currently targeting a player).
	 * This excludes neutral mobs and passive mobs.
	 */
	@Unique
	private static boolean thc$isHostileMobTargetingPlayer(Mob mob) {
		if (mob.getType().getCategory() != MobCategory.MONSTER) {
			return false;
		}
		return mob.getTarget() instanceof Player;
	}

	/**
	 * Apply Slowness III to a mob.
	 */
	@Unique
	private static void thc$applySlowness(Mob mob, ServerPlayer source) {
		mob.addEffect(
			new MobEffectInstance(MobEffects.SLOWNESS, THC_SLOWNESS_DURATION, THC_SLOWNESS_AMPLIFIER),
			source
		);
	}

	/**
	 * Apply knockback to target mob away from the thrower.
	 * Per CONTEXT.md: knockback direction is away from the thrower, not the impact point.
	 */
	@Unique
	private static void thc$applyKnockback(Mob target, ServerPlayer thrower) {
		// Direction: from thrower to mob (away from thrower)
		Vec3 direction = target.position().subtract(thrower.position()).normalize();

		// Apply horizontal knockback + slight upward lift
		target.setDeltaMovement(
			direction.x * THC_KNOCKBACK_STRENGTH,
			THC_KNOCKBACK_VERTICAL,
			direction.z * THC_KNOCKBACK_STRENGTH
		);
		target.hurtMarked = true; // Force sync to client
	}

	/**
	 * Apply Slowness III to hostile mobs within the AoE radius.
	 * Only affects hostile mobs targeting a player, excluding the direct hit target.
	 */
	@Unique
	private static void thc$applyAoESlowness(ServerLevel level, Mob target, ServerPlayer source) {
		AABB area = target.getBoundingBox().inflate(THC_AOE_RADIUS);

		for (Mob nearby : level.getEntitiesOfClass(Mob.class, area,
			SnowballHitMixin::thc$isHostileMobTargetingPlayer)) {
			// Skip the direct hit target (already has slowness)
			if (nearby == target) {
				continue;
			}
			thc$applySlowness(nearby, source);
		}
	}
}
