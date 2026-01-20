package thc.threat;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
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

	/**
	 * Decay threat for all players by 1 per second.
	 * Should be called frequently (e.g., in goal canUse/canContinueToUse).
	 * Uses tick-based check to only decay once per second (20 ticks).
	 */
	public static void decayThreat(Mob mob) {
		if (mob.level().isClientSide()) return;

		long now = mob.level().getGameTime();
		long lastDecay = mob.getAttachedOrCreate(THCAttachments.THREAT_LAST_DECAY);

		// Only decay once per second (20 ticks)
		if (now - lastDecay < 20) {
			return;
		}

		Map<UUID, Double> threats = mob.getAttached(THCAttachments.MOB_THREAT);
		if (threats == null || threats.isEmpty()) {
			return;
		}

		// Decay all threat values by 1
		threats.replaceAll((uuid, threat) -> threat - 1.0);

		// Remove entries at or below zero
		threats.entrySet().removeIf(entry -> entry.getValue() <= 0.0);

		mob.setAttached(THCAttachments.THREAT_LAST_DECAY, now);
	}

	/**
	 * Find the player with highest threat that meets minimum threshold.
	 * Returns null if no player meets the threshold or is valid target.
	 */
	public static Player getHighestThreatTarget(Mob mob, double minThreat) {
		Map<UUID, Double> threats = mob.getAttached(THCAttachments.MOB_THREAT);
		if (threats == null || threats.isEmpty()) {
			return null;
		}

		Player highestPlayer = null;
		double highestThreat = minThreat;

		for (Map.Entry<UUID, Double> entry : threats.entrySet()) {
			if (entry.getValue() < minThreat) {
				continue;
			}

			Player player = mob.level().getPlayerByUUID(entry.getKey());
			if (player == null || !player.isAlive() || player.isSpectator()) {
				continue;
			}

			// Check if mob can actually attack this player
			if (!mob.canAttack(player)) {
				continue;
			}

			if (entry.getValue() > highestThreat || highestPlayer == null) {
				highestThreat = entry.getValue();
				highestPlayer = player;
			}
		}

		return highestPlayer;
	}
}
