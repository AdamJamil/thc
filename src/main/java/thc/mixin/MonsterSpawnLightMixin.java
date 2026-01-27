package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
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
 * <p>This mixin bypasses ALL spawn rule methods that check light:
 * <ol>
 *   <li>isDarkEnoughToSpawn - sky light and block light checks</li>
 *   <li>checkMonsterSpawnRules - standard monsters (zombie, skeleton, creeper, etc.)</li>
 *   <li>checkAnyLightMonsterSpawnRules - Blaze, Breeze, Zoglin</li>
 *   <li>checkSurfaceMonstersSpawnRules - Husk, Parched</li>
 * </ol>
 *
 * <p>Spawn protection comes ONLY from claimed player bases via
 * NaturalSpawnerMixin, not from light.
 */
@Mixin(Monster.class)
public abstract class MonsterSpawnLightMixin {

	/**
	 * Bypass ALL light checks - just return true unconditionally.
	 */
	@Inject(method = "isDarkEnoughToSpawn", at = @At("HEAD"), cancellable = true)
	private static void thc$bypassDarkCheck(
			ServerLevelAccessor level,
			BlockPos pos,
			RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}

	/**
	 * Bypass monster spawn rules - only check difficulty, skip ALL light logic.
	 * Don't call Mob.checkMobSpawnRules as it may have hidden checks.
	 */
	@Inject(method = "checkMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void thc$bypassMonsterRules(
			EntityType<? extends Mob> entityType,
			ServerLevelAccessor level,
			EntitySpawnReason reason,
			BlockPos pos,
			RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}

	/**
	 * Bypass checkAnyLightMonsterSpawnRules for Blaze/Breeze/Zoglin.
	 */
	@Inject(method = "checkAnyLightMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void thc$bypassAnyLightRules(
			EntityType<? extends Monster> entityType,
			LevelAccessor level,
			EntitySpawnReason reason,
			BlockPos pos,
			RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}

	/**
	 * Bypass checkSurfaceMonstersSpawnRules for Husk/Parched.
	 * Removes the canSeeSky requirement - husks can spawn underground too.
	 */
	@Inject(method = "checkSurfaceMonstersSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void thc$bypassSurfaceRules(
			EntityType<? extends Mob> entityType,
			ServerLevelAccessor level,
			EntitySpawnReason reason,
			BlockPos pos,
			RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}
}
