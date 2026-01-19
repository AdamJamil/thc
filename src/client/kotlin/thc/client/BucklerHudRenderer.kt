package thc.client

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import thc.buckler.BucklerStatsRegistry
import thc.item.BucklerItem

object BucklerHudRenderer {
    val POISE_ID: Identifier = Identifier.fromNamespaceAndPath("thc", "poise_bar")
    private val BUCKLER_FULL = Identifier.fromNamespaceAndPath("thc", "textures/item/stone_buckler.png")
    private val BUCKLER_HALF = Identifier.fromNamespaceAndPath("thc", "textures/item/stone_buckler_half.png")
    private val BUCKLER_EMPTY = Identifier.fromNamespaceAndPath("thc", "textures/item/stone_buckler_empty.png")

    private const val ICON_RENDER_SIZE = 9
    private const val ICON_SPACING = 8
    private const val BAR_HEIGHT = 10
    private const val FULL_DISPLAY_TICKS = 160

    fun render(guiGraphics: GuiGraphics) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.options.hideGui || player.isSpectator) {
            return
        }

        val offhand = player.offhandItem
        val maxPoise = resolveMaxPoise(player, offhand)
        val poise = BucklerClientState.getPoise().coerceAtLeast(0.0)
        val lastFullTick = BucklerClientState.getLastFullTick()
        val tick = player.level().gameTime

        if (maxPoise <= 0.0) {
            return
        }

        if (!shouldRender(poise, maxPoise, lastFullTick, tick)) {
            return
        }

        val maxHalf = kotlin.math.round(maxPoise * 2.0).toInt()
        val totalIcons = (maxHalf + 1) / 2
        val poiseHalf = kotlin.math.floor(poise * 2.0).toInt()

        val left = (guiGraphics.guiWidth() / 2) - 91
        val top = guiGraphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(POISE_ID)
        for (i in 0 until totalIcons) {
            val x = left + (i * ICON_SPACING)
            val iconHalf = poiseHalf - (i * 2)
            val icon = if (iconHalf >= 2) BUCKLER_FULL else if (iconHalf == 1) BUCKLER_HALF else BUCKLER_EMPTY
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                x,
                top,
                0.0f,
                0.0f,
                ICON_RENDER_SIZE,
                ICON_RENDER_SIZE,
                16,
                16,
                16,
                16
            )
        }
    }

    fun getRenderHeight(player: Player?): Int {
        if (player == null) {
            return 0
        }
        val maxPoise = resolveMaxPoise(player, player.offhandItem)
        if (maxPoise <= 0.0) {
            return 0
        }
        val poise = BucklerClientState.getPoise().coerceAtLeast(0.0)
        val lastFullTick = BucklerClientState.getLastFullTick()
        val tick = player.level().gameTime
        return if (shouldRender(poise, maxPoise, lastFullTick, tick)) BAR_HEIGHT else 0
    }

    private fun resolveMaxPoise(player: Player, offhand: net.minecraft.world.item.ItemStack): Double {
        var maxPoise = BucklerClientState.getMaxPoise()
        if (maxPoise <= 0.0 && BucklerItem.isBuckler(offhand)) {
            val stats = BucklerStatsRegistry.forStack(offhand)
            if (stats != null) {
                maxPoise = stats.maxPoiseHearts
            }
        }
        return maxPoise
    }

    private fun shouldRender(poise: Double, maxPoise: Double, lastFullTick: Long, tick: Long): Boolean {
        val isFull = poise >= maxPoise - 1.0E-4
        if (!isFull) {
            return true
        }
        return lastFullTick < 0L || tick - lastFullTick <= FULL_DISPLAY_TICKS
    }
}
