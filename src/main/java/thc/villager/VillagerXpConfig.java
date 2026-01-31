package thc.villager;

/**
 * XP threshold configuration for villager leveling.
 *
 * <p>THC replaces automatic villager leveling with manual emerald-payment
 * advancement. This class defines how much XP is needed to become eligible
 * for level up at each tier.
 *
 * <p>XP requirements (trades to max XP):
 * <ul>
 *   <li>Level 1 (Novice): 10 XP = 2 trades</li>
 *   <li>Level 2 (Apprentice): 15 XP = 3 trades</li>
 *   <li>Level 3 (Journeyman): 20 XP = 4 trades</li>
 *   <li>Level 4 (Expert): 25 XP = 5 trades</li>
 *   <li>Level 5 (Master): 0 = no more leveling</li>
 * </ul>
 *
 * <p>XP is capped at the max for current level to prevent overflow.
 */
public final class VillagerXpConfig {

    private VillagerXpConfig() {
    }

    /**
     * Uniform XP gain per trade completion.
     * All trades award this same amount.
     */
    public static final int XP_PER_TRADE = 5;

    /**
     * Maximum XP for each villager level (0-5).
     * Index = villager level.
     * Level 0 is unused (villagers start at level 1).
     * Level 5 (Master) has 0 because no further leveling is possible.
     */
    private static final int[] MAX_XP_PER_LEVEL = {
        0,   // Level 0: unused
        10,  // Level 1 (Novice): 2 trades
        15,  // Level 2 (Apprentice): 3 trades
        20,  // Level 3 (Journeyman): 4 trades
        25,  // Level 4 (Expert): 5 trades
        0    // Level 5 (Master): no more leveling
    };

    /**
     * Get the maximum XP a villager can accumulate at a given level.
     *
     * @param level the villager's current level (1-5)
     * @return maximum XP for that level, or 0 if level is out of range
     */
    public static int getMaxXpForLevel(int level) {
        if (level < 0 || level >= MAX_XP_PER_LEVEL.length) {
            return 0;
        }
        return MAX_XP_PER_LEVEL[level];
    }

    /**
     * Check if a villager has reached max XP for their current level.
     *
     * @param level the villager's current level (1-5)
     * @param currentXp the villager's current XP
     * @return true if XP is at or above max for the level
     */
    public static boolean isAtMaxXp(int level, int currentXp) {
        int max = getMaxXpForLevel(level);
        return max > 0 && currentXp >= max;
    }
}
