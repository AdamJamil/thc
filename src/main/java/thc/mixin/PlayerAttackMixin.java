package thc.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import thc.playerclass.ClassManager;
import thc.playerclass.PlayerClass;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
	/**
	 * Reduce all melee damage by 75%, then apply class-based melee multiplier.
	 * The attack() method stores damage in a local variable 'f' (damage from getAttackDamage).
	 * We intercept it after the attribute calculation and scale it down.
	 */
	@ModifyVariable(
		method = "attack",
		at = @At(value = "STORE"),
		ordinal = 0
	)
	private float thc$reduceMeleeDamage(float originalDamage) {
		// Reduce damage to 18.75% of original (0.75 * 0.25)
		float baseDamage = originalDamage * 0.1875f;

		// Apply class-based melee multiplier
		Player self = (Player) (Object) this;
		if (self instanceof ServerPlayer serverPlayer) {
			PlayerClass playerClass = ClassManager.getClass(serverPlayer);
			if (playerClass != null) {
				baseDamage *= (float) playerClass.getMeleeMultiplier();
			}
		}

		return baseDamage;
	}

	/**
	 * Increase critical hit multiplier from 1.5x to 2.0x.
	 * Slice ensures we only modify the 1.5F between canCriticalAttack and isSweepAttack.
	 */
	@ModifyConstant(
		method = "attack",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"),
			to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSweepAttack(ZZZ)Z")
		),
		constant = @Constant(floatValue = 1.5F)
	)
	private float thc$doubleCritDamage(float original) {
		return 2.0F;
	}

	/**
	 * Disable sweeping edge by returning 0 for SWEEPING_DAMAGE_RATIO attribute.
	 * In 1.21.11+, sweeping damage is controlled by an attribute, not EnchantmentHelper.
	 */
	@Redirect(
		method = "doSweepAttack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"
		)
	)
	private double thc$disableSweepingEdge(Player player, Holder<Attribute> attribute) {
		// Return 0 for sweeping damage ratio to completely disable sweep attacks
		if (attribute.value() == Attributes.SWEEPING_DAMAGE_RATIO) {
			return 0.0;
		}
		// For other attributes, call the original method
		return player.getAttributeValue(attribute);
	}
}
