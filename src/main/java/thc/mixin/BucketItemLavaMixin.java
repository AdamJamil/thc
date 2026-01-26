package thc.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks lava bucket placement by intercepting BucketItem.use().
 * Returns FAIL for lava buckets, effectively preventing lava placement
 * while allowing vanilla lava buckets to remain in inventory for other uses.
 */
@Mixin(BucketItem.class)
public class BucketItemLavaMixin {

    @Inject(
        method = "use",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockLavaPlacement(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() == Items.LAVA_BUCKET) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
