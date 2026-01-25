package thc.mixin.access;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose Entity's protected fire visual flag methods.
 * Used to clear the fire visual when passengers are protected in iron boats.
 */
@Mixin(Entity.class)
public interface EntityAccessor {

    /**
     * Invokes the protected setSharedFlagOnFire method to clear/set the fire visual flag.
     * This is different from clearFire() which only clears remainingFireTicks.
     */
    @Invoker("setSharedFlagOnFire")
    void invokeSetSharedFlagOnFire(boolean onFire);
}
