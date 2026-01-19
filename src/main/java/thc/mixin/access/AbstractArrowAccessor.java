package thc.mixin.access;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to read AbstractArrow's private baseDamage field.
 * Note: setBaseDamage() is already public in 1.21.11, only getter needed.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Accessor("baseDamage")
    double getBaseDamage();
}
