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
	 * Get the player's selected class.
	 * @param player The player to check
	 * @return The player's class, or null if no class selected
	 */
	public static PlayerClass getClass(ServerPlayer player) {
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
		applyHealthModifier(player, playerClass);
		return true;
	}

	/**
	 * Apply health modifier based on class selection.
	 * Uses existing ServerPlayerHealthAccess interface.
	 */
	private static void applyHealthModifier(ServerPlayer player, PlayerClass playerClass) {
		double baseHealth = 8.0; // THC default (4 hearts)
		double newHealth = baseHealth + playerClass.getHealthBonus();
		((ServerPlayerHealthAccess) player).thc$setMaxHealth(newHealth);
	}

	/**
	 * Restore health modifier for returning players on login.
	 * Call this in JOIN event for players who already have a class.
	 * @param player The player to restore health for
	 */
	public static void restoreHealthModifier(ServerPlayer player) {
		PlayerClass playerClass = getClass(player);
		if (playerClass != null) {
			applyHealthModifier(player, playerClass);
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
}
