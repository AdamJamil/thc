package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent player-summoned iron golems via pumpkin pattern.
 *
 * Part of THC difficulty - players cannot create iron golems using the
 * traditional pumpkin + iron block pattern. This forces reliance on
 * village-spawned golems for iron golem presence.
 *
 * Note: Villager-summoned golems still work (different code path via
 * Villager.spawnGolem). Existing golems in world are unaffected.
 * Snow golems can still be built (different pattern).
 */
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {

    @Shadow
    private static BlockPattern ironGolemFull;

    /**
     * Intercept golem spawn attempt and block iron golem creation.
     *
     * The trySpawnGolem method is called from onPlace after the pumpkin
     * is placed. It checks for both iron golem and snow golem patterns.
     *
     * We inject at HEAD, check for iron golem pattern ourselves, and if
     * found, cancel without spawning. Snow golem path proceeds normally.
     */
    @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
    private void thc$preventIronGolemSummon(
            Level level,
            BlockPos pos,
            CallbackInfo ci) {

        // Check if iron golem pattern is present
        // If so, cancel (don't spawn) but allow block to remain placed
        // Snow golem will be checked by vanilla if we don't cancel

        if (!level.isClientSide()) {
            BlockPattern.BlockPatternMatch match = getOrCreateIronGolemFull()
                .find(level, pos);
            if (match != null) {
                // Iron golem pattern detected - cancel to prevent spawn
                // The pumpkin stays placed, just no golem spawns
                ci.cancel();
            }
        }
        // If no iron golem pattern, let vanilla continue for snow golem check
    }

    @Shadow
    private static BlockPattern getOrCreateIronGolemFull() {
        throw new AssertionError();
    }
}
