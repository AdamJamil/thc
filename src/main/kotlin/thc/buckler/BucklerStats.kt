package thc.buckler

import net.minecraft.world.item.ItemStack
import thc.item.THCBucklers

data class BucklerStats(
    val shieldPowerHearts: Double,
    val parryWindowSeconds: Double,
    val stunDurationSeconds: Double
) {
    val maxPoiseHearts: Double = shieldPowerHearts * 2.0
}

object BucklerStatsRegistry {
    private val stone = BucklerStats(shieldPowerHearts = 2.0, parryWindowSeconds = 0.16, stunDurationSeconds = 1.5)
    private val iron = BucklerStats(shieldPowerHearts = 3.0, parryWindowSeconds = 0.16, stunDurationSeconds = 1.5)
    private val gold = BucklerStats(shieldPowerHearts = 3.25, parryWindowSeconds = 0.18, stunDurationSeconds = 1.5)
    private val diamond = BucklerStats(shieldPowerHearts = 4.0, parryWindowSeconds = 0.22, stunDurationSeconds = 1.5)
    private val netherite = BucklerStats(shieldPowerHearts = 4.5, parryWindowSeconds = 0.25, stunDurationSeconds = 3.0)

    fun forStack(stack: ItemStack): BucklerStats? {
        return when (stack.item) {
            THCBucklers.STONE_BUCKLER -> stone
            THCBucklers.IRON_BUCKLER -> iron
            THCBucklers.GOLD_BUCKLER -> gold
            THCBucklers.DIAMOND_BUCKLER -> diamond
            THCBucklers.NETHERITE_BUCKLER -> netherite
            else -> null
        }
    }
}
