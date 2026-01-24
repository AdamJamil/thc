package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.THCAttachments;

@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void thc$tagSpawnOrigin(
			ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData groupData,
			CallbackInfoReturnable<SpawnGroupData> cir) {

		// Only tag NATURAL and CHUNK_GENERATION spawns (excludes spawners, commands, etc.)
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
			return;
		}

		// Only tag Overworld mobs
		ServerLevel serverLevel = (ServerLevel) level.getLevel();
		if (serverLevel.dimension() != Level.OVERWORLD) {
			return;
		}

		Mob self = (Mob) (Object) this;
		String region = detectRegion(serverLevel, self.blockPosition());
		self.setAttached(THCAttachments.SPAWN_REGION, region);

		// Only monsters count toward cap
		boolean isMonster = self.getType().getCategory() == MobCategory.MONSTER;
		self.setAttached(THCAttachments.SPAWN_COUNTED, isMonster);
	}

	@Unique
	private static String detectRegion(ServerLevel level, BlockPos pos) {
		int y = pos.getY();

		// Lower cave: below Y=0 (sea level)
		if (y < 0) {
			return "OW_LOWER_CAVE";
		}

		// Surface: Y >= heightmap at X/Z
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		if (y >= surfaceY) {
			return "OW_SURFACE";
		}

		// Upper cave: Y >= 0 but below heightmap
		return "OW_UPPER_CAVE";
	}
}
