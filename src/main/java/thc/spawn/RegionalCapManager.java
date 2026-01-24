package thc.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import thc.THCAttachments;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages regional monster caps for independent Overworld zone spawn limits.
 *
 * <p>Three independent caps prevent any single zone from consuming all monster capacity:
 * <ul>
 *   <li>Surface: 21 mobs (30% of 70)</li>
 *   <li>Upper Cave (Y >= 0): 28 mobs (40% of 70)</li>
 *   <li>Lower Cave (Y < 0): 35 mobs (50% of 70)</li>
 * </ul>
 *
 * <p>Each region operates independently - surface cap doesn't affect cave spawns.
 * Only mobs with SPAWN_COUNTED=true attachment contribute to regional counts.
 *
 * <p>ThreadLocal storage ensures thread-safe counting per spawn cycle.
 * Counts are populated at spawn cycle start (NaturalSpawner.spawnForChunk HEAD)
 * and cleared at cycle end (RETURN).
 */
public final class RegionalCapManager {

	/**
	 * Per-cycle regional mob counts. ThreadLocal ensures thread safety and
	 * automatic cleanup when thread completes.
	 */
	private static final ThreadLocal<Map<String, Integer>> REGIONAL_COUNTS =
		ThreadLocal.withInitial(HashMap::new);

	/**
	 * Hard-coded regional caps per user decision.
	 * Each region is independent - no global cap applies.
	 */
	private static final Map<String, Integer> REGIONAL_CAPS = Map.of(
		"OW_SURFACE", 21,      // 30% of 70
		"OW_UPPER_CAVE", 28,   // 40% of 70
		"OW_LOWER_CAVE", 35    // 50% of 70
	);

	private RegionalCapManager() {
		// Utility class
	}

	/**
	 * Count all SPAWN_COUNTED monsters by region in the given level.
	 *
	 * <p>Iterates all entities, filters to MONSTER category mobs with
	 * SPAWN_COUNTED=true, and groups by SPAWN_REGION attachment.
	 *
	 * <p>Results stored in ThreadLocal for subsequent canSpawnInRegion checks.
	 *
	 * @param level The server level to count mobs in
	 */
	public static void countMobsByRegion(ServerLevel level) {
		Map<String, Integer> counts = REGIONAL_COUNTS.get();
		counts.clear();

		for (Entity entity : level.getAllEntities()) {
			// Filter: must be a Mob
			if (!(entity instanceof Mob mob)) {
				continue;
			}

			// Filter: must be MONSTER category
			if (mob.getType().getCategory() != MobCategory.MONSTER) {
				continue;
			}

			// Filter: must have SPAWN_COUNTED=true
			Boolean spawnCounted = mob.getAttached(THCAttachments.SPAWN_COUNTED);
			if (spawnCounted == null || !spawnCounted) {
				continue;
			}

			// Group by region
			String region = mob.getAttached(THCAttachments.SPAWN_REGION);
			if (region != null) {
				counts.merge(region, 1, Integer::sum);
			}
		}
	}

	/**
	 * Check if spawning is allowed in the given region based on regional cap.
	 *
	 * <p>Returns true (allow spawn) if:
	 * <ul>
	 *   <li>Region is null (non-Overworld uses vanilla caps)</li>
	 *   <li>Region not in cap map (unknown region)</li>
	 *   <li>Current count is below the cap</li>
	 * </ul>
	 *
	 * @param region The region string (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE) or null
	 * @return true if spawning is allowed, false if regional cap reached
	 */
	public static boolean canSpawnInRegion(String region) {
		// Non-Overworld (null region) bypasses regional caps
		if (region == null) {
			return true;
		}

		// Unknown region (not in cap map) allows spawn
		Integer cap = REGIONAL_CAPS.get(region);
		if (cap == null) {
			return true;
		}

		// Check count against cap
		int count = REGIONAL_COUNTS.get().getOrDefault(region, 0);
		return count < cap;
	}

	/**
	 * Clear regional counts after spawn cycle completes.
	 *
	 * <p>Called at RETURN of NaturalSpawner.spawnForChunk to prevent
	 * ThreadLocal memory leaks.
	 */
	public static void clearCounts() {
		REGIONAL_COUNTS.remove();
	}
}
