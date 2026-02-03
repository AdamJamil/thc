package thc.playerclass;

public enum PlayerClass {
	BASTION(2.0, 2.5, 1.0),   // +1 heart, x2.5 melee, x1 ranged
	MELEE(1.0, 4.0, 1.0),     // +0.5 hearts, x4 melee, x1 ranged
	RANGED(0.0, 1.0, 5.0),    // no health change, x1 melee, x5 ranged
	SUPPORT(0.0, 1.0, 3.0);   // no health change, x1 melee, x3 ranged

	private final double healthBonus;  // In HP (2 HP = 1 heart)
	private final double meleeMultiplier;
	private final double rangedMultiplier;

	PlayerClass(double healthBonus, double meleeMultiplier, double rangedMultiplier) {
		this.healthBonus = healthBonus;
		this.meleeMultiplier = meleeMultiplier;
		this.rangedMultiplier = rangedMultiplier;
	}

	public double getHealthBonus() {
		return healthBonus;
	}

	public double getMeleeMultiplier() {
		return meleeMultiplier;
	}

	public double getRangedMultiplier() {
		return rangedMultiplier;
	}

	/**
	 * Parse a class name string (case-insensitive).
	 * @param name The class name to parse
	 * @return The PlayerClass enum value, or null if invalid
	 */
	public static PlayerClass fromString(String name) {
		if (name == null) {
			return null;
		}
		try {
			return PlayerClass.valueOf(name.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
