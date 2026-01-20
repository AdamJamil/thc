package thc.threat;

import net.minecraft.world.entity.Mob;
import thc.THCAttachments;

import java.util.Map;
import java.util.UUID;

public final class ThreatManager {
	private ThreatManager() {
	}

	/**
	 * Add threat from a player to a mob.
	 */
	public static void addThreat(Mob mob, UUID playerUuid, double amount) {
		if (amount <= 0) return;
		Map<UUID, Double> threats = mob.getAttachedOrCreate(THCAttachments.MOB_THREAT);
		double current = threats.getOrDefault(playerUuid, 0.0);
		threats.put(playerUuid, current + amount);
	}

	/**
	 * Get current threat level for a player on a mob.
	 */
	public static double getThreat(Mob mob, UUID playerUuid) {
		Map<UUID, Double> threats = mob.getAttached(THCAttachments.MOB_THREAT);
		if (threats == null) return 0.0;
		return threats.getOrDefault(playerUuid, 0.0);
	}

	/**
	 * Set threat to a specific value.
	 */
	public static void setThreat(Mob mob, UUID playerUuid, double amount) {
		Map<UUID, Double> threats = mob.getAttachedOrCreate(THCAttachments.MOB_THREAT);
		if (amount <= 0) {
			threats.remove(playerUuid);
		} else {
			threats.put(playerUuid, amount);
		}
	}

	/**
	 * Get the threat map for a mob (for iteration).
	 */
	public static Map<UUID, Double> getThreatMap(Mob mob) {
		return mob.getAttachedOrCreate(THCAttachments.MOB_THREAT);
	}

	/**
	 * Check if mob has any threat registered.
	 */
	public static boolean hasThreat(Mob mob) {
		Map<UUID, Double> threats = mob.getAttached(THCAttachments.MOB_THREAT);
		return threats != null && !threats.isEmpty();
	}
}
