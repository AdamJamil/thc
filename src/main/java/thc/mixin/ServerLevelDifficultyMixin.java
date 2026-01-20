package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelDifficultyMixin {

	@Inject(method = "getCurrentDifficultyAt", at = @At("HEAD"), cancellable = true)
	private void thc$forceMaxDifficulty(BlockPos pos, CallbackInfoReturnable<DifficultyInstance> cir) {
		ServerLevel self = (ServerLevel) (Object) this;
		// Use maximum inhabited time (3,600,000 ticks = 50 hours, the cap)
		// Use moon phase 1.0f (full moon)
		// DifficultyInstance(difficulty, levelTime, chunkInhabitedTime, moonPhaseFactor)
		DifficultyInstance maxDifficulty = new DifficultyInstance(
			Difficulty.HARD,
			self.getLevelData().getGameTime(),
			3600000L,  // Max inhabited time
			1.0f       // Full moon
		);
		cir.setReturnValue(maxDifficulty);
	}
}
