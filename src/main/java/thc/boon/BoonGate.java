package thc.boon;

import net.minecraft.server.level.ServerPlayer;
import thc.playerclass.ClassManager;
import thc.stage.StageManager;

/**
 * Utility for checking class-specific boon gates.
 * Used by features that unlock at specific stage thresholds.
 */
public final class BoonGate {
	private BoonGate() {}

	/**
	 * Check if player has Stage 3+ boon for Bastion class.
	 * Used for: parry threat propagation, sweeping edge.
	 * @param player The player to check
	 * @return true if player is Bastion with boon level >= 3
	 */
	public static boolean hasStage3Boon(ServerPlayer player) {
		if (!ClassManager.isBastion(player)) {
			return false;
		}
		return StageManager.getBoonLevel(player) >= 3;
	}
}
