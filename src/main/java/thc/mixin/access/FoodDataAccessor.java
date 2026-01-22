package thc.mixin.access;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to allow modification of FoodData's private fields.
 * Used for saturation cap behavior (eating) and custom tick() override (exhaustion/healing).
 */
@Mixin(FoodData.class)
public interface FoodDataAccessor {

    @Accessor("saturationLevel")
    void setSaturationLevel(float level);

    @Accessor("exhaustionLevel")
    float getExhaustionLevel();

    @Accessor("exhaustionLevel")
    void setExhaustionLevel(float level);

    @Accessor("tickTimer")
    int getTickTimer();

    @Accessor("tickTimer")
    void setTickTimer(int timer);
}
