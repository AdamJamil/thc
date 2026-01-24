package thc.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * AI goal that makes Endermen aggro on players within 3 blocks proximity.
 * No eye contact required - just being close triggers aggro.
 *
 * This makes Endermen more threatening - players can't safely approach them.
 */
public class EndermanProximityAggroGoal extends TargetGoal {
	private static final double AGGRO_RANGE = 3.0;

	private final EnderMan enderman;

	public EndermanProximityAggroGoal(EnderMan enderman) {
		super(enderman, false); // checkVisibility = false (proximity only)
		this.enderman = enderman;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		// Don't acquire new target if already has one
		if (this.enderman.getTarget() != null) {
			return false;
		}

		// Find nearest player within aggro range
		Player nearestPlayer = this.enderman.level().getNearestPlayer(
			this.enderman.getX(),
			this.enderman.getY(),
			this.enderman.getZ(),
			AGGRO_RANGE,
			true // ignoreInvisible = true (invisible players are safe)
		);

		// No player nearby
		if (nearestPlayer == null) {
			return false;
		}

		// Skip spectators and creative mode players
		if (nearestPlayer.isSpectator() || nearestPlayer.isCreative()) {
			return false;
		}

		// Player is close enough - aggro!
		this.enderman.setTarget(nearestPlayer);
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		// Let vanilla Enderman AI handle de-aggro (player distance, damage, etc.)
		return this.enderman.getTarget() != null;
	}
}
