package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.claim.ClaimManager;
import thc.spawn.RegionalCapManager;

import java.util.List;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

	/**
	 * Block natural mob spawning in claimed base chunks.
	 *
	 * <p>THC bases are safe zones where mobs should not naturally spawn.
	 * This does NOT affect spawners or spawn eggs, only natural spawning.
	 */
	@Inject(
		method = "isValidSpawnPostitionForType",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void thc$blockSpawnInBaseChunks(
			ServerLevel level,
			MobCategory category,
			StructureManager structureManager,
			ChunkGenerator generator,
			MobSpawnSettings.SpawnerData spawnerData,
			BlockPos.MutableBlockPos pos,
			double squaredDistance,
			CallbackInfoReturnable<Boolean> cir) {

		// Check if this chunk is claimed
		ChunkPos chunkPos = new ChunkPos(pos);
		if (ClaimManager.INSTANCE.isClaimed(level.getServer(), chunkPos)) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * Initialize regional mob counts at start of spawn cycle.
	 *
	 * <p>Counts SPAWN_COUNTED=true monsters grouped by SPAWN_REGION.
	 * Only runs for Overworld monster spawning to populate ThreadLocal counts
	 * used by subsequent canSpawnInRegion checks in SpawnReplacementMixin.
	 */
	@Inject(
		method = "spawnForChunk",
		at = @At("HEAD")
	)
	private static void thc$initRegionalCounts(
			ServerLevel level,
			LevelChunk chunk,
			NaturalSpawner.SpawnState state,
			List<MobCategory> categories,
			CallbackInfo ci) {

		// Only count for Overworld when monsters are being spawned
		if (level.dimension() == Level.OVERWORLD && categories.contains(MobCategory.MONSTER)) {
			RegionalCapManager.countMobsByRegion(level);
		}
	}

	/**
	 * Clear regional counts after spawn cycle completes.
	 *
	 * <p>Removes ThreadLocal storage to prevent memory leaks.
	 * Always clears regardless of dimension to ensure cleanup.
	 */
	@Inject(
		method = "spawnForChunk",
		at = @At("RETURN")
	)
	private static void thc$clearRegionalCounts(
			ServerLevel level,
			LevelChunk chunk,
			NaturalSpawner.SpawnState state,
			List<MobCategory> categories,
			CallbackInfo ci) {

		RegionalCapManager.clearCounts();
	}
}
