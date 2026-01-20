package thc.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.threat.ThreatTargetGoal;

/**
 * Inject ThreatTargetGoal into all Monster mobs.
 * Targets Mob.registerGoals() to catch all mob goal registration, but only adds
 * ThreatTargetGoal to Monster subclasses (zombie, skeleton, creeper, etc.)
 */
@Mixin(Mob.class)
public abstract class MonsterThreatGoalMixin {

	@Shadow
	@Final
	protected GoalSelector targetSelector;

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void thc$addThreatTargetGoal(CallbackInfo ci) {
		// Only add to Monster subclasses (hostile mobs)
		if ((Object) this instanceof Monster) {
			// Priority 0 = highest priority, overrides default targeting (priority 1-2)
			this.targetSelector.addGoal(0, new ThreatTargetGoal((Mob) (Object) this));
		}
	}
}
