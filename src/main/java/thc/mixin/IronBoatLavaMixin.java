package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.entity.IronBoat;
import thc.mixin.access.AbstractBoatAccessor;

/**
 * Mixin to make IronBoat float on lava by injecting into the private checkInWater method.
 * Since checkInWater is private in AbstractBoat, we inject at the AbstractBoat level
 * and check if the instance is an IronBoat.
 *
 * KEY FIX: Also sets the status and waterLevel fields that floatBoat() uses.
 * Without setting these fields, just changing the return value doesn't make the boat float.
 */
@Mixin(AbstractBoat.class)
public abstract class IronBoatLavaMixin {

    /**
     * After checkInWater checks for water, also check for lava.
     * If we find lava, set status to IN_WATER and waterLevel to lava height.
     * This makes floatBoat() apply buoyancy correctly.
     */
    @Inject(method = "checkInWater", at = @At("RETURN"), cancellable = true)
    private void thc$checkInLava(CallbackInfoReturnable<Boolean> cir) {
        // Only apply to IronBoat instances
        if (!((Object) this instanceof IronBoat)) {
            return;
        }

        // If already found water, no need to check lava
        if (cir.getReturnValue()) {
            return;
        }

        // Cast to get access to entity methods and accessor
        AbstractBoat self = (AbstractBoat) (Object) this;
        AbstractBoatAccessor accessor = (AbstractBoatAccessor) this;
        AABB aabb = self.getBoundingBox();

        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX);
        int minY = Mth.floor(aabb.minY);
        int maxY = Mth.ceil(aabb.maxY);
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ);

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        double maxLavaLevel = Double.MIN_VALUE;
        boolean foundLava = false;

        // Scan the boat's bounding box for lava and find highest lava level
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = self.level().getFluidState(blockPos);

                    if (fluidState.is(FluidTags.LAVA)) {
                        float fluidHeight = (float) y + fluidState.getHeight(self.level(), blockPos);
                        if (fluidHeight >= aabb.minY) {
                            foundLava = true;
                            if (fluidHeight > maxLavaLevel) {
                                maxLavaLevel = fluidHeight;
                            }
                        }
                    }
                }
            }
        }

        if (foundLava) {
            // KEY: Set the internal fields that floatBoat() uses
            accessor.setStatus(AbstractBoat.Status.IN_WATER);
            accessor.setWaterLevel(maxLavaLevel);
            cir.setReturnValue(true);
        }
    }
}
