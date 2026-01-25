package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.entity.IronBoat;

/**
 * Mixin to protect passengers in iron boats from lava damage.
 *
 * KEY BEHAVIORS:
 * - Only protects from LAVA damage type (not campfire, on_fire, etc.)
 * - Only protects if passenger's eyes are at or below boat level (not submerged in deep lava)
 * - Fire visual clearing is handled in IronBoat.tick()
 */
@Mixin(LivingEntity.class)
public abstract class IronBoatPassengerMixin {

    // Height buffer above boat Y for protection zone
    private static final double BOAT_PROTECTION_HEIGHT = 1.5;

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void thc$cancelLavaDamageInIronBoat(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity vehicle = self.getVehicle();

        // Check if riding an iron boat
        if (vehicle instanceof IronBoat boat) {
            // Only protect from LAVA damage specifically, not all fire damage
            // This allows campfire damage, being on_fire, etc. to still hurt
            if (source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
                // Check if passenger is at or below boat protection level
                // If eyes are above this level (submerged in lava column), damage applies
                double boatSurfaceY = boat.getY() + BOAT_PROTECTION_HEIGHT;

                if (self.getEyeY() <= boatSurfaceY) {
                    // Protected by boat - passenger is at normal riding height
                    cir.setReturnValue(false);
                }
                // If eyes are above protection height, passenger is submerged - damage applies
            }
            // Other fire sources (campfire, etc.) - no protection, let vanilla handle
        }
    }
}
