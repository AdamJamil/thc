package thc.mixin.access;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to allow modification of AbstractBoat's private fields.
 * Used by IronBoat to set status for lava floating behavior.
 *
 * Note: getStatus() already exists as a public method in AbstractBoat, so we only need setters.
 */
@Mixin(AbstractBoat.class)
public interface AbstractBoatAccessor {

    // Note: getStatus() is already public in AbstractBoat, no accessor needed

    @Accessor("status")
    void setStatus(AbstractBoat.Status status);

    // Note: getWaterLevel() might be public too, only need setter
    @Accessor("waterLevel")
    void setWaterLevel(double level);
}
