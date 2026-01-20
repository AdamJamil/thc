package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.claim.ClaimManager;

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
}
