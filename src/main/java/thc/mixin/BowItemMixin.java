package thc.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TippedArrowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thc.bow.BowType;

/**
 * Mixin to block tipped arrows from being fired by the Wooden Bow.
 * When a tipped arrow is selected by vanilla logic, we swap it for a regular arrow.
 * If no regular arrow exists, the bow does not fire.
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {

	/**
	 * Redirects Player.getProjectile() in BowItem.releaseUsing to block tipped arrows
	 * for the Wooden Bow. If vanilla selects a tipped arrow and the bow is wooden,
	 * we search inventory for a regular arrow to use instead.
	 */
	@Redirect(
		method = "releaseUsing",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	private ItemStack thc$blockTippedArrowsForWoodenBow(Player player, ItemStack weaponStack) {
		ItemStack vanillaSelection = player.getProjectile(weaponStack);

		// Only restrict wooden bow
		BowType bowType = BowType.fromBowItem(weaponStack);
		if (bowType != BowType.WOODEN) {
			return vanillaSelection;
		}

		// If vanilla didn't find any arrow, nothing to do
		if (vanillaSelection.isEmpty()) {
			return vanillaSelection;
		}

		// If the selected arrow is NOT tipped, allow it
		if (!(vanillaSelection.getItem() instanceof TippedArrowItem)) {
			return vanillaSelection;
		}

		// Tipped arrow selected -- find a regular arrow in inventory instead
		ItemStack regularArrow = thc$findRegularArrow(player);
		if (!regularArrow.isEmpty()) {
			return regularArrow;
		}

		// No regular arrows available -- block firing entirely
		player.displayClientMessage(
			Component.translatable("thc.bow.tipped_blocked"),
			true
		);
		return ItemStack.EMPTY;
	}

	/**
	 * Searches the player's inventory for a non-tipped arrow.
	 * Regular arrows: vanilla Arrow, and THC tiered arrows (iron/diamond/netherite).
	 * Returns the first matching stack, or EMPTY if none found.
	 */
	private ItemStack thc$findRegularArrow(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			// Accept any ArrowItem that is NOT a TippedArrowItem
			// This covers: vanilla Arrow, Iron Arrow, Diamond Arrow, Netherite Arrow
			if (stack.getItem() instanceof net.minecraft.world.item.ArrowItem
				&& !(stack.getItem() instanceof TippedArrowItem)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}
}
