package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
	/**
	 * Reduce all melee damage by 75%.
	 * The attack() method stores damage in a local variable 'f' (damage from getAttackDamage).
	 * We intercept it after the attribute calculation and scale it down.
	 */
	@ModifyVariable(
		method = "attack",
		at = @At(value = "STORE"),
		ordinal = 0
	)
	private float thc$reduceMeleeDamage(float originalDamage) {
		// Reduce damage by 25% (multiply by 0.75)
		return originalDamage * 0.75f;
	}

	/**
	 * Disable sweeping edge enchantment by returning 0 for sweep damage ratio.
	 */
	@Redirect(
		method = "attack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getSweepingDamageRatio(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)F"
		)
	)
	private float thc$disableSweepingEdge(ServerLevel level, ItemStack weapon, float baseDamage) {
		// Return 0 to completely disable sweeping edge bonus damage
		return 0.0f;
	}
}
