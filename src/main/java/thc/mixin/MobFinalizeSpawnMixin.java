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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.THCAttachments;
import thc.spawn.RegionDetector;

@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void thc$tagSpawnOrigin(
			ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData groupData,
			CallbackInfoReturnable<SpawnGroupData> cir) {

		// Only tag NATURAL spawns (excludes spawners, commands, etc.)
		// Skip CHUNK_GENERATION - heightmap query isn't safe during chunk gen
		if (reason != EntitySpawnReason.NATURAL) {
			return;
		}

		// Only tag Overworld mobs
		ServerLevel serverLevel = (ServerLevel) level.getLevel();
		if (serverLevel.dimension() != Level.OVERWORLD) {
			return;
		}

		Mob self = (Mob) (Object) this;
		String region = RegionDetector.getRegion(serverLevel, self.blockPosition());
		self.setAttached(THCAttachments.SPAWN_REGION, region);

		// Only monsters count toward cap
		boolean isMonster = self.getType().getCategory() == MobCategory.MONSTER;
		self.setAttached(THCAttachments.SPAWN_COUNTED, isMonster);
	}
}
