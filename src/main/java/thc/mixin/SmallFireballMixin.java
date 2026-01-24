package thc.mixin;

import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Reduce Blaze fireball damage to ~3.8 (from 5 Normal / 7.5 Hard).
 *
 * <p>FR-11 (v2.3): Blaze fireballs deal reduced damage while maintaining
 * fire ignition effects. Multiplier of 0.76 preserves difficulty scaling.
 *
 * <p>Applies to ALL SmallFireball instances (Blaze-shot, dispenser-shot,
 * command-spawned) which is intentional for consistency.
 */
@Mixin(SmallFireball.class)
public abstract class SmallFireballMixin {

    /**
     * Intercept the damage value passed to hurt() in onHitEntity.
     *
     * <p>Vanilla: 5 (Normal) / 7.5 (Hard)
     * <p>THC: ~3.8 (Normal) / ~5.7 (Hard)
     * <p>Multiplier: 0.76
     *
     * @param original The vanilla damage value
     * @return Reduced damage value (76% of original)
     */
    @ModifyArg(
        method = "onHitEntity",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
        index = 2
    )
    private float thc$reduceFireballDamage(float original) {
        return original * 0.76f;
    }
}
