package thc.threat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * AI goal that targets players based on threat levels.
 * Priority 0 (highest) to override default targeting.
 *
 * Targeting rules (THREAT-05, THREAT-06):
 * - Target highest-threat player when threat >= 5
 * - Only switch targets on:
 *   1. Revenge strike (mob was just hurt by different player)
 *   2. Another player gains strictly higher threat
 */
public class ThreatTargetGoal extends TargetGoal {
	private static final double MIN_THREAT = 5.0;

	private Player currentTarget;

	public ThreatTargetGoal(Mob mob) {
		super(mob, false); // checkVisibility = false (threat overrides LoS)
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		// Decay threat before checking (lazy decay)
		ThreatManager.decayThreat(this.mob);

		// Find highest threat target
		Player target = ThreatManager.getHighestThreatTarget(this.mob, MIN_THREAT);
		if (target != null) {
			this.currentTarget = target;
			return true;
		}
		return false;
	}

	@Override
	public void start() {
		this.mob.setTarget(this.currentTarget);
		super.start();
	}

	@Override
	public boolean canContinueToUse() {
		if (this.currentTarget == null || !this.currentTarget.isAlive() || this.currentTarget.isSpectator()) {
			return false;
		}

		// Decay threat
		ThreatManager.decayThreat(this.mob);

		// Check if current target still has sufficient threat
		double currentThreat = ThreatManager.getThreat(this.mob, this.currentTarget.getUUID());
		if (currentThreat < MIN_THREAT) {
			// Current target lost threat, find new target
			Player newTarget = ThreatManager.getHighestThreatTarget(this.mob, MIN_THREAT);
			if (newTarget != null) {
				this.currentTarget = newTarget;
				this.mob.setTarget(this.currentTarget);
				return true;
			}
			return false;
		}

		// Check for revenge switch (THREAT-06: revenge allows immediate switch)
		LivingEntity revenge = this.mob.getLastHurtByMob();
		if (revenge instanceof Player revengePlayer && revengePlayer != this.currentTarget) {
			double revengeThreat = ThreatManager.getThreat(this.mob, revengePlayer.getUUID());
			if (revengeThreat >= MIN_THREAT) {
				this.currentTarget = revengePlayer;
				this.mob.setTarget(this.currentTarget);
				return true;
			}
		}

		// Check for strictly higher threat (THREAT-06: only switch if strictly higher)
		Player highestTarget = ThreatManager.getHighestThreatTarget(this.mob, MIN_THREAT);
		if (highestTarget != null && highestTarget != this.currentTarget) {
			double highestThreat = ThreatManager.getThreat(this.mob, highestTarget.getUUID());
			if (highestThreat > currentThreat) { // Strictly greater
				this.currentTarget = highestTarget;
				this.mob.setTarget(this.currentTarget);
			}
		}

		return this.mob.canAttack(this.currentTarget);
	}

	@Override
	public void stop() {
		this.currentTarget = null;
		// Don't clear mob target - let lower priority goals handle it
		super.stop();
	}
}
