package thc.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.entity.EndermanProximityAggroGoal;

/**
 * Modifies Enderman behavior:
 * - FR-11: Proximity aggro - aggros when player is within 3 blocks (no eye contact needed)
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
}
