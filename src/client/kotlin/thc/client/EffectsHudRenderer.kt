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
    private val NUMERALS_TEXTURE = Identifier.fromNamespaceAndPath("thc", "textures/item/numerals.png")

    // Base design / texture source constants
    private const val BASE_FRAME_SIZE = 44
    private const val ICON_SOURCE_SIZE = 18
    private const val NUMERAL_SRC_WIDTH = 13
    private const val NUMERAL_SRC_HEIGHT = 9
    private const val NUMERAL_SHEET_HEIGHT = 90 // 10 numerals * 9px each

    // Ratios derived from original 44px base design
    private const val ICON_RATIO = 36.0 / 44.0       // icon render size relative to frame
    private const val ICON_OFFSET_RATIO = 4.0 / 44.0 // icon offset relative to frame
    private const val MARGIN_RATIO = 4.0 / 44.0      // margin relative to frame
    private const val NUMERAL_W_RATIO = 13.0 / 44.0  // numeral width relative to frame
    private const val NUMERAL_H_RATIO = 9.0 / 44.0   // numeral height relative to frame
    private const val NUMERAL_OX_RATIO = 5.0 / 44.0  // numeral offset X relative to frame
    private const val NUMERAL_OY_RATIO = 5.0 / 44.0  // numeral offset Y relative to frame

    private const val OVERLAY_COLOR = 0x3300FF00.toInt() // Green with 20% alpha (ARGB)

    /** Tracks the original duration for each effect to compute drain ratio. */
    private val originalDurations = mutableMapOf<String, Int>()

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
            // Clean up all tracked durations when no effects are active
            originalDurations.clear()
            return
        }

        val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val sorted = activeEffects
            .filter { !it.isInfiniteDuration }
            .sortedWith(effectComparator)

        if (sorted.isEmpty()) {
            originalDurations.clear()
            return
        }

        // Build set of currently active effect keys for cleanup
        val activeKeys = mutableSetOf<String>()

        // Compute dynamic sizes from config scale percentage
        val screenWidth = guiGraphics.guiWidth()
        val frameSize = (screenWidth * EffectsGuiConfig.getScalePercent() / 100.0).toInt().coerceAtLeast(16)
        val iconRenderSize = (frameSize * ICON_RATIO).toInt()
        val iconOffset = (frameSize * ICON_OFFSET_RATIO).toInt()
        val margin = (frameSize * MARGIN_RATIO).toInt().coerceAtLeast(2)
        val numeralWidth = (frameSize * NUMERAL_W_RATIO).toInt()
        val numeralHeight = (frameSize * NUMERAL_H_RATIO).toInt()
        val numeralOffsetX = (frameSize * NUMERAL_OX_RATIO).toInt()
        val numeralOffsetY = (frameSize * NUMERAL_OY_RATIO).toInt()

        val startX = margin
        val screenHeight = guiGraphics.guiHeight()
        // Bottom of first (highest-priority) frame sits at screenHeight - margin
        val baseY = screenHeight - margin - frameSize

        for ((index, effectInstance) in sorted.withIndex()) {
            val y = baseY - (index * frameSize)

            // Render frame (12-param blit: decouples render size from source region)
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FRAME_TEXTURE,
                startX,
                y,
                0.0f,
                0.0f,
                frameSize, frameSize,                   // render size (dynamic)
                BASE_FRAME_SIZE, BASE_FRAME_SIZE,       // source region (44x44 = full texture)
                BASE_FRAME_SIZE, BASE_FRAME_SIZE        // texture dimensions (44x44)
            )

            // Render vanilla mob effect icon scaled and centered in frame
            val effectKey = effectInstance.effect.unwrapKey().orElse(null)
            if (effectKey != null) {
                val loc = effectKey.identifier()
                val effectName = loc.toString()
                activeKeys.add(effectName)

                val iconTexture = Identifier.fromNamespaceAndPath(
                    loc.namespace,
                    "textures/mob_effect/${loc.path}.png"
                )
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    iconTexture,
                    startX + iconOffset,
                    y + iconOffset,
                    0.0f,
                    0.0f,
                    iconRenderSize, iconRenderSize,     // render size (dynamic)
                    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, // source region (18x18 = full texture)
                    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE  // texture dimensions (18x18)
                )

                // Duration overlay
                renderDurationOverlay(guiGraphics, effectInstance, effectName, startX, y, partialTick, iconRenderSize, iconOffset)

                // Roman numeral for amplifier >= 1
                renderAmplifierNumeral(guiGraphics, effectInstance, startX, y, numeralWidth, numeralHeight, numeralOffsetX, numeralOffsetY)
            }
        }

        // Clean up stale entries from originalDurations
        originalDurations.keys.retainAll(activeKeys)
    }

    private fun renderDurationOverlay(
        guiGraphics: GuiGraphics,
        effectInstance: MobEffectInstance,
        effectName: String,
        frameX: Int,
        frameY: Int,
        partialTick: Float,
        iconRenderSize: Int,
        iconOffset: Int
    ) {
        // Infinite effects are filtered out before rendering, so only finite effects reach here
        val remaining = effectInstance.duration
        // Update original duration tracking:
        // Store when first seen or when re-applied with higher duration
        val stored = originalDurations[effectName]
        if (stored == null || remaining > stored) {
            originalDurations[effectName] = remaining
        }
        val original = originalDurations[effectName]!!.toFloat()
        val ratio: Float = if (original <= 0f) {
            0f
        } else {
            // Sub-tick interpolation: subtract fractional tick not yet elapsed
            val effectiveRemaining = (remaining.toFloat() - (1.0f - partialTick)).coerceAtLeast(0f)
            (effectiveRemaining / original).coerceIn(0f, 1f)
        }

        val overlayHeight = (iconRenderSize * ratio).toInt()
        if (overlayHeight <= 0) return

        val iconX = frameX + iconOffset
        val iconY = frameY + iconOffset

        // Fill from bottom upward within the icon area
        guiGraphics.fill(
            iconX,
            iconY + iconRenderSize - overlayHeight,
            iconX + iconRenderSize,
            iconY + iconRenderSize,
            OVERLAY_COLOR
        )
    }

    private fun renderAmplifierNumeral(
        guiGraphics: GuiGraphics,
        effectInstance: MobEffectInstance,
        frameX: Int,
        frameY: Int,
        numeralWidth: Int,
        numeralHeight: Int,
        numeralOffsetX: Int,
        numeralOffsetY: Int
    ) {
        val amplifier = effectInstance.amplifier
        if (amplifier < 1 || amplifier > 9) return

        val numeralX = frameX + numeralOffsetX
        val numeralY = frameY + numeralOffsetY

        // Source sheet uses original 13x9 per numeral, 10 rows = 90px total
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            NUMERALS_TEXTURE,
            numeralX,
            numeralY,
            0.0f,
            (amplifier * NUMERAL_SRC_HEIGHT).toFloat(),
            numeralWidth, numeralHeight,                // render size (dynamic)
            NUMERAL_SRC_WIDTH, NUMERAL_SRC_HEIGHT,      // source region (13x9 = one numeral)
            NUMERAL_SRC_WIDTH, NUMERAL_SHEET_HEIGHT     // texture dimensions (13x90 = full sheet)
        )
    }
}
