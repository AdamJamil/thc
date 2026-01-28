package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.enchant.EnchantmentEnforcement;

/**
 * Mixin to replace RNG-based enchanting with deterministic book-slot enchanting.
 *
 * The enchanting table now requires:
 * - 15 bookshelves (all valid positions filled)
 * - Single-enchantment book in slot 1 (former lapis slot)
 * - Player level meets stage requirement (10/20/30)
 * - Only one button shown (button 0)
 *
 * The book determines exactly what enchantment is applied.
 * Book remains in slot after enchanting (unlimited uses).
 * Cost is always 3 levels regardless of enchantment.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {

    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow public int[] costs;
    @Shadow public int[] enchantClue;
    @Shadow public int[] levelClue;

    protected EnchantmentMenuMixin() { super(null, 0); }

    @Inject(method = "method_17411", at = @At("HEAD"), cancellable = true)
    private void thc$calculateBookEnchantCosts(ItemStack itemStack, Level level, BlockPos pos, CallbackInfo ci) {
        // Reset all costs (hides buttons 1 and 2)
        costs[0] = 0;
        costs[1] = 0;
        costs[2] = 0;
        enchantClue[0] = -1;
        enchantClue[1] = -1;
        enchantClue[2] = -1;
        levelClue[0] = -1;
        levelClue[1] = -1;
        levelClue[2] = -1;

        // Count bookshelves (vanilla max is 15)
        int bookshelfCount = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                bookshelfCount++;
            }
        }

        // Require 15 bookshelves (all valid positions filled)
        if (bookshelfCount < 15) {
            ci.cancel();
            return;
        }

        // Get item and book from slots
        ItemStack item = enchantSlots.getItem(0);
        ItemStack book = enchantSlots.getItem(1);

        // Need both item to enchant and enchanted book
        if (item.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) {
            ci.cancel();
            return;
        }

        // Get enchantment from book
        ItemEnchantments storedEnchants = book.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchants == null || storedEnchants.isEmpty()) {
            ci.cancel();
            return;
        }

        // Get first enchantment (require single-enchantment books)
        var entries = storedEnchants.entrySet().iterator();
        if (!entries.hasNext()) {
            ci.cancel();
            return;
        }
        var entry = entries.next();

        // Reject multi-enchantment books
        if (entries.hasNext()) {
            ci.cancel();
            return;
        }

        Holder<Enchantment> enchantHolder = entry.getKey();
        String enchantId = enchantHolder.unwrapKey().orElse(null) != null
            ? enchantHolder.unwrapKey().get().identifier().toString()
            : null;

        // Check compatibility
        if (!thc$isCompatible(enchantHolder, item)) {
            ci.cancel();
            return;
        }

        // Get level requirement based on stage
        int stage = EnchantmentEnforcement.INSTANCE.getStageForEnchantment(enchantId);
        int levelReq = EnchantmentEnforcement.INSTANCE.getLevelRequirementForStage(stage);

        // Set cost for button 0 (only button we use)
        costs[0] = levelReq;

        ci.cancel();
    }

    @Unique
    private boolean thc$isCompatible(Holder<Enchantment> enchantHolder, ItemStack item) {
        // Can this enchantment go on this item type?
        if (!enchantHolder.value().canEnchant(item)) {
            return false;
        }

        // Check for duplicates and conflicts
        ItemEnchantments existingEnchants = item.get(DataComponents.ENCHANTMENTS);
        if (existingEnchants != null) {
            // Already has this exact enchantment?
            if (existingEnchants.getLevel(enchantHolder) > 0) {
                return false;
            }

            // Conflicts with existing enchantments?
            for (Holder<Enchantment> existing : existingEnchants.keySet()) {
                if (!Enchantment.areCompatible(enchantHolder, existing)) {
                    return false;
                }
            }
        }
        return true;
    }
}
