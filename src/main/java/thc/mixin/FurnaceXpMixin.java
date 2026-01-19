package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Blocks XP orb spawning from furnace smelting.
 *
 * <p>THC enforces that XP should only come from combat (killing mobs) or
 * rare/expensive items (experience bottles). Taking items from furnace output
 * no longer awards XP.
 *
 * <p>Uses @Redirect to intercept ExperienceOrb.award() call within
 * createExperience and make it a no-op. Complete cancellation -
 * no XP orbs spawn at all when removing smelted items.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceXpMixin {

	/**
	 * Redirects the ExperienceOrb.award() call in createExperience to do nothing.
	 *
	 * <p>This intercepts the furnace XP award when player removes smelted items.
	 *
	 * @param level The server level
	 * @param position The position where XP would spawn
	 * @param amount The XP amount that would have been awarded
	 */
	@Redirect(
		method = "createExperience",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"
		)
	)
	private static void thc$blockFurnaceXp(ServerLevel level, Vec3 position, int amount) {
		// No-op: completely cancel XP orb spawning from furnace
	}
}
