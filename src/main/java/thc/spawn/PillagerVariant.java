package thc.spawn;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Equipment variants for pillagers spawned via regional distribution.
 *
 * <p>MELEE pillagers carry iron swords and have no crossbow.
 * RANGED pillagers use vanilla crossbow behavior (no modification needed).
 *
 * <p>Equipment must be applied BEFORE finalizeSpawn to ensure proper AI setup.
 */
public enum PillagerVariant {
	/**
	 * Melee pillager variant with iron sword.
	 * Crossbow is removed, AI will use melee attacks.
	 */
	MELEE {
		@Override
		public void applyEquipment(Pillager pillager) {
			// Set iron sword in main hand
			ItemStack sword = new ItemStack(Items.IRON_SWORD);
			pillager.setItemSlot(EquipmentSlot.MAINHAND, sword);

			// Set drop chance to 0 - never drops weapon
			pillager.setDropChance(EquipmentSlot.MAINHAND, 0.0f);

			// Clear offhand to ensure no crossbow held there
			pillager.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	},
	/**
	 * Ranged pillager variant with vanilla crossbow.
	 * No modification needed - vanilla populateDefaultEquipmentSlots sets crossbow.
	 */
	RANGED {
		@Override
		public void applyEquipment(Pillager pillager) {
			// No-op: vanilla crossbow already set by populateDefaultEquipmentSlots
		}
	};

	/**
	 * Apply equipment appropriate for this variant to the pillager.
	 * Must be called AFTER finalizeSpawn (TAIL injection) so equipment isn't overwritten.
	 *
	 * @param pillager The pillager to equip
	 */
	public abstract void applyEquipment(Pillager pillager);
}
