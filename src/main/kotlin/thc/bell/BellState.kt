package thc.bell

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BellBlockEntity
import thc.THCAttachments

object BellState {
    private fun target(level: Level, pos: BlockPos): AttachmentTarget? {
        val blockEntity = level.getBlockEntity(pos)
        return if (blockEntity is BellBlockEntity) {
            blockEntity as AttachmentTarget
        } else {
            null
        }
    }

    @JvmStatic
    fun isActivated(level: Level, pos: BlockPos): Boolean {
        val entity = target(level, pos) ?: return false
        val value = entity.getAttachedOrCreate(THCAttachments.BELL_ACTIVATED)
        return value != null && value
    }

    @JvmStatic
    fun setActivated(level: Level, pos: BlockPos, activated: Boolean) {
        val entity = target(level, pos) ?: return
        entity.setAttached(THCAttachments.BELL_ACTIVATED, activated)
    }
}
