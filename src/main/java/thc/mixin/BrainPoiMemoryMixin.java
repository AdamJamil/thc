package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.claim.ClaimManager;
import thc.village.ServerHolder;
import thc.villager.JobAssignmentBypass;

/**
 * Block villagers from storing POI memories for locations in claimed chunks.
 *
 * <p>Intercepts Brain.setMemory to check if the value being stored is a
 * GlobalPos (used for HOME, JOB_SITE, MEETING_POINT memories). If the
 * position is in a claimed chunk, the memory setting is cancelled.
 *
 * <p>Use {@link JobAssignmentBypass#allowPosition(BlockPos)} to bypass this check
 * for explicit player-triggered job assignments.
 */
@Mixin(Brain.class)
public class BrainPoiMemoryMixin {

    /**
     * Block POI memory storage when the target position is in a claimed chunk.
     *
     * <p>Only blocks GlobalPos values (POI locations). Other memory types pass through.
     * Uses ServerHolder to access server instance set during mod initialization.
     */
    @Inject(
        method = "setMemory(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <U> void thc$blockPoiClaimInClaimedChunks(
            MemoryModuleType<U> type,
            U value,
            CallbackInfo ci) {

        // Only process GlobalPos values (HOME, JOB_SITE, MEETING_POINT use GlobalPos)
        if (!(value instanceof GlobalPos globalPos)) {
            return;
        }

        // Check position-based bypass (for explicit job assignments)
        if (JobAssignmentBypass.isAllowed(globalPos.pos())) {
            return; // Allow through - this position was explicitly assigned by player
        }

        // Check if this is a POI-related memory type
        if (type != MemoryModuleType.HOME &&
            type != MemoryModuleType.JOB_SITE &&
            type != MemoryModuleType.POTENTIAL_JOB_SITE &&
            type != MemoryModuleType.MEETING_POINT) {
            return;
        }

        // Get server reference (null on client or before server starts)
        MinecraftServer server = ServerHolder.INSTANCE.getServer();
        if (server == null) {
            return; // Client-side or no server context, allow through
        }

        BlockPos pos = globalPos.pos();
        ChunkPos chunkPos = new ChunkPos(pos);

        if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
            ci.cancel(); // Block memory storage for POI in claimed chunks
        }
    }
}
