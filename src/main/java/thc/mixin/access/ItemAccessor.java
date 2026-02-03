package thc.mixin.access;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to allow modification of Item's component map
 * and access to protected static methods.
 */
@Mixin(Item.class)
public interface ItemAccessor {

    @Accessor("components")
    DataComponentMap getComponentsInternal();

    @Mutable
    @Accessor("components")
    void setComponentsInternal(DataComponentMap components);

    /**
     * Invokes the protected static getPlayerPOVHitResult method.
     * Used for boat land placement to detect what player is looking at.
     */
    @Invoker("getPlayerPOVHitResult")
    static BlockHitResult invokeGetPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluid) {
        throw new AssertionError("Mixin invoker not applied");
    }
}
