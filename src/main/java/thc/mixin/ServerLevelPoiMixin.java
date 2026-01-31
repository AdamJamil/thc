package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.claim.ClaimManager;
import thc.villager.AllowedProfessions;

/**
 * Block POI registration in two scenarios:
 *
 * <ol>
 *   <li>All POI in claimed chunks (beds, workstations, bells)</li>
 *   <li>Disallowed job blocks everywhere (brewing stand, smithing table, etc.)</li>
 * </ol>
 *
 * <p>This provides defense-in-depth for profession restriction. Even if a villager
 * somehow tried to acquire a disallowed profession, the job site POI wouldn't exist.
 */
@Mixin(ServerLevel.class)
public class ServerLevelPoiMixin {

    @Inject(
        method = "updatePOIOnBlockStateChange",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockPoiInClaimedChunks(
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            CallbackInfo ci) {

        ServerLevel self = (ServerLevel) (Object) this;
        ChunkPos chunkPos = new ChunkPos(pos);

        // Block all POI in claimed chunks
        if (ClaimManager.INSTANCE.isClaimed(self.getServer(), chunkPos)) {
            ci.cancel();
            return;
        }

        // Block disallowed job site POI everywhere
        Block newBlock = newState.getBlock();
        if (AllowedProfessions.isDisallowedJobBlock(newBlock)) {
            ci.cancel();
        }
    }
}
