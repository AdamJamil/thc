package thc.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders radial progress ring around cursor when looking at downed player.
 * Empty ring shows as background, filled ring sweeps clockwise from 12 o'clock.
 *
 * Uses rotation transforms with multiple blit calls to achieve radial fill effect.
 * Each segment is a thin slice of the texture, rotated to create the pie shape.
 */
object RevivalProgressRenderer {
    val REVIVAL_PROGRESS_ID: Identifier = Identifier.fromNamespaceAndPath("thc", "revival_progress")

    private val EMPTY_TEXTURE = Identifier.fromNamespaceAndPath("thc", "textures/item/revival_progress_empty.png")
    private val FULL_TEXTURE = Identifier.fromNamespaceAndPath("thc", "textures/item/revival_progress_full.png")

    private const val TEXTURE_SIZE = 32
    private const val RENDER_SIZE = 32
    private const val HALF_SIZE = RENDER_SIZE / 2

    // Number of segments for radial fill (more = smoother)
    private const val SEGMENTS = 64

    // Width of each segment in pixels (thin strips from center to edge)
    private const val SEGMENT_WIDTH = 2

    fun render(guiGraphics: GuiGraphics) {
        val client = Minecraft.getInstance()
        if (client.options.hideGui || client.player?.isSpectator == true) {
            return
        }

        if (!RevivalClientState.hasTarget()) {
            return
        }

        val progress = RevivalClientState.getProgress()

        val screenCenterX = guiGraphics.guiWidth() / 2
        val screenCenterY = guiGraphics.guiHeight() / 2

        // Always draw empty ring as full background
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            EMPTY_TEXTURE,
            screenCenterX - HALF_SIZE,
            screenCenterY - HALF_SIZE,
            0.0f,
            0.0f,
            RENDER_SIZE,
            RENDER_SIZE,
            TEXTURE_SIZE,
            TEXTURE_SIZE
        )

        // Draw filled ring with radial progress using segmented approach
        if (progress > 0.0) {
            val segmentsToRender = (progress * SEGMENTS).toInt().coerceIn(0, SEGMENTS)

            val matrices = guiGraphics.pose()

            for (i in 0 until segmentsToRender) {
                // Calculate angle for this segment (0 = top/12 o'clock, going clockwise)
                val angle = (i.toDouble() / SEGMENTS) * 360.0 - 90.0  // -90 to start at top

                matrices.pushMatrix()

                // Translate to screen center
                matrices.translate(screenCenterX.toFloat(), screenCenterY.toFloat())

                // Rotate around center
                matrices.rotate(Math.toRadians(angle).toFloat())

                // Draw a thin vertical strip from the texture
                // The strip is at the "top" of the texture (12 o'clock position)
                // After rotation, it appears at the correct angle

                // Calculate UV coordinates for this segment's strip
                // We sample a thin vertical strip from the top-center of the texture
                val uOffset = (TEXTURE_SIZE / 2 - SEGMENT_WIDTH / 2).toFloat()

                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    FULL_TEXTURE,
                    -SEGMENT_WIDTH / 2,  // x: centered on rotation point
                    -HALF_SIZE,          // y: from top (after rotation this extends outward)
                    uOffset,             // u: sample from center-top of texture
                    0.0f,                // v: from top of texture
                    SEGMENT_WIDTH,       // width: thin strip
                    HALF_SIZE,           // height: half the texture (center to edge)
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
                )

                matrices.popMatrix()
            }

            // For antialiasing at the leading edge, render one more segment with reduced opacity
            if (segmentsToRender < SEGMENTS && progress > 0.0) {
                val partialProgress = (progress * SEGMENTS) - segmentsToRender
                if (partialProgress > 0.1) {
                    val angle = (segmentsToRender.toDouble() / SEGMENTS) * 360.0 - 90.0

                    matrices.pushMatrix()
                    matrices.translate(screenCenterX.toFloat(), screenCenterY.toFloat())
                    matrices.rotate(Math.toRadians(angle).toFloat())

                    // Draw partial segment (approximated by drawing full segment - visual AA)
                    val uOffset = (TEXTURE_SIZE / 2 - SEGMENT_WIDTH / 2).toFloat()
                    val partialHeight = (HALF_SIZE * partialProgress).toInt().coerceAtLeast(1)

                    guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        FULL_TEXTURE,
                        -SEGMENT_WIDTH / 2,
                        -HALF_SIZE,
                        uOffset,
                        0.0f,
                        SEGMENT_WIDTH,
                        partialHeight,
                        TEXTURE_SIZE,
                        TEXTURE_SIZE
                    )

                    matrices.popMatrix()
                }
            }
        }
    }
}
