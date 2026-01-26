package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypass sky light checks for hostile mob spawning.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where
 * hostile mobs spawn during daytime just as they do at night. Sky light
 * no longer prevents spawns, but block light from torches and lamps
 * still protects areas.
 *
 * <p>This complements NaturalSpawnerMixin which handles base chunk
 * spawn blocking. Together they create a world where mobs spawn
 * everywhere except in player-claimed safe zones.
 */
@Mixin(Monster.class)
public abstract class MonsterSpawnLightMixin {

	/**
	 * Intercept spawn light check to bypass sky light while preserving block light.
	 *
	 * <p>The vanilla isDarkEnoughToSpawn method checks:
	 * <ol>
	 *   <li>SKY light - if too high, blocks spawn (we bypass this)</li>
	 *   <li>BLOCK light - if above dimension limit, blocks spawn (we preserve this)</li>
	 *   <li>Overall brightness test - dimension-specific threshold (we preserve this)</li>
	 * </ol>
	 *
	 * <p>By injecting at HEAD and handling the logic ourselves, we skip the sky
	 * light check while keeping block light protection functional.
	 */
	@Inject(
		method = "isDarkEnoughToSpawn",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void thc$bypassSkyLightCheck(
			ServerLevelAccessor level,
			BlockPos pos,
			RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {

		// Skip sky light check entirely - mobs can spawn in daylight

		// Preserve block light check - torches still protect areas
		DimensionType dimensionType = level.dimensionType();
		int blockLightLimit = dimensionType.monsterSpawnBlockLightLimit();
		if (blockLightLimit < 15 && level.getBrightness(LightLayer.BLOCK, pos) > blockLightLimit) {
			cir.setReturnValue(false);
			return;
		}

		// Use ONLY block light for final check (not getMaxLocalRawBrightness which includes sky)
		// This allows daytime spawns while still letting torches prevent spawns
		int blockLight = level.getBrightness(LightLayer.BLOCK, pos);

		// The monsterSpawnLightTest is dimension-specific randomized threshold
		cir.setReturnValue(blockLight <= dimensionType.monsterSpawnLightTest().sample(random));
	}
}
