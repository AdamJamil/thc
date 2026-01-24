package thc.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.mixin.access.AbstractArrowAccessor;

/**
 * Boost Piglin crossbow arrow damage to ~8 (from ~4 base).
 *
 * <p>FR-13 (v2.3): Piglin crossbow attacks deal increased damage to match
 * THC's danger balance where Piglins are threatening ranged combatants.
 *
 * <p>Only affects EntityType.PIGLIN (not PIGLIN_BRUTE which is melee-only).
 */
@Mixin(CrossbowItem.class)
public abstract class PiglinCrossbowMixin {

    /**
     * After crossbow projectile is shot, check if shooter is Piglin and boost damage.
     *
     * <p>Vanilla: ~4 base damage
     * <p>THC: ~8 base damage
     * <p>Multiplier: 2.0x
     *
     * <p>Uses TAIL injection after the projectile is added to the world,
     * accessing it via the callback parameters.
     */
    @Inject(
        method = "shootProjectile",
        at = @At("TAIL")
    )
    private static void thc$boostPiglinArrowDamage(
            LivingEntity shooter,
            Projectile projectile,
            int index,
            float speed,
            float inaccuracy,
            float angle,
            LivingEntity target,
            CallbackInfo ci) {

        // Only boost for regular Piglins (not Brutes, which don't use crossbows anyway)
        if (shooter.getType() != EntityType.PIGLIN) {
            return;
        }

        // Only modify arrows (not fireworks)
        if (!(projectile instanceof AbstractArrow arrow)) {
            return;
        }

        // Double the base damage (from ~4 to ~8)
        double currentDamage = ((AbstractArrowAccessor) arrow).getBaseDamage();
        arrow.setBaseDamage(currentDamage * 2.0);
    }
}
