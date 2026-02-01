package thc.villager;

import net.minecraft.core.BlockPos;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bypass mechanism for BrainPoiMemoryMixin.
 * Allows explicit job assignments to set JOB_SITE memory in claimed chunks.
 *
 * Tracks which positions have player-assigned job sites. Any villager can
 * set JOB_SITE memory at these positions even in claimed chunks.
 */
public final class JobAssignmentBypass {

    /**
     * Set of positions where job site memory is allowed in claimed chunks.
     * When a player places a job block and assigns a villager, the position
     * is added here permanently.
     */
    private static final Set<BlockPos> ALLOWED_POSITIONS = ConcurrentHashMap.newKeySet();

    private JobAssignmentBypass() {}

    /**
     * Allow JOB_SITE memory to be set for this position.
     */
    public static void allowPosition(BlockPos pos) {
        ALLOWED_POSITIONS.add(pos.immutable());
    }

    /**
     * Remove allowance (e.g., when job block is broken).
     */
    public static void removePosition(BlockPos pos) {
        ALLOWED_POSITIONS.remove(pos);
    }

    /**
     * Check if JOB_SITE memory is allowed at this position.
     */
    public static boolean isAllowed(BlockPos pos) {
        return ALLOWED_POSITIONS.contains(pos);
    }
}
