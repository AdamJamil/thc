package thc.mixin.access;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to allow modification of Item's component map.
 */
@Mixin(Item.class)
public interface ItemAccessor {

    @Accessor("components")
    DataComponentMap getComponentsInternal();

    @Mutable
    @Accessor("components")
    void setComponentsInternal(DataComponentMap components);
}
