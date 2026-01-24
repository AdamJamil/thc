package thc.mixin;

import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Reduce Evoker fang damage to ~2.5 (from 6 Normal / 9 Hard).
 *
 * <p>FR-12 (v2.3): Evoker fangs deal reduced damage. Multiplier of 0.417
 * preserves difficulty scaling.
 *
 * <p>Applies to ALL EvokerFangs instances regardless of summoner (Evoker,
 * commands, mods) per project design decision - consistent damage rules.
 */
@Mixin(EvokerFangs.class)
public abstract class EvokerFangsMixin {

    /**
     * Intercept the damage value passed to hurt() in dealDamageTo.
     *
     * <p>Vanilla: 6 (Normal) / 9 (Hard)
     * <p>THC: ~2.5 (Normal) / ~3.75 (Hard)
     * <p>Multiplier: 0.417
     *
     * @param original The vanilla damage value
     * @return Reduced damage value (~42% of original)
     */
    @ModifyArg(
        method = "dealDamageTo",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
        index = 2
    )
    private float thc$reduceFangDamage(float original) {
        return original * 0.417f;
    }
}
