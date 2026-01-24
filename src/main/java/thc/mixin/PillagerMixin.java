package thc.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Modifies pillager AI based on equipped weapon variant.
 *
 * <p><b>Why this is needed:</b> Vanilla pillagers have RangedAttackGoal hardcoded,
 * which only fires if the mob holds a bow or crossbow. When a melee pillager
 * (spawned via regional distribution) has an iron sword, it becomes passive -
 * the RangedAttackGoal never activates because there's no ranged weapon, and
 * there's no MeleeAttackGoal to fall back to.
 *
 * <p><b>Detection method:</b> Equipment-based. If mainhand is IRON_SWORD, this is
 * a melee variant (set by PillagerVariant.MELEE in SpawnReplacementMixin).
 * Any other weapon (or no weapon) leaves vanilla AI intact for crossbow use.
 *
 * <p><b>Timing:</b> This mixin injects at finalizeSpawn TAIL, which runs after:
 * <ol>
 *   <li>Mob.populateDefaultEquipmentSlots (vanilla equipment)</li>
 *   <li>PillagerVariant.applyEquipment (THC equipment override from SpawnReplacementMixin)</li>
 *   <li>MobFinalizeSpawnMixin (NBT tagging)</li>
 * </ol>
 * This ensures the iron sword is present when we check, and AI modification
 * happens at the correct lifecycle point.
 *
 * <p><b>AI modification:</b> For melee variants, we remove RangedAttackGoal and
 * add MeleeAttackGoal at the same priority (4). Speed multiplier 1.0 and
 * pauseWhenMobIdle=false ensures aggressive pursuit behavior.
 */
@Mixin(Pillager.class)
public abstract class PillagerMixin {

	@Shadow
	@Final
	protected GoalSelector goalSelector;

	/**
	 * Configure melee AI for iron sword pillager variants.
	 *
	 * <p>Runs at TAIL of finalizeSpawn after equipment is set.
	 * Detects melee variant by checking for iron sword in mainhand.
	 */
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void thc$configureMeleeAI(
			ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData groupData,
			CallbackInfoReturnable<SpawnGroupData> cir) {

		Pillager self = (Pillager) (Object) this;

		// Check if this is a melee variant (iron sword in mainhand)
		if (!self.getMainHandItem().is(Items.IRON_SWORD)) {
			// Not a melee variant - leave vanilla RangedAttackGoal intact
			return;
		}

		// Remove RangedAttackGoal - it won't fire without a ranged weapon anyway,
		// but removing it cleans up the goal selector
		goalSelector.getAvailableGoals().removeIf(
			wrappedGoal -> wrappedGoal.getGoal() instanceof RangedAttackGoal
		);

		// Add MeleeAttackGoal with priority 4 (same as vanilla RangedAttackGoal)
		// - Speed multiplier 1.0: normal movement speed when attacking
		// - pauseWhenMobIdle false: aggressive pursuit, doesn't pause when target stationary
		goalSelector.addGoal(4, new MeleeAttackGoal(self, 1.0, false));
	}
}
