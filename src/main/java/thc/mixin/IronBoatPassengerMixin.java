package thc.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.entity.IronBoat;

@Mixin(LivingEntity.class)
public abstract class IronBoatPassengerMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void thc$cancelFireDamageInIronBoat(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity vehicle = self.getVehicle();

        // Check if riding an iron boat
        if (vehicle instanceof IronBoat) {
            // Cancel fire and lava damage
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                cir.setReturnValue(false);
            }
        }
    }
}
