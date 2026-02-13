package thc.bow

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

enum class BowType(val dragFactor: Double, val tag: String) {
    WOODEN(dragFactor = 0.015, tag = "wooden_bow"),
    BLAZE(dragFactor = 0.015, tag = "blaze_bow"),
    BREEZE(dragFactor = 0.01, tag = "breeze_bow");

    companion object {
        @JvmStatic
        fun fromBowItem(stack: ItemStack): BowType {
            return when (stack.item) {
                Items.BOW -> WOODEN
                // Future custom bow items will be mapped here
                else -> WOODEN
            }
        }

        @JvmStatic
        fun fromTag(tag: String?): BowType? {
            if (tag == null) return null
            return entries.firstOrNull { it.tag == tag }
        }
    }
}
