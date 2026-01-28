package thc.lectern

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LecternBlock
import net.minecraft.world.level.block.entity.LecternBlockEntity
import thc.enchant.EnchantmentEnforcement

object LecternEnchanting {

    @JvmStatic
    fun register() {
        UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
            val pos = hitResult.blockPos
            val blockState = level.getBlockState(pos)

            // Only handle lecterns
            if (!blockState.`is`(Blocks.LECTERN)) {
                return@register InteractionResult.PASS
            }

            // Server-side only processing
            if (level.isClientSide) {
                return@register InteractionResult.SUCCESS
            }

            val itemInHand = player.getItemInHand(hand)
            val hasBook = blockState.getValue(LecternBlock.HAS_BOOK)
            val blockEntity = level.getBlockEntity(pos) as? LecternBlockEntity

            // Shift+right-click: Remove book from lectern
            if (player.isShiftKeyDown && hasBook && blockEntity != null) {
                val storedBook = blockEntity.book
                if (!storedBook.isEmpty && storedBook.`is`(Items.ENCHANTED_BOOK)) {
                    // Give book back to player
                    if (!player.inventory.add(storedBook.copy())) {
                        player.drop(storedBook.copy(), false)
                    }
                    // Clear lectern
                    blockEntity.setBook(net.minecraft.world.item.ItemStack.EMPTY)
                    level.setBlock(pos, blockState.setValue(LecternBlock.HAS_BOOK, false), 3)
                    return@register InteractionResult.SUCCESS
                }
            }

            // Right-click with enchanted book: Place on empty lectern
            if (itemInHand.`is`(Items.ENCHANTED_BOOK) && !hasBook) {
                val storedEnchants = itemInHand.get(DataComponents.STORED_ENCHANTMENTS)
                if (storedEnchants != null && !storedEnchants.isEmpty) {
                    // Check if ALL enchantments are stage 1-2
                    for (entry in storedEnchants.entrySet()) {
                        val enchantId = entry.key.unwrapKey().orElse(null)?.identifier()?.toString()
                        if (!EnchantmentEnforcement.isStage12Enchantment(enchantId)) {
                            player.displayClientMessage(
                                Component.literal("This enchantment requires an enchanting table!"),
                                true
                            )
                            return@register InteractionResult.FAIL
                        }
                    }

                    // Place book on lectern
                    if (blockEntity != null) {
                        blockEntity.setBook(itemInHand.copy())
                        level.setBlock(pos, blockState.setValue(LecternBlock.HAS_BOOK, true), 3)

                        // Remove book from player hand
                        itemInHand.shrink(1)

                        return@register InteractionResult.SUCCESS
                    }
                }
                return@register InteractionResult.PASS
            }

            // Right-click with gear: Apply enchantment from lectern book
            if (hasBook && blockEntity != null && !itemInHand.isEmpty && !itemInHand.`is`(Items.ENCHANTED_BOOK)) {
                val storedBook = blockEntity.book
                if (storedBook.isEmpty || !storedBook.`is`(Items.ENCHANTED_BOOK)) {
                    return@register InteractionResult.PASS
                }

                val storedEnchants = storedBook.get(DataComponents.STORED_ENCHANTMENTS)
                    ?: return@register InteractionResult.PASS

                if (storedEnchants.isEmpty) {
                    return@register InteractionResult.PASS
                }

                // Check level requirement
                if (player.experienceLevel < 10) {
                    player.displayClientMessage(
                        Component.literal("You must be level 10!"),
                        true
                    )
                    return@register InteractionResult.FAIL
                }

                // Check compatibility
                val targetEnchants = itemInHand.get(DataComponents.ENCHANTMENTS) ?: ItemEnchantments.EMPTY

                for (entry in storedEnchants.entrySet()) {
                    val enchantHolder = entry.key

                    // Already has this enchantment?
                    if (targetEnchants.getLevel(enchantHolder) > 0) {
                        player.displayClientMessage(
                            Component.literal("Already applied!"),
                            true
                        )
                        return@register InteractionResult.FAIL
                    }

                    // Can this enchantment go on this item?
                    if (!enchantHolder.value().canEnchant(itemInHand)) {
                        player.displayClientMessage(
                            Component.literal("Incompatible enchantment!"),
                            true
                        )
                        return@register InteractionResult.FAIL
                    }

                    // Conflicts with existing enchantments?
                    for (existing in targetEnchants.keySet()) {
                        if (!Enchantment.areCompatible(enchantHolder, existing)) {
                            player.displayClientMessage(
                                Component.literal("Incompatible enchantment!"),
                                true
                            )
                            return@register InteractionResult.FAIL
                        }
                    }
                }

                // Apply enchantments
                EnchantmentHelper.updateEnchantments(itemInHand) { mutable ->
                    for (entry in storedEnchants.entrySet()) {
                        mutable.set(entry.key, entry.intValue)
                    }
                }

                // Deduct levels
                player.giveExperienceLevels(-3)

                // Play feedback
                val serverLevel = level as? ServerLevel
                if (serverLevel != null) {
                    // Sound
                    serverLevel.playSound(
                        null,
                        pos,
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS,
                        1.0f,
                        1.0f
                    )

                    // Particles around player
                    val random = serverLevel.random
                    for (i in 0 until 15) {
                        serverLevel.sendParticles(
                            ParticleTypes.ENCHANT,
                            player.x + random.nextGaussian() * 0.3,
                            player.y + 1.0 + random.nextGaussian() * 0.3,
                            player.z + random.nextGaussian() * 0.3,
                            1, 0.0, 0.0, 0.0, 0.0
                        )
                    }
                }

                return@register InteractionResult.SUCCESS
            }

            // Let vanilla handle other interactions
            InteractionResult.PASS
        }
    }
}
