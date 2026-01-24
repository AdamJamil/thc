package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thc.spawn.PillagerVariant;
import thc.spawn.RegionalCapManager;
import thc.spawn.SpawnDistributions;

/**
 * Replaces natural spawns with regional custom mobs and surface variants.
 *
 * <p>Spawn replacement priority order:
 * <ol>
 *   <li><b>Base chunk blocking</b> - NaturalSpawnerMixin HEAD on isValidSpawnPostitionForType cancels
 *       spawns in claimed chunks BEFORE this redirect runs</li>
 *   <li><b>Regional distribution roll</b> - This redirect rolls SpawnDistributions.selectMob() to
 *       potentially replace vanilla spawn with custom mob (witch, vex, pillager, blaze, etc.)</li>
 *   <li><b>Surface variant replacement</b> - Only runs if vanilla fallback selected; replaces
 *       surface zombies/skeletons with husks/strays</li>
 * </ol>
 *
 * <p>Region detection uses canSeeSky(pos) per FR-18 spec:
 * <ul>
 *   <li>Any sky visibility = SURFACE (even through small holes)</li>
 *   <li>No sky + Y &lt; 0 = LOWER_CAVE</li>
 *   <li>No sky + Y &gt;= 0 = UPPER_CAVE</li>
 * </ul>
 *
 * <p>Custom spawns bypass vanilla spawn conditions - witches spawn anywhere,
 * blazes/breezes don't need fortresses. Pack spawning [1,4] with position collision checks.
 */
@Mixin(NaturalSpawner.class)
public class SpawnReplacementMixin {

	/**
	 * Equipment slots to copy from original to replacement entity.
	 */
	@Unique
	private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

	/**
	 * Redirect addFreshEntityWithPassengers to apply regional distributions
	 * and surface variant replacements.
	 *
	 * <p>The spawnCategoryForPosition method is where natural spawning occurs.
	 * We intercept the entity being added and potentially replace it based on
	 * regional distribution or surface variant rules.
	 */
	@Redirect(
		method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"
		)
	)
	private static void thc$replaceWithSurfaceVariant(ServerLevel level, Entity entity) {
		// Preserve passenger checks (spider jockey protection)
		if (!entity.getPassengers().isEmpty() || entity.getVehicle() != null) {
			level.addFreshEntityWithPassengers(entity);
			return;
		}

		BlockPos pos = entity.blockPosition();

		// Step 1: Detect region for cap check and distribution
		String region = thc$detectRegion(level, pos);

		// Step 2: Regional cap check - block spawn if cap reached
		// Per spec: three independent caps, no fallback when cap reached
		if (region != null && !RegionalCapManager.canSpawnInRegion(region)) {
			// Regional cap reached - do not spawn this entity
			return;
		}

		// Step 3: Regional distribution roll (Overworld only)
		if (region != null) {
			SpawnDistributions.MobSelection selection = SpawnDistributions.selectMob(region, level.random);

			if (!selection.isVanilla()) {
				// Custom mob selected - spawn pack and skip vanilla entity
				thc$spawnCustomPack(level, selection, pos);
				// Don't spawn the vanilla entity - custom pack replaces it
				return;
			}
		}

		// Step 4: Vanilla fallback - apply surface variant replacement if applicable
		Entity entityToSpawn = thc$getReplacementEntity(level, entity);
		level.addFreshEntityWithPassengers(entityToSpawn);
	}

	/**
	 * Detect region for spawn distribution based on position.
	 * Uses canSeeSky(pos) per FR-18 spec for surface detection.
	 *
	 * @param level The server level
	 * @param pos   The spawn position
	 * @return Region string (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE) or null if non-Overworld
	 */
	@Unique
	private static String thc$detectRegion(ServerLevel level, BlockPos pos) {
		// Only Overworld has custom distributions
		// Nether/End skip regional distribution, use vanilla spawning only
		// End support deferred to Phase 44b (requires separate distribution table)
		if (level.dimension() != Level.OVERWORLD) {
			return null;
		}

		// FIRST check: if can see sky -> SURFACE (per FR-18 isSkyVisible semantics)
		if (level.canSeeSky(pos)) {
			return "OW_SURFACE";
		}

		// Underground: check Y level
		if (pos.getY() < 0) {
			return "OW_LOWER_CAVE";
		}

		return "OW_UPPER_CAVE";
	}

	/**
	 * Spawn a pack of custom mobs at the given position.
	 *
	 * <p>Pack size is [1,4] with uniform distribution. Pack members spawn nearby
	 * with position collision checks. Same mob type/variant for entire pack.
	 *
	 * @param level     The server level
	 * @param selection The mob selection (type and variant)
	 * @param origin    The origin position for the pack
	 */
	@Unique
	private static void thc$spawnCustomPack(
			ServerLevel level,
			SpawnDistributions.MobSelection selection,
			BlockPos origin) {

		// Random pack size [1, 4] uniform distribution
		int packSize = 1 + level.random.nextInt(4);

		// Thread SpawnGroupData through pack members
		SpawnGroupData groupData = null;

		BlockPos currentPos = origin;

		for (int i = 0; i < packSize; i++) {
			// First mob uses origin; additional pack members offset from PREVIOUS position
			if (i > 0) {
				int dx = level.random.nextInt(11) - 5; // -5 to +5
				int dz = level.random.nextInt(11) - 5;
				currentPos = currentPos.offset(dx, 0, dz);
			}

			// Check spawn position is valid for this mob type
			if (!SpawnPlacements.isSpawnPositionOk(selection.type(), level, currentPos)) {
				// Skip this pack member (partial spawns allowed)
				continue;
			}

			// Create the custom mob
			Mob mob = (Mob) selection.type().create(level, EntitySpawnReason.NATURAL);
			if (mob == null) {
				continue;
			}

			// Position the mob (center of block, random yaw)
			mob.snapTo(
				currentPos.getX() + 0.5,
				currentPos.getY(),
				currentPos.getZ() + 0.5,
				level.random.nextFloat() * 360.0f,
				0.0f
			);

			// Apply variant equipment for pillagers BEFORE finalizeSpawn
			// Note: equipment applied after finalizeSpawn's TAIL to avoid being overwritten
			// We apply in finalizeSpawn's populateDefaultEquipmentSlots, then re-apply after

			// Finalize spawn - triggers:
			// 1. Equipment setup via populateDefaultEquipmentSlots
			// 2. NBT tagging (SPAWN_REGION, SPAWN_COUNTED) via MobFinalizeSpawnMixin TAIL inject
			// This ensures custom mobs are tracked in regional spawn caps just like vanilla mobs
			DifficultyInstance difficulty = level.getCurrentDifficultyAt(currentPos);
			groupData = mob.finalizeSpawn(level, difficulty, EntitySpawnReason.NATURAL, groupData);

			// Apply variant equipment AFTER finalizeSpawn (so it's not overwritten)
			if (selection.variant() != null && mob instanceof Pillager pillager) {
				PillagerVariant.valueOf(selection.variant()).applyEquipment(pillager);
			}

			// Add to world
			level.addFreshEntityWithPassengers(mob);
		}
	}

	/**
	 * Determines if the entity should be replaced with a surface variant.
	 * Only applies husk/stray replacement for vanilla fallback spawns on surface.
	 *
	 * @param level  The server level
	 * @param entity The original entity
	 * @return The replacement entity, or the original if no replacement needed
	 */
	@Unique
	private static Entity thc$getReplacementEntity(ServerLevel level, Entity entity) {
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
