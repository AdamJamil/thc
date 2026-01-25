package thc.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Shared region detection logic for spawn system.
 *
 * <p>Used by SpawnReplacementMixin (distribution + cap check) and
 * MobFinalizeSpawnMixin (NBT tagging).
 *
 * <p>Uses heightmap-based detection per v2.3 audit recommendation:
 * <ul>
 *   <li>Matches player intuition ("surface" = ground level, not sky visibility)</li>
 *   <li>MOTION_BLOCKING excludes leaves (under-tree = surface, not cave)</li>
 * </ul>
 */
public final class RegionDetector {

	private RegionDetector() {
		// Utility class
	}

	/**
	 * Detect spawn region for a given position.
	 *
	 * <p>Algorithm:
	 * <ul>
	 *   <li>Non-Overworld: null (no regional system)</li>
	 *   <li>Y &lt; 0: OW_LOWER_CAVE</li>
	 *   <li>Y &gt;= MOTION_BLOCKING heightmap: OW_SURFACE</li>
	 *   <li>Otherwise: OW_UPPER_CAVE</li>
	 * </ul>
	 *
	 * @param level The server level
	 * @param pos   The spawn position
	 * @return Region string or null if non-Overworld
	 */
	public static String getRegion(ServerLevel level, BlockPos pos) {
		// Only Overworld has regional spawn system
		if (level.dimension() != Level.OVERWORLD) {
			return null;
		}

		int y = pos.getY();

		// Lower cave: below Y=0 (sea level)
		if (y < 0) {
			return "OW_LOWER_CAVE";
		}

		// Surface: Y >= heightmap at X/Z
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		if (y >= surfaceY) {
			return "OW_SURFACE";
		}

		// Upper cave: Y >= 0 but below heightmap
		return "OW_UPPER_CAVE";
	}
}
