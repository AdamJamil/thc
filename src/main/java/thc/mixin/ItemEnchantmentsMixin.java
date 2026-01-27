package thc.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Override enchantment tooltip to display names without level suffix.
 *
 * <p>LVL-01: All enchantments display as single level (no I/II/III suffix).
 * Since THC enforces single-level enchantments, the level suffix is redundant.
 *
 * <p>Applies to all enchantment display contexts: item tooltips, enchanted books,
 * anvil UI, and enchanting table UI.
 */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {

    /**
     * Replace vanilla addToTooltip to show enchantment names without levels.
     *
     * <p>Vanilla uses Enchantment.getFullname(holder, level) which appends
     * Roman numeral suffix. We display just the enchantment description
     * which has no level suffix.
     */
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void thc$hideEnchantmentLevels(
            Item.TooltipContext context,
            Consumer<Component> consumer,
            TooltipFlag flag,
            DataComponentGetter componentGetter,
            CallbackInfo ci) {

        ItemEnchantments self = (ItemEnchantments) (Object) this;

        // Iterate all enchantments and add their names WITHOUT level suffix
        for (Holder<Enchantment> holder : self.keySet()) {
            // Use the enchantment description directly (name without level)
            Component name = holder.value().description();
            consumer.accept(name);
        }

        ci.cancel();
    }
}
