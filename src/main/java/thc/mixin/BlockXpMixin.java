package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks XP orb spawning from ore mining.
 *
 * <p>THC enforces that XP should only come from combat (killing mobs) or
 * rare/expensive items (experience bottles). Mining ores like coal, lapis,
 * redstone, emerald, diamond, and nether quartz no longer awards XP.
 *
 * <p>Uses complete cancellation approach - no XP orbs spawn at all when
 * breaking these blocks, rather than spawning zero-value orbs.
 */
@Mixin(Block.class)
public abstract class BlockXpMixin {

	/**
	 * Cancels XP orb spawning from block breaking.
	 *
	 * <p>This method is called by OreBlock subclasses when broken. Canceling it
	 * prevents XP from coal, lapis, redstone, emerald, diamond, and nether quartz ores.
	 *
	 * @param level The server level
	 * @param pos The block position
	 * @param amount The XP amount that would have spawned
	 * @param ci Callback info for cancellation
	 */
	@Inject(method = "popExperience", at = @At("HEAD"), cancellable = true)
	private void thc$blockOreXp(ServerLevel level, BlockPos pos, int amount, CallbackInfo ci) {
		// Completely cancel XP orb spawning from block breaking
		ci.cancel();
	}
}
