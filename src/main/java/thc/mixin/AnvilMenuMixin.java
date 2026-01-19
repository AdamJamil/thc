package thc.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.item.THCArrows;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow @Final private DataSlot cost;
    @Shadow protected Container inputSlots;
    @Shadow protected ResultContainer resultSlots;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void thc$handleArrowCrafting(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);

        // Check for arrow upgrade recipe: 64 arrows in left slot
        if (!left.is(Items.ARROW) || left.getCount() < 64) {
            return;
        }

        ItemStack result = null;
        int levelCost = 1;

        // Iron Ingot -> Iron Arrows
        if (right.is(Items.IRON_INGOT) && right.getCount() >= 1) {
            result = new ItemStack(THCArrows.IRON_ARROW, 64);
            levelCost = 1;
        }
        // Diamond -> Diamond Arrows
        else if (right.is(Items.DIAMOND) && right.getCount() >= 1) {
            result = new ItemStack(THCArrows.DIAMOND_ARROW, 64);
            levelCost = 2;
        }
        // Netherite Ingot -> Netherite Arrows
        else if (right.is(Items.NETHERITE_INGOT) && right.getCount() >= 1) {
            result = new ItemStack(THCArrows.NETHERITE_ARROW, 64);
            levelCost = 3;
        }

        if (result != null) {
            this.resultSlots.setItem(0, result);
            this.cost.set(levelCost);
            ci.cancel();
        }
    }
}
