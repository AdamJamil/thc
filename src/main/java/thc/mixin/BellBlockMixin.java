package thc.mixin;

import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes bells indestructible like bedrock.
 *
 * Overrides getDestroyProgress to return 0 (no mining progress).
 * This prevents the mining animation from ever starting.
 */
@Mixin(BellBlock.class)
public abstract class BellBlockMixin {

    /**
     * Override destroy progress to always return 0.
     * This makes the bell appear to have infinite hardness.
     */
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void makeUnbreakable(BlockState state, net.minecraft.world.entity.player.Player player,
                                  net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                                  CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0f);
    }
}
