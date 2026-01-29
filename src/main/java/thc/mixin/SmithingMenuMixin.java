package thc.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.smithing.TierUpgradeConfig;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {

    @Unique
    private boolean thc$wasTierUpgrade = false;

    // Dummy constructor required for extending ItemCombinerMenu
    protected SmithingMenuMixin() {
        super(null, 0, null, null, null);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void thc$handleTierUpgrade(CallbackInfo ci) {
        // SmithingMenu slots: inputSlots.getItem(0) = template, getItem(1) = base, getItem(2) = addition
        ItemStack template = this.inputSlots.getItem(0);
        ItemStack base = this.inputSlots.getItem(1);
        ItemStack addition = this.inputSlots.getItem(2);

        // Reset upgrade flag
        thc$wasTierUpgrade = false;

        // Check if this is a tier upgrade
        if (base.isEmpty() || addition.isEmpty()) {
            return;
        }

        Item baseItem = base.getItem();
        Item additionItem = addition.getItem();

        if (!TierUpgradeConfig.INSTANCE.isValidTierUpgrade(baseItem, additionItem)) {
            return;
        }

        // This is a tier upgrade - mark it
        thc$wasTierUpgrade = true;

        // Get required material count
        int requiredCount = TierUpgradeConfig.INSTANCE.getRequiredMaterialCount(baseItem);

        // Check if sufficient materials
        if (addition.getCount() < requiredCount) {
            // Insufficient materials - clear result and cancel
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            ci.cancel();
            return;
        }

        // Get upgrade result
        Item resultItem = TierUpgradeConfig.INSTANCE.getUpgradeResult(baseItem, additionItem);
        if (resultItem == null) {
            return;
        }

        // Create result item with enchantments from base (vanilla smithing_transform copies these)
        ItemStack result = new ItemStack(resultItem);

        // Copy all components from base except damage (restores durability)
        result.applyComponents(base.getComponents());
        result.remove(DataComponents.DAMAGE);

        // Set result
        this.resultSlots.setItem(0, result);
        ci.cancel();
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void thc$consumeExtraMaterials(Player player, ItemStack result, CallbackInfo ci) {
        if (!thc$wasTierUpgrade) {
            return;
        }

        // Get the base and addition items
        ItemStack base = this.inputSlots.getItem(1);
        ItemStack addition = this.inputSlots.getItem(2);

        if (base.isEmpty() || addition.isEmpty()) {
            return;
        }

        Item baseItem = base.getItem();
        int requiredCount = TierUpgradeConfig.INSTANCE.getRequiredMaterialCount(baseItem);

        // Consume extra materials (vanilla already consumed 1)
        if (requiredCount > 1) {
            addition.shrink(requiredCount - 1);
        }

        // Reset flag
        thc$wasTierUpgrade = false;
    }
}
