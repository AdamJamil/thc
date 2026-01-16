package thc.item

import net.minecraft.world.item.Item

/**
 * Land plot book item used as currency for claiming chunks.
 *
 * This item represents a land plot claim book. Players must find bells in the world
 * and use them to acquire these books, which can then be used to claim chunks.
 *
 * Non-stackable to maintain scarcity and value.
 */
class LandPlotItem(properties: Properties) : Item(properties)
