package thc.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freeze game time at night for monster spawning.
 *
 * <p>By returning a fixed night time from getDayTime(), all vanilla systems
 * that depend on time of day will behave as if it's always night:
 * <ul>
 *   <li>Light calculations (skyDarken) will be at night level</li>
 *   <li>Monster spawn checks will pass naturally</li>
 *   <li>No need to bypass individual spawn rules</li>
 * </ul>
 *
 * <p>Only affects Overworld. Client-side visuals are handled separately
 * by ClientLevelTimeMixin which renders perpetual dusk.
 *
 * <p>Time values:
 * <ul>
 *   <li>0 = sunrise (6am)</li>
 *   <li>6000 = noon</li>
 *   <li>12000 = sunset</li>
 *   <li>13000 = dusk</li>
 *   <li>14000 = night begins (monsters spawn)</li>
 *   <li>18000 = midnight</li>
 * </ul>
 */
@Mixin(Level.class)
public abstract class LevelTimeMixin {

    // 14000 = early night, monsters spawn reliably
    private static final long FROZEN_NIGHT_TIME = 14000L;

    /**
     * Override getDayTime() to return frozen night time in Overworld.
     */
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void thc$freezeTimeAtNight(CallbackInfoReturnable<Long> cir) {
        Level self = (Level) (Object) this;
        if (self.dimension() == Level.OVERWORLD) {
            cir.setReturnValue(FROZEN_NIGHT_TIME);
        }
    }
}
