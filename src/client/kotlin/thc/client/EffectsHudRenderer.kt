package thc.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects

object EffectsHudRenderer {
    val EFFECTS_HUD_ID: Identifier = Identifier.fromNamespaceAndPath("thc", "effects_hud")

    private val FRAME_TEXTURE = Identifier.fromNamespaceAndPath("thc", "textures/item/effect_frame.png")

    private const val FRAME_SIZE = 44
    private const val ICON_RENDER_SIZE = 36
    private const val ICON_SOURCE_SIZE = 18
    private const val ICON_OFFSET = 4 // (44 - 36) / 2
    private const val MARGIN = 4

    /** Lower number = higher priority = rendered closer to bottom (first in stack). */
    private val PRIORITY_MAP: Map<ResourceKey<MobEffect>, Int> = buildPriorityMap()

    private fun buildPriorityMap(): Map<ResourceKey<MobEffect>, Int> {
        val map = mutableMapOf<ResourceKey<MobEffect>, Int>()
        val entries = listOf(
            MobEffects.WITHER to 0,
            MobEffects.POISON to 1,
            MobEffects.RESISTANCE to 2,
            MobEffects.ABSORPTION to 3,
            MobEffects.STRENGTH to 4,
            MobEffects.SLOWNESS to 5,
            MobEffects.WEAKNESS to 6,
            MobEffects.SPEED to 7
        )
        for ((holder, priority) in entries) {
            val key = holder.unwrapKey().orElse(null)
            if (key != null) {
                map[key] = priority
            }
        }
        return map
    }

    private val effectComparator = Comparator<MobEffectInstance> { a, b ->
        val keyA = a.effect.unwrapKey().orElse(null)
        val keyB = b.effect.unwrapKey().orElse(null)
        val priorityA = if (keyA != null) PRIORITY_MAP.getOrDefault(keyA, 100) else 100
        val priorityB = if (keyB != null) PRIORITY_MAP.getOrDefault(keyB, 100) else 100
        if (priorityA != priorityB) {
            priorityA.compareTo(priorityB)
        } else {
            // Secondary sort by registry name for deterministic ordering
            val nameA = keyA?.identifier()?.toString() ?: ""
            val nameB = keyB?.identifier()?.toString() ?: ""
            nameA.compareTo(nameB)
        }
    }

    fun render(guiGraphics: GuiGraphics) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.options.hideGui || player.isSpectator) {
            return
        }

        val activeEffects = player.activeEffects
        if (activeEffects.isEmpty()) {
            return
        }

        val sorted = activeEffects.sortedWith(effectComparator)

        val startX = MARGIN
        val screenHeight = guiGraphics.guiHeight()
        // Bottom of first (highest-priority) frame sits at screenHeight - MARGIN
        val baseY = screenHeight - MARGIN - FRAME_SIZE

        for ((index, effectInstance) in sorted.withIndex()) {
            val y = baseY - (index * FRAME_SIZE)

            // Render frame
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FRAME_TEXTURE,
                startX,
                y,
                0.0f,
                0.0f,
                FRAME_SIZE,
                FRAME_SIZE,
                FRAME_SIZE,
                FRAME_SIZE
            )

            // Render vanilla mob effect icon at 2x scale centered in frame
            val effectKey = effectInstance.effect.unwrapKey().orElse(null)
            if (effectKey != null) {
                val loc = effectKey.identifier()
                val iconTexture = Identifier.fromNamespaceAndPath(
                    loc.namespace,
                    "textures/mob_effect/${loc.path}.png"
                )
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    iconTexture,
                    startX + ICON_OFFSET,
                    y + ICON_OFFSET,
                    0.0f,
                    0.0f,
                    ICON_RENDER_SIZE,
                    ICON_RENDER_SIZE,
                    ICON_SOURCE_SIZE,
                    ICON_SOURCE_SIZE
                )
            }
        }
    }
}
