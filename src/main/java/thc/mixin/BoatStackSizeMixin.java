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
 * Mixin to modify all wooden boat max stack size from 1 to 16.
 * Boats become more practical inventory items for the Boat Mastery boon.
 */
@Mixin(Items.class)
public class BoatStackSizeMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void thc$increaseBoatStackSize(CallbackInfo ci) {
        setStackSize(Items.OAK_BOAT, 16);
        setStackSize(Items.BIRCH_BOAT, 16);
        setStackSize(Items.SPRUCE_BOAT, 16);
        setStackSize(Items.JUNGLE_BOAT, 16);
        setStackSize(Items.ACACIA_BOAT, 16);
        setStackSize(Items.DARK_OAK_BOAT, 16);
        setStackSize(Items.MANGROVE_BOAT, 16);
        setStackSize(Items.CHERRY_BOAT, 16);
        setStackSize(Items.PALE_OAK_BOAT, 16);
        setStackSize(Items.BAMBOO_RAFT, 16);
    }

    private static void setStackSize(net.minecraft.world.item.Item item, int size) {
        DataComponentMap original = item.components();
        DataComponentMap.Builder builder = DataComponentMap.builder();
        builder.addAll(original);
        builder.set(DataComponents.MAX_STACK_SIZE, size);
        ((ItemAccessor) item).setComponentsInternal(builder.build());
    }
}
