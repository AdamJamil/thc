package thc.mixin.access;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to allow modification of AbstractArrow's base damage.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Accessor("baseDamage")
    double getBaseDamage();

    @Mutable
    @Accessor("baseDamage")
    void setBaseDamage(double baseDamage);
}
