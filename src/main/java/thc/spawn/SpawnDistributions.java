package thc.spawn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regional spawn distribution tables with weighted random selection.
 *
 * <p>Each Overworld region has a distribution table defining custom mob spawn
 * probabilities. When a spawn attempt occurs, the table is consulted to determine
 * whether to spawn a custom mob (witch, vex, pillager, etc.) or allow vanilla
 * spawn behavior.
 *
 * <p>Custom spawns bypass vanilla spawn conditions - witches spawn anywhere,
 * blazes/breezes don't need fortresses. This is intentional per design spec.
 */
public final class SpawnDistributions {

	/**
	 * A weighted entry in a spawn distribution table.
	 *
	 * @param type    The entity type to spawn, or null for vanilla fallback
	 * @param variant The variant string (e.g., "MELEE" or "RANGED" for pillagers), or null
	 * @param weight  The weight for weighted random selection (percentages summing to 100)
	 */
	public record WeightedEntry(EntityType<?> type, String variant, int weight) {
		/**
		 * Create a weighted entry for a standard mob (no variant).
		 */
		public WeightedEntry(EntityType<?> type, int weight) {
			this(type, null, weight);
		}
	}

	/**
	 * Result of a spawn distribution selection.
	 *
	 * @param type      The entity type to spawn, or null for vanilla
	 * @param variant   The variant string for pillagers, or null
	 * @param isVanilla True if vanilla mob should spawn (type is null)
	 */
	public record MobSelection(EntityType<?> type, String variant, boolean isVanilla) {
		/**
		 * Create a vanilla fallback selection.
		 */
		public static MobSelection vanillaFallback() {
			return new MobSelection(null, null, true);
		}
	}

	/**
	 * Distribution tables by region name.
	 * Keys: OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE
	 */
	private static final Map<String, List<WeightedEntry>> TABLES = new HashMap<>();

	static {
		// OW_SURFACE: 5% witch, 95% vanilla fallback
		List<WeightedEntry> surface = new ArrayList<>();
		surface.add(new WeightedEntry(EntityType.WITCH, 5));
		surface.add(new WeightedEntry(null, null, 95)); // vanilla fallback
		TABLES.put("OW_SURFACE", surface);

		// OW_UPPER_CAVE: 5% witch, 2% vex, 10% pillager ranged, 25% pillager melee, 58% vanilla
		List<WeightedEntry> upperCave = new ArrayList<>();
		upperCave.add(new WeightedEntry(EntityType.WITCH, 5));
		upperCave.add(new WeightedEntry(EntityType.VEX, 2));
		upperCave.add(new WeightedEntry(EntityType.PILLAGER, "RANGED", 10));
		upperCave.add(new WeightedEntry(EntityType.PILLAGER, "MELEE", 25));
		upperCave.add(new WeightedEntry(null, null, 58)); // vanilla fallback
		TABLES.put("OW_UPPER_CAVE", upperCave);

		// OW_LOWER_CAVE: 8% blaze, 8% breeze, 12% vindicator, 25% pillager melee, 2% evoker, 45% vanilla
		List<WeightedEntry> lowerCave = new ArrayList<>();
		lowerCave.add(new WeightedEntry(EntityType.BLAZE, 8));
		lowerCave.add(new WeightedEntry(EntityType.BREEZE, 8));
		lowerCave.add(new WeightedEntry(EntityType.VINDICATOR, 12));
		lowerCave.add(new WeightedEntry(EntityType.PILLAGER, "MELEE", 25));
		lowerCave.add(new WeightedEntry(EntityType.EVOKER, 2));
		lowerCave.add(new WeightedEntry(null, null, 45)); // vanilla fallback
		TABLES.put("OW_LOWER_CAVE", lowerCave);

		// Validate each table sums to 100
		for (var entry : TABLES.entrySet()) {
			int total = entry.getValue().stream().mapToInt(WeightedEntry::weight).sum();
			if (total != 100) {
				throw new IllegalStateException("Distribution for " + entry.getKey() +
					" sums to " + total + ", expected 100");
			}
		}
	}

	private SpawnDistributions() {
	}

	/**
	 * Select a mob type for the given region using weighted random selection.
	 *
	 * @param region The region name (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE)
	 * @param random The random source for selection
	 * @return The selected mob, or vanilla fallback if region unknown or vanilla selected
	 */
	public static MobSelection selectMob(String region, RandomSource random) {
		List<WeightedEntry> table = TABLES.get(region);
		if (table == null || table.isEmpty()) {
			return MobSelection.vanillaFallback();
		}

		int roll = random.nextInt(100); // 0-99
		int cumulative = 0;

		for (WeightedEntry entry : table) {
			cumulative += entry.weight();
			if (roll < cumulative) {
				if (entry.type() == null) {
					return MobSelection.vanillaFallback();
				} else {
					return new MobSelection(entry.type(), entry.variant(), false);
				}
			}
		}

		// Should never reach here if tables sum to 100, but fallback anyway
		return MobSelection.vanillaFallback();
	}
}
