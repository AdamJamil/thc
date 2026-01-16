package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes bells indestructible like bedrock.
 *
 * Injects into BlockBehaviour.getDestroyProgress to return 0 for bells.
 * This prevents the mining animation from ever starting.
 */
@Mixin(BlockBehaviour.class)
public abstract class BellBlockMixin {

    /**
     * Override destroy progress to return 0 for bells.
     * This makes bells appear to have infinite hardness.
     */
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void thc$makeBellsUnbreakable(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                           CallbackInfoReturnable<Float> cir) {
        if (state.is(Blocks.BELL)) {
            cir.setReturnValue(0.0f);
        }
    }
}
