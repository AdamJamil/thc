package thc.mixin.client;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Client-side mixin for the enchanting table screen.
 *
 * TODO: Hide the "1" lapis cost display since we use enchanted books instead.
 * The vanilla UI shows buttonIndex + 1 as the lapis requirement, but our
 * book-slot system doesn't consume lapis. This is a cosmetic issue - the
 * enchanting still works correctly, but the "1" is misleading.
 *
 * To fix: Find the render method that draws the lapis cost and suppress it,
 * or modify the argument to render empty string instead.
 */
@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {
    // Placeholder for future lapis cost display fix
}
