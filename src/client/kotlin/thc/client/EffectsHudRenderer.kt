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

    // Design-space constants (positions within the 44x44 frame)
    private const val ICON_SIZE = 36         // icon render size in design space
    private const val ICON_OFFSET = 4        // icon inset from frame edge
    private const val MARGIN_RATIO = 4.0 / 44.0  // screen margin relative to frame
    private const val NUMERAL_X = 5          // numeral X in design space
    private const val NUMERAL_Y = 5          // numeral Y in design space

    private const val OVERLAY_COLOR = 0x5A00FF00.toInt() // Green with ~35% alpha (ARGB)

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

        // Compute screen-space positioning
        val screenWidth = guiGraphics.guiWidth()
        val frameSize = (screenWidth * EffectsGuiConfig.getScalePercent() / 100.0).toInt().coerceAtLeast(16)
        val scaleFactor = frameSize.toFloat() / BASE_FRAME_SIZE.toFloat()
        val margin = (frameSize * MARGIN_RATIO).toInt().coerceAtLeast(2)

        val startX = margin
        val screenHeight = guiGraphics.guiHeight()
        val baseY = screenHeight - margin - frameSize

        for ((index, effectInstance) in sorted.withIndex()) {
            val y = baseY - (index * frameSize)

            // Scale to design space — all drawing uses base 44x44 coordinates
            val matrices = guiGraphics.pose()
            matrices.pushMatrix()
            matrices.translate(startX.toFloat(), y.toFloat())
            matrices.scale(scaleFactor, scaleFactor)

            // Render frame at design coordinates (1:1 with texture)
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FRAME_TEXTURE,
                0, 0,
                0.0f, 0.0f,
                BASE_FRAME_SIZE, BASE_FRAME_SIZE,
                BASE_FRAME_SIZE, BASE_FRAME_SIZE,
                BASE_FRAME_SIZE, BASE_FRAME_SIZE
            )

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
                    ICON_OFFSET, ICON_OFFSET,
                    0.0f, 0.0f,
                    ICON_SIZE, ICON_SIZE,
                    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE,
                    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE
                )

                // Duration overlay (covers the 36x36 icon area)
                renderDurationOverlay(guiGraphics, effectInstance, effectName, partialTick)

                // Roman numeral for amplifier >= 1
                renderAmplifierNumeral(guiGraphics, effectInstance)
            }

            matrices.popMatrix()
        }

        // Clean up stale entries from originalDurations
        originalDurations.keys.retainAll(activeKeys)
    }

    private fun renderDurationOverlay(
        guiGraphics: GuiGraphics,
        effectInstance: MobEffectInstance,
        effectName: String,
        partialTick: Float
    ) {
        val remaining = effectInstance.duration
        val stored = originalDurations[effectName]
        if (stored == null || remaining > stored) {
            originalDurations[effectName] = remaining
        }
        val original = originalDurations[effectName]!!.toFloat()
        val ratio: Float = if (original <= 0f) {
            0f
        } else {
            val effectiveRemaining = (remaining.toFloat() - (1.0f - partialTick)).coerceAtLeast(0f)
            (effectiveRemaining / original).coerceIn(0f, 1f)
        }

        val overlayHeight = (ICON_SIZE * ratio).toInt()
        if (overlayHeight <= 0) return

        // Fill from bottom upward within the icon area (design-space coordinates)
        guiGraphics.fill(
            ICON_OFFSET,
            ICON_OFFSET + ICON_SIZE - overlayHeight,
            ICON_OFFSET + ICON_SIZE,
            ICON_OFFSET + ICON_SIZE,
            OVERLAY_COLOR
        )
    }

    private fun renderAmplifierNumeral(
        guiGraphics: GuiGraphics,
        effectInstance: MobEffectInstance
    ) {
        val amplifier = effectInstance.amplifier
        if (amplifier < 1 || amplifier > 9) return

        // All coordinates in design space — pose stack handles scaling
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            NUMERALS_TEXTURE,
            NUMERAL_X, NUMERAL_Y,
            0.0f,
            (amplifier * NUMERAL_SRC_HEIGHT).toFloat(),
            NUMERAL_SRC_WIDTH, NUMERAL_SRC_HEIGHT,
            NUMERAL_SRC_WIDTH, NUMERAL_SRC_HEIGHT,
            NUMERAL_SRC_WIDTH, NUMERAL_SHEET_HEIGHT
        )
    }
}
