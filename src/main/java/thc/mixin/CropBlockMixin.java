package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
	@Shadow
	public abstract int getMaxAge();

	@Shadow
	public abstract BlockState getStateForAge(int age);

	/**
	 * Make bonemeal fully mature crops in one use.
	 * Intercepts performBonemeal and sets to max age instead of random growth.
	 */
	@Inject(
		method = "performBonemeal",
		at = @At("HEAD"),
		cancellable = true
	)
	private void thc$instantGrowth(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
		level.setBlock(pos, this.getStateForAge(this.getMaxAge()), 2);
		ci.cancel();
	}
}
