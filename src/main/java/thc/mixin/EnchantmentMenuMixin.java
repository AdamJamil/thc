package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    @Unique
    private static final Identifier EMPTY_SLOT_BOOK = Identifier.fromNamespaceAndPath("thc", "container/slot/book_slot");

    @Unique
    private static final int ENCHANT_COST = 3;

    /** The minimum level required for the current enchantment (stage-based: 10/20/30) */
    @Unique
    private int thc$levelRequirement = 0;

    protected EnchantmentMenuMixin() { super(null, 0); }

    /**
     * Replace slots at end of constructor to implement book-slot enchanting:
     * - Slot 0 (item slot): accepts enchantable items EXCEPT enchanted books
     * - Slot 1 (book slot): accepts ONLY enchanted books
     */
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void thc$replaceSlotWithBookSlot(int syncId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        // Slot 0: item slot - accepts enchantable items but NOT enchanted books
        // Allow already-enchanted items (isEnchantable() returns false for those)
        Slot itemSlot = new Slot(enchantSlots, 0, 15, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.is(Items.ENCHANTED_BOOK)) {
                    return false;
                }
                // Accept items that can be enchanted OR are already enchanted
                return stack.isEnchantable() || stack.isEnchanted();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        itemSlot.index = 0;
        this.slots.set(0, itemSlot);

        // Slot 1: book slot - accepts ONLY enchanted books
        Slot bookSlot = new Slot(enchantSlots, 1, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_SLOT_BOOK;
            }
        };
        bookSlot.index = 1;
        this.slots.set(1, bookSlot);
    }

    /**
     * Override slotsChanged to implement book-slot enchanting.
     * We inject at HEAD because vanilla's isEnchantable() check blocks already-enchanted items.
     */
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void thc$onSlotsChanged(Container container, CallbackInfo ci) {
        // Only handle changes to our enchant slots
        if (container != enchantSlots) {
            return;
        }

        System.out.println("[THC DEBUG] slotsChanged called");

        // Reset all costs first
        costs[0] = 0;
        costs[1] = 0;
        costs[2] = 0;
        enchantClue[0] = -1;
        enchantClue[1] = -1;
        enchantClue[2] = -1;
        levelClue[0] = -1;
        levelClue[1] = -1;
        levelClue[2] = -1;

        // Get item and book from slots
        ItemStack item = enchantSlots.getItem(0);
        ItemStack book = enchantSlots.getItem(1);
        System.out.println("[THC DEBUG] Item: " + item.getItem() + ", Book: " + book.getItem());

        // Need both item and enchanted book for our system
        if (item.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) {
            System.out.println("[THC DEBUG] FAIL: Item empty=" + item.isEmpty() + ", isEnchantedBook=" + book.is(Items.ENCHANTED_BOOK));
            // Let vanilla handle it (or reset costs)
            this.broadcastChanges();
            ci.cancel();
            return;
        }

        // Run our book-slot enchanting logic with world access
        access.execute((level, pos) -> {
            thc$calculateBookEnchantCosts(item, book, level, pos);
        });

        this.broadcastChanges();
        ci.cancel();
    }

    @Unique
    private void thc$calculateBookEnchantCosts(ItemStack item, ItemStack book, Level level, BlockPos pos) {
        // Count bookshelves (vanilla max is 15)
        int bookshelfCount = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                bookshelfCount++;
            }
        }
        System.out.println("[THC DEBUG] Bookshelf count: " + bookshelfCount);

        // Require 15 bookshelves (all valid positions filled)
        if (bookshelfCount < 15) {
            System.out.println("[THC DEBUG] FAIL: Not enough bookshelves (" + bookshelfCount + "/15)");
            return;
        }

        // Get enchantment from book
        ItemEnchantments storedEnchants = book.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchants == null || storedEnchants.isEmpty()) {
            System.out.println("[THC DEBUG] FAIL: No stored enchantments on book");
            return;
        }

        // Get first enchantment (require single-enchantment books)
        var entries = storedEnchants.entrySet().iterator();
        if (!entries.hasNext()) {
            System.out.println("[THC DEBUG] FAIL: No enchantment entries");
            return;
        }
        var entry = entries.next();

        // Reject multi-enchantment books
        if (entries.hasNext()) {
            System.out.println("[THC DEBUG] FAIL: Multi-enchantment book");
            return;
        }

        Holder<Enchantment> enchantHolder = entry.getKey();
        String enchantId = enchantHolder.unwrapKey().orElse(null) != null
            ? enchantHolder.unwrapKey().get().identifier().toString()
            : null;
        System.out.println("[THC DEBUG] Enchantment: " + enchantId);

        // Check compatibility
        if (!thc$isCompatible(enchantHolder, item)) {
            System.out.println("[THC DEBUG] FAIL: Enchantment not compatible with item");
            return;
        }

        // Get level requirement based on stage (10/20/30)
        int stage = EnchantmentEnforcement.INSTANCE.getStageForEnchantment(enchantId);
        thc$levelRequirement = EnchantmentEnforcement.INSTANCE.getLevelRequirementForStage(stage);
        System.out.println("[THC DEBUG] Stage: " + stage + ", Level requirement: " + thc$levelRequirement);

        // Display cost of 3 (what player actually pays)
        costs[0] = ENCHANT_COST;
        System.out.println("[THC DEBUG] SUCCESS: Setting costs[0] = " + ENCHANT_COST);
    }

    @Unique
    private boolean thc$isCompatible(Holder<Enchantment> enchantHolder, ItemStack item) {
        // Check if item type supports this enchantment
        // Use isSupportedItem which checks the enchantment's supportedItems definition
        // This works for already-enchanted items unlike canEnchant()
        boolean supported = enchantHolder.value().isSupportedItem(item);
        System.out.println("[THC DEBUG] isSupportedItem(" + item.getItem() + "): " + supported);
        if (!supported) {
            return false;
        }

        // Check for duplicates and conflicts
        ItemEnchantments existingEnchants = item.get(DataComponents.ENCHANTMENTS);
        if (existingEnchants != null) {
            // Already has this exact enchantment?
            int existingLevel = existingEnchants.getLevel(enchantHolder);
            System.out.println("[THC DEBUG] Existing level of this enchantment: " + existingLevel);
            if (existingLevel > 0) {
                System.out.println("[THC DEBUG] FAIL: Already has this enchantment");
                return false;
            }

            // Conflicts with existing enchantments?
            for (Holder<Enchantment> existing : existingEnchants.keySet()) {
                boolean compatible = Enchantment.areCompatible(enchantHolder, existing);
                System.out.println("[THC DEBUG] Compatible with " + existing.unwrapKey().orElse(null) + ": " + compatible);
                if (!compatible) {
                    System.out.println("[THC DEBUG] FAIL: Conflicts with existing enchantment");
                    return false;
                }
            }
        }
        return true;
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void thc$applyBookEnchantment(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        // Only handle button 0 (our single enchant button)
        if (buttonId != 0) {
            cir.setReturnValue(false);
            return;
        }

        // Verify cost is set (button should be visible)
        if (costs[0] <= 0) {
            cir.setReturnValue(false);
            return;
        }

        // Check player meets level requirement (10/20/30 based on stage)
        if (player.experienceLevel < thc$levelRequirement) {
            cir.setReturnValue(false);
            return;
        }

        // Get item and book
        ItemStack item = enchantSlots.getItem(0);
        ItemStack book = enchantSlots.getItem(1);

        if (item.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) {
            cir.setReturnValue(false);
            return;
        }

        ItemEnchantments storedEnchants = book.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchants == null || storedEnchants.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        // Re-validate bookshelves on server
        access.execute((level, pos) -> {
            int bookshelfCount = 0;
            for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                    bookshelfCount++;
                }
            }
            if (bookshelfCount < 15) {
                return;
            }

            // Get first enchantment from book
            var entry = storedEnchants.entrySet().iterator().next();
            Holder<Enchantment> enchantHolder = entry.getKey();

            // Final compatibility check
            if (!thc$isCompatible(enchantHolder, item)) {
                return;
            }

            // Apply enchantment using the proven LecternEnchanting pattern
            EnchantmentHelper.updateEnchantments(item, mutable -> {
                mutable.set(enchantHolder, entry.getIntValue());
            });

            // Deduct 3 levels (always 3 per CONTEXT.md)
            player.giveExperienceLevels(-3);

            // Play sound
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0f, 1.0f);

            // Broadcast slot changes
            this.broadcastChanges();
        });

        cir.setReturnValue(true);
    }

    /**
     * Handle shift-click for enchanted books to go to slot 1 (book slot).
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void thc$quickMoveEnchantedBook(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        // Only handle clicks from player inventory (slots 2+)
        if (slotIndex < 2) {
            return;
        }

        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return;
        }

        ItemStack itemStack = slot.getItem();
        if (itemStack.is(Items.ENCHANTED_BOOK)) {
            // Try to move enchanted book to slot 1 (book slot)
            ItemStack copy = itemStack.copy();
            if (this.moveItemStackTo(itemStack, 1, 2, false)) {
                if (itemStack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                cir.setReturnValue(copy);
            } else {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }
}
