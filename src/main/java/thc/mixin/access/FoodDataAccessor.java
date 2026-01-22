package thc.mixin.access;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to allow modification of FoodData's saturation level.
 * Used to implement saturation cap behavior when eating food.
 */
@Mixin(FoodData.class)
public interface FoodDataAccessor {

    @Accessor("saturationLevel")
    void setSaturationLevel(float level);
}
