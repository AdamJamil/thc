package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.entity.EndermanProximityAggroGoal;
import thc.mixin.access.EnderManAccessor;

/**
 * Modifies Enderman behavior:
 * - FR-11: Proximity aggro - aggros when player is within 3 blocks (no eye contact needed)
 * - FR-10: 50% chance to teleport behind player after taking damage
 */
@Mixin(EnderMan.class)
public abstract class EndermanMixin {

	@Shadow
	@Final
	protected GoalSelector targetSelector;

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void thc$addProximityAggroGoal(CallbackInfo ci) {
		// Priority 1 = high priority, after ThreatTargetGoal (0) but before vanilla targeting (2+)
		this.targetSelector.addGoal(1, new EndermanProximityAggroGoal((EnderMan) (Object) this));
	}

	/**
	 * FR-10: After taking damage from a player, 50% chance to teleport behind them.
	 * This makes Endermen harder to fight - they flank the player.
	 */
	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void thc$teleportBehindPlayer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		// Only trigger if damage was actually dealt
		if (!cir.getReturnValue() || amount <= 0) {
			return;
		}

		// Only teleport behind players, not other entities
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		// 50% chance to trigger
		if (!level.random.nextBoolean()) {
			return;
		}

		// Calculate position 3 blocks behind player (based on their look direction)
		Vec3 playerPos = player.position();
		Vec3 playerLook = player.getLookAngle();
		Vec3 behindPos = playerPos.subtract(playerLook.scale(3.0));

		// Attempt to teleport via accessor - if invalid position, vanilla handles random teleport
		((EnderManAccessor) this).invokerTeleport(behindPos.x, behindPos.y, behindPos.z);
	}
}
