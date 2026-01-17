package thc.mixin;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.mixin.access.ItemAccessor;

/**
 * Mixin to modify snowball max stack size from 16 to 64.
 *
 * In MC 1.21+, item properties are stored as DataComponents.
 * This mixin injects at the end of Items class static initialization
 * to modify the SNOWBALL item's MAX_STACK_SIZE component.
 */
@Mixin(Items.class)
public class SnowballItemMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void thc$increaseSnowballStackSize(CallbackInfo ci) {
        // Build a new component map with the updated max stack size
        DataComponentMap original = Items.SNOWBALL.components();
        DataComponentMap.Builder builder = DataComponentMap.builder();
        builder.addAll(original);
        builder.set(DataComponents.MAX_STACK_SIZE, 64);

        // Use accessor to replace the item's components
        ((ItemAccessor) Items.SNOWBALL).setComponentsInternal(builder.build());
    }
}
