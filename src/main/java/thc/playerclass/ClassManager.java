package thc.playerclass;

import net.minecraft.server.level.ServerPlayer;
import thc.THCAttachments;
import thc.access.ServerPlayerHealthAccess;

/**
 * Static utility for player class CRUD operations.
 * Follows the same pattern as ThreatManager.
 */
public final class ClassManager {
	private ClassManager() {
	}

	/**
	 * Get the player's selected class (internal use only).
	 * External callers should use is<Class>() for boolean gates
	 * or getEffective*() for numeric values.
	 */
	private static PlayerClass getClass(ServerPlayer player) {
		String className = player.getAttached(THCAttachments.PLAYER_CLASS);
		if (className == null) return null;
		return PlayerClass.fromString(className);
	}

	/**
	 * Check if player has a class selected.
	 * @param player The player to check
	 * @return true if player has a class
	 */
	public static boolean hasClass(ServerPlayer player) {
		return getClass(player) != null;
	}

	/**
	 * Set the player's class. This is permanent - can only be set once.
	 * @param player The player to set class for
	 * @param playerClass The class to set
	 * @return true if class was set, false if player already has a class
	 */
	public static boolean setClass(ServerPlayer player, PlayerClass playerClass) {
		if (hasClass(player)) return false; // Already has class - permanent
		player.setAttached(THCAttachments.PLAYER_CLASS, playerClass.name());
		applyHealthModifier(player);
		return true;
	}

	/**
	 * Apply health modifier based on effective class bonus.
	 * Uses existing ServerPlayerHealthAccess interface.
	 */
	private static void applyHealthModifier(ServerPlayer player) {
		double baseHealth = 8.0; // THC default (4 hearts)
		double newHealth = baseHealth + getEffectiveHealthBonus(player);
		((ServerPlayerHealthAccess) player).thc$setMaxHealth(newHealth);
	}

	/**
	 * Restore health modifier for returning players on login.
	 * Call this in JOIN event for players who already have a class.
	 * @param player The player to restore health for
	 */
	public static void restoreHealthModifier(ServerPlayer player) {
		if (hasClass(player)) {
			applyHealthModifier(player);
		}
	}

	// --- allClasses override mode ---

	private static boolean allClassesEnabled = false;

	/**
	 * Check if player is Bastion class (or allClasses override is active).
	 */
	public static boolean isBastion(ServerPlayer player) {
		return allClassesEnabled || getClass(player) == PlayerClass.BASTION;
	}

	/**
	 * Check if player is Melee class (or allClasses override is active).
	 */
	public static boolean isMelee(ServerPlayer player) {
		return allClassesEnabled || getClass(player) == PlayerClass.MELEE;
	}

	/**
	 * Check if player is Ranged class (or allClasses override is active).
	 */
	public static boolean isRanged(ServerPlayer player) {
		return allClassesEnabled || getClass(player) == PlayerClass.RANGED;
	}

	/**
	 * Check if player is Support class (or allClasses override is active).
	 */
	public static boolean isSupport(ServerPlayer player) {
		return allClassesEnabled || getClass(player) == PlayerClass.SUPPORT;
	}

	/**
	 * Set the allClasses override mode.
	 * When enabled, all is<Class>() checks return true for any player.
	 */
	public static void setAllClasses(boolean enabled) {
		allClassesEnabled = enabled;
	}

	/**
	 * Check if allClasses override mode is currently enabled.
	 */
	public static boolean isAllClassesEnabled() {
		return allClassesEnabled;
	}

	// --- Effective numeric accessors (respect allClasses) ---

	/**
	 * Get effective melee multiplier. Checks is<Class>() in descending order
	 * so allClasses mode yields the highest multiplier.
	 * MELEE(4.0) > BASTION(2.5) > RANGED(1.0) = SUPPORT(1.0)
	 */
	public static double getEffectiveMeleeMultiplier(ServerPlayer player) {
		if (isMelee(player)) return PlayerClass.MELEE.getMeleeMultiplier();
		if (isBastion(player)) return PlayerClass.BASTION.getMeleeMultiplier();
		if (isRanged(player)) return PlayerClass.RANGED.getMeleeMultiplier();
		if (isSupport(player)) return PlayerClass.SUPPORT.getMeleeMultiplier();
		return 1.0; // no class selected
	}

	/**
	 * Get effective ranged multiplier. Checks is<Class>() in descending order
	 * so allClasses mode yields the highest multiplier.
	 * RANGED(5.0) > SUPPORT(3.0) > BASTION(1.0) = MELEE(1.0)
	 */
	public static double getEffectiveRangedMultiplier(ServerPlayer player) {
		if (isRanged(player)) return PlayerClass.RANGED.getRangedMultiplier();
		if (isSupport(player)) return PlayerClass.SUPPORT.getRangedMultiplier();
		if (isBastion(player)) return PlayerClass.BASTION.getRangedMultiplier();
		if (isMelee(player)) return PlayerClass.MELEE.getRangedMultiplier();
		return 1.0; // no class selected
	}

	/**
	 * Get effective health bonus. Checks is<Class>() in descending order
	 * so allClasses mode yields the highest bonus.
	 * BASTION(2.0) > MELEE(1.0) > RANGED(0.0) = SUPPORT(0.0)
	 */
	public static double getEffectiveHealthBonus(ServerPlayer player) {
		if (isBastion(player)) return PlayerClass.BASTION.getHealthBonus();
		if (isMelee(player)) return PlayerClass.MELEE.getHealthBonus();
		if (isRanged(player)) return PlayerClass.RANGED.getHealthBonus();
		if (isSupport(player)) return PlayerClass.SUPPORT.getHealthBonus();
		return 0.0; // no class selected
	}
}
