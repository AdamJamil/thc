package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Blocks XP orb spawning from animal breeding.
 *
 * <p>THC enforces that XP should only come from combat (killing mobs) or
 * rare/expensive items (experience bottles). Breeding animals no longer awards XP.
 *
 * <p>Uses @Redirect to intercept ExperienceOrb.award() call within
 * finalizeSpawnChildFromBreeding and make it a no-op. Complete cancellation -
 * no XP orbs spawn at all.
 */
@Mixin(Animal.class)
public abstract class AnimalBreedingXpMixin {

	/**
	 * Redirects the ExperienceOrb.award() call in finalizeSpawnChildFromBreeding to do nothing.
	 *
	 * <p>This intercepts the breeding XP award and prevents any orbs from spawning.
	 *
	 * @param level The server level
	 * @param position The position where XP would spawn
	 * @param amount The XP amount that would have been awarded
	 */
	@Redirect(
		method = "finalizeSpawnChildFromBreeding",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"
		)
	)
	private void thc$blockBreedingXp(ServerLevel level, Vec3 position, int amount) {
		// No-op: completely cancel XP orb spawning from breeding
	}
}
