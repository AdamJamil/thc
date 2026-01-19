package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Blocks XP orb spawning from fishing.
 *
 * <p>THC enforces that XP should only come from combat (killing mobs) or
 * rare/expensive items (experience bottles). Fishing no longer awards XP.
 *
 * <p>Uses @Redirect to intercept ExperienceOrb.award() call within
 * the retrieve() method and make it a no-op. Complete cancellation -
 * no XP orbs spawn at all when reeling in a catch.
 */
@Mixin(FishingHook.class)
public abstract class FishingXpMixin {

	/**
	 * Redirects the ExperienceOrb.award() call in retrieve() to do nothing.
	 *
	 * <p>This intercepts the fishing XP award when player reels in a catch.
	 *
	 * @param level The server level
	 * @param position The position where XP would spawn
	 * @param amount The XP amount that would have been awarded
	 */
	@Redirect(
		method = "retrieve",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"
		)
	)
	private void thc$blockFishingXp(ServerLevel level, Vec3 position, int amount) {
		// No-op: completely cancel XP orb spawning from fishing
	}
}
