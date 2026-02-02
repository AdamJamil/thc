package thc.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

/**
 * Renders radial progress ring around cursor when looking at downed player.
 * Empty ring shows as background, filled ring fills based on progress.
 *
 * Current implementation: top-down vertical fill (top portion reveals first).
 * This approximates the "starting from 12 o'clock" concept.
 * True radial/pie fill requires custom shader work beyond standard blit API.
 */
object RevivalProgressRenderer {
    val REVIVAL_PROGRESS_ID: Identifier = Identifier.fromNamespaceAndPath("thc", "revival_progress")

    private val EMPTY_TEXTURE = Identifier.fromNamespaceAndPath("thc", "textures/item/revival_progress_empty.png")
    private val FULL_TEXTURE = Identifier.fromNamespaceAndPath("thc", "textures/item/revival_progress_full.png")

    // Texture is 32x32, render at 1:1 scale (user can tune this)
    private const val TEXTURE_SIZE = 32
    private const val RENDER_SIZE = 32
    private const val HALF_SIZE = RENDER_SIZE / 2

    fun render(guiGraphics: GuiGraphics) {
        val client = Minecraft.getInstance()
        if (client.options.hideGui || client.player?.isSpectator == true) {
            return
        }

        // Check if we have a revival target
        if (!RevivalClientState.hasTarget()) {
            return
        }

        val progress = RevivalClientState.getProgress()

        val centerX = guiGraphics.guiWidth() / 2
        val centerY = guiGraphics.guiHeight() / 2

        // Always render empty ring as background
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            EMPTY_TEXTURE,
            centerX - HALF_SIZE,
            centerY - HALF_SIZE,
            0.0f,
            0.0f,
            RENDER_SIZE,
            RENDER_SIZE,
            TEXTURE_SIZE,
            TEXTURE_SIZE
        )

        // Render filled ring based on progress (fills from top down)
        if (progress > 0.0) {
            // Calculate how many pixels to show from the top
            val visibleHeight = (RENDER_SIZE * progress).toInt().coerceIn(1, RENDER_SIZE)

            // Render partial texture from top down
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FULL_TEXTURE,
                centerX - HALF_SIZE,
                centerY - HALF_SIZE,  // Start at top
                0.0f,
                0.0f,  // UV starts at top of texture
                RENDER_SIZE,
                visibleHeight,  // Only show this many pixels
                TEXTURE_SIZE,
                TEXTURE_SIZE
            )
        }
    }
}
