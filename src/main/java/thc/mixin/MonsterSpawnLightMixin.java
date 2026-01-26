package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypass ALL light checks for hostile mob spawning.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where
 * hostile mobs spawn regardless of light level. Neither sky light nor
 * block light (torches) prevents spawns.
 *
 * <p>Spawn protection comes ONLY from claimed player bases via
 * NaturalSpawnerMixin, not from light.
 */
@Mixin(Monster.class)
public abstract class MonsterSpawnLightMixin {

	/**
	 * Always return true - light never prevents spawns.
	 * Spawn blocking is handled by NaturalSpawnerMixin for claimed chunks.
	 */
	@Inject(
		method = "isDarkEnoughToSpawn",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void thc$bypassAllLightChecks(
			ServerLevelAccessor level,
			BlockPos pos,
			RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}
}
