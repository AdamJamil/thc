package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.claim.ClaimManager;

/**
 * Block POI registration for beds/workstations/bells in claimed chunks.
 *
 * <p>When a POI-eligible block is placed in a claimed chunk, this mixin
 * cancels the POI registration, preventing villagers from seeing it as
 * part of a village.
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

        if (ClaimManager.INSTANCE.isClaimed(self.getServer(), chunkPos)) {
            ci.cancel(); // Block POI registration/removal in claimed chunks
        }
    }
}
