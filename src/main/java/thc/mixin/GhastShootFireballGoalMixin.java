package thc.mixin;

import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * Modify Ghast fire rate from 3 seconds to 4 seconds.
 *
 * <p>FR-08: Ghast fires every 4 seconds on average (not 3).
 *
 * <p>Vanilla GhastShootFireballGoal resets chargeTime to -40 after firing.
 * Total cycle: -40 → 20 = 60 ticks (3 seconds).
 * THC changes reset to -60 for 80 tick cycle (4 seconds).
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
public abstract class GhastShootFireballGoalMixin {

    /**
     * Change chargeTime reset value from -40 to -60.
     *
     * <p>This increases the fire interval from 60 ticks to 80 ticks.
     */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = -40))
    private int thc$modifyChargeTimeReset(int original) {
        return -60;  // 80 tick cycle: -60 → 20 = 80 ticks (4 seconds)
    }
}
