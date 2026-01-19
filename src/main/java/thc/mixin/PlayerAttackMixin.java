package thc.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
		// Reduce damage by 75% (multiply by 0.25)
		return originalDamage * 0.25f;
	}
}
