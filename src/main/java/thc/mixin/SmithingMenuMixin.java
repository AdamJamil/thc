package thc.mixin;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.smithing.TierUpgradeConfig;

import java.util.List;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends AbstractContainerMenu {

    @Unique
    private boolean thc$wasTierUpgrade = false;

    @Unique
    private int thc$requiredMaterialCount = 0;

    // Dummy constructor required for extending AbstractContainerMenu
    protected SmithingMenuMixin() {
        super(null, 0);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void thc$handleTierUpgrade(CallbackInfo ci) {
        // SmithingMenu slots: 0 = template, 1 = base, 2 = addition, 3 = result
        List<Slot> slots = this.slots;
        ItemStack base = slots.get(1).getItem();
        ItemStack addition = slots.get(2).getItem();

        // Reset upgrade flag
        thc$wasTierUpgrade = false;

        // Check if this is a tier upgrade
        if (base.isEmpty() || addition.isEmpty()) {
            return;
        }

        Item baseItem = base.getItem();
        Item additionItem = addition.getItem();

        // Check if this is a valid tier upgrade and get result in one lookup
        Item resultItem = TierUpgradeConfig.INSTANCE.getUpgradeResult(baseItem, additionItem);
        if (resultItem == null) {
            // Not a tier upgrade - let vanilla handle it
            return;
        }

        // This IS a tier upgrade - we handle it entirely, vanilla should not run
        // Get required material count for the RESULT item (what we're creating)
        int requiredCount = TierUpgradeConfig.INSTANCE.getRequiredMaterialCount(resultItem);
        if (requiredCount == 0) {
            // Fallback to base item count if result not in map
            requiredCount = TierUpgradeConfig.INSTANCE.getRequiredMaterialCount(baseItem);
        }
        if (requiredCount == 0) {
            // Safety fallback
            requiredCount = 1;
        }

        // Store for onTake
        thc$wasTierUpgrade = true;
        thc$requiredMaterialCount = requiredCount;

        // Check if sufficient materials
        if (addition.getCount() < requiredCount) {
            // Insufficient materials - show no result
            slots.get(3).set(ItemStack.EMPTY);
            ci.cancel();
            return;
        }

        // Create result item and selectively copy user-applied components
        // DO NOT copy item-type-specific components (EQUIPPABLE, ITEM_MODEL, ATTRIBUTE_MODIFIERS, etc.)
        ItemStack result = new ItemStack(resultItem);

        // Copy only user modifications that should transfer
        copyComponentIfPresent(base, result, DataComponents.ENCHANTMENTS);
        copyComponentIfPresent(base, result, DataComponents.CUSTOM_NAME);
        copyComponentIfPresent(base, result, DataComponents.LORE);
        copyComponentIfPresent(base, result, DataComponents.REPAIR_COST);

        // Set result using Slot.set() for proper client sync
        slots.get(3).set(result);
        ci.cancel();
    }

    @Unique
    private <T> void copyComponentIfPresent(ItemStack source, ItemStack dest, DataComponentType<T> component) {
        T value = source.get(component);
        if (value != null) {
            dest.set(component, value);
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void thc$consumeExtraMaterials(Player player, ItemStack result, CallbackInfo ci) {
        if (!thc$wasTierUpgrade) {
            return;
        }

        // Get the addition item before vanilla processes it
        List<Slot> slots = this.slots;
        ItemStack addition = slots.get(2).getItem();

        if (addition.isEmpty()) {
            thc$wasTierUpgrade = false;
            thc$requiredMaterialCount = 0;
            return;
        }

        // Consume extra materials (vanilla will consume 1, we consume the rest)
        // We need to shrink by (required - 1) because vanilla shrinks by 1
        if (thc$requiredMaterialCount > 1) {
            addition.shrink(thc$requiredMaterialCount - 1);
        }

        // Reset flags
        thc$wasTierUpgrade = false;
        thc$requiredMaterialCount = 0;
    }
}
