package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces natural surface zombie/skeleton spawns with husks/strays.
 *
 * <p>THC increases surface difficulty by replacing basic undead with their
 * more dangerous variants:
 * <ul>
 *   <li>Zombies on surface -> Husks (apply hunger effect)</li>
 *   <li>Skeletons on surface -> Strays (apply slowness effect)</li>
 * </ul>
 *
 * <p>Exceptions preserved:
 * <ul>
 *   <li>Underground spawns (no sky visibility) - remain vanilla</li>
 *   <li>Spider jockey skeletons - detected via passenger check</li>
 *   <li>Spawner/structure spawns - use different code paths, not affected</li>
 * </ul>
 */
@Mixin(NaturalSpawner.class)
public class SpawnReplacementMixin {

	/**
	 * Equipment slots to copy from original to replacement entity.
	 */
	@Unique
	private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

	/**
	 * Redirect addFreshEntityWithPassengers to conditionally replace
	 * zombies/skeletons with husks/strays on surface spawns.
	 *
	 * <p>The spawnCategoryForPosition method is where natural spawning occurs.
	 * We intercept the entity being added and replace it if conditions are met.
	 */
	@Redirect(
		method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"
		)
	)
	private static void thc$replaceWithSurfaceVariant(ServerLevel level, Entity entity) {
		// Check if entity should be replaced
		Entity entityToSpawn = thc$getReplacementEntity(level, entity);
		level.addFreshEntityWithPassengers(entityToSpawn);
	}

	/**
	 * Determines if the entity should be replaced with a surface variant.
	 *
	 * @param level The server level
	 * @param entity The original entity
	 * @return The replacement entity, or the original if no replacement needed
	 */
	@Unique
	private static Entity thc$getReplacementEntity(ServerLevel level, Entity entity) {
		// Skip replacement if entity has passengers (spider jockey spider)
		// or is a passenger (would be the skeleton on a spider - but this case
		// doesn't occur since passengers are added via finalizeSpawn)
		if (!entity.getPassengers().isEmpty() || entity.getVehicle() != null) {
			return entity;
		}

		BlockPos pos = entity.blockPosition();

		// Only replace if surface (can see sky)
		if (!level.canSeeSky(pos)) {
			return entity;
		}

		// Check entity type and create replacement
		// Use EntityType comparison per MC 1.21.11 pattern (not instanceof)
		EntityType<?> type = entity.getType();

		if (type == EntityType.ZOMBIE) {
			return thc$createHuskReplacement(level, (Zombie) entity);
		}

		if (type == EntityType.SKELETON) {
			return thc$createStrayReplacement(level, (AbstractSkeleton) entity);
		}

		// No replacement needed
		return entity;
	}

	/**
	 * Creates a Husk replacement for a Zombie, copying relevant entity data.
	 */
	@Unique
	private static Mob thc$createHuskReplacement(ServerLevel level, Zombie original) {
		Husk husk = EntityType.HUSK.create(level, EntitySpawnReason.NATURAL);
		if (husk == null) {
			return original; // Fallback to original if creation fails
		}

		// Copy position and rotation
		husk.snapTo(
			original.getX(),
			original.getY(),
			original.getZ(),
			original.getYRot(),
			original.getXRot()
		);

		// Copy baby status
		if (original.isBaby()) {
			husk.setBaby(true);
		}

		// Copy equipment
		for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
			husk.setItemSlot(slot, original.getItemBySlot(slot).copy());
		}

		return husk;
	}

	/**
	 * Creates a Stray replacement for a Skeleton, copying relevant entity data.
	 */
	@Unique
	private static Mob thc$createStrayReplacement(ServerLevel level, AbstractSkeleton original) {
		Stray stray = EntityType.STRAY.create(level, EntitySpawnReason.NATURAL);
		if (stray == null) {
			return original; // Fallback to original if creation fails
		}

		// Copy position and rotation
		stray.snapTo(
			original.getX(),
			original.getY(),
			original.getZ(),
			original.getYRot(),
			original.getXRot()
		);

		// Copy equipment
		for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
			stray.setItemSlot(slot, original.getItemBySlot(slot).copy());
		}

		return stray;
	}
}
