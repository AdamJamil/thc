package thc.client

import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import org.joml.Matrix4f
import thc.mixin.client.access.CameraAccessor

/**
 * Renders floating three-layer health bars above hostile mobs in world-space using billboard quads.
 *
 * Layer 1: Empty background (always full width)
 * Layer 2: Filled HP bar (clipped to current HP ratio)
 * Layer 3: Absorption overlay (only when mob has absorption)
 *
 * Also renders active status effect icons above the health bar, using the same visual style
 * as the player's effects GUI (frame + icon + duration overlay + roman numerals).
 *
 * Health bars are hidden when the mob is at full HP with no active effects and no absorption.
 */
object MobHealthBarRenderer {
    // Texture identifiers
    private val EMPTY_TEXTURE = Identifier.parse("thc:textures/item/health_bar_empty.png")
    private val FULL_TEXTURE = Identifier.parse("thc:textures/item/health_bar_full.png")
    private val ABSORPTION_TEXTURE = Identifier.parse("thc:textures/item/health_bar_absorption.png")

    // Texture dimensions (px): empty/full are 328x64, absorption is 82x16
    private const val TEX_WIDTH = 328f
    private const val TEX_HEIGHT = 64f

    // World-space render size: ~1.5 blocks wide, height derived from aspect ratio
    private const val BAR_WIDTH = 1.5f
    private const val BAR_HEIGHT = BAR_WIDTH * (TEX_HEIGHT / TEX_WIDTH) // ~0.293f

    // Range filter: 32 blocks (squared to avoid sqrt)
    private const val RANGE_SQ = 1024.0

    // Inset in texture pixels for the fillable region (8px borders on left and right)
    private const val INSET_PX = 8f
    private const val FILL_REGION_PX = TEX_WIDTH - INSET_PX - INSET_PX // 312px

    /**
     * Tracks original effect durations per mob for drain ratio calculation.
     * Outer key: entity ID, inner key: effect registry name, value: original duration in ticks.
     */
    private val originalDurations = mutableMapOf<Int, MutableMap<String, Int>>()

    /** Set of entity IDs rendered this frame, used for cleanup. */
    private val renderedEntityIds = mutableSetOf<Int>()

    fun render(context: WorldRenderContext) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.options.hideGui) return

        val level = client.level ?: return
        val camera = client.gameRenderer.mainCamera
        val cameraAccessor = camera as CameraAccessor
        val cameraPos = cameraAccessor.position
        val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(true)

        // Query monsters within 32-block range of the player
        val searchBox = AABB(
            player.x - 32.0, player.y - 32.0, player.z - 32.0,
            player.x + 32.0, player.y + 32.0, player.z + 32.0
        )
        val monsters = level.getEntities(
            EntityTypeTest.forClass(Monster::class.java),
            searchBox
        ) { true }

        renderedEntityIds.clear()

        for (mob in monsters) {
            // Range check (squared distance to player)
            if (mob.distanceToSqr(player) > RANGE_SQ) continue

            // Skip invisible mobs
            if (mob.isInvisible) continue

            // HBAR-06: Visibility gate - skip if full HP, no effects, no absorption
            if (mob.health >= mob.maxHealth
                && mob.activeEffects.isEmpty()
                && mob.absorptionAmount <= 0f
            ) continue

            renderedEntityIds.add(mob.id)
            renderHealthBar(mob, context, cameraPos, partialTick)
        }

        // Clean up duration tracking for mobs no longer rendered (died, moved out of range)
        originalDurations.keys.retainAll(renderedEntityIds)
    }

    private fun renderHealthBar(
        mob: Monster,
        context: WorldRenderContext,
        cameraPos: net.minecraft.world.phys.Vec3,
        partialTick: Float
    ) {
        val stack = context.matrices()
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val cameraAccessor = camera as CameraAccessor

        // Get interpolated mob position
        val mobPos = mob.getPosition(partialTick)

        stack.pushPose()

        // Translate to world position relative to camera, 0.5 blocks above mob head
        stack.translate(
            mobPos.x - cameraPos.x,
            mobPos.y - cameraPos.y + mob.bbHeight.toDouble() + 0.5,
            mobPos.z - cameraPos.z
        )

        // Billboard rotation: face the camera
        stack.mulPose(Axis.YP.rotationDegrees(-cameraAccessor.yRot))
        stack.mulPose(Axis.XP.rotationDegrees(cameraAccessor.xRot))

        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()

        val halfW = BAR_WIDTH / 2f
        val halfH = BAR_HEIGHT / 2f
        val pose = stack.last()
        val matrix = pose.pose()

        // Layer 1: Empty bar (always full width)
        renderQuad(
            bufferSource, matrix, pose,
            EMPTY_TEXTURE,
            -halfW, -halfH, halfW, halfH,
            0f, 0f, 1f, 1f
        )

        // Layer 2: Full bar (clipped to HP ratio)
        val hpRatio = (mob.health / mob.maxHealth).coerceIn(0f, 1f)
        if (hpRatio > 0f) {
            val renderWidth = (INSET_PX / TEX_WIDTH) * BAR_WIDTH + hpRatio * (FILL_REGION_PX / TEX_WIDTH) * BAR_WIDTH
            val uEnd = (INSET_PX + hpRatio * FILL_REGION_PX) / TEX_WIDTH

            renderQuad(
                bufferSource, matrix, pose,
                FULL_TEXTURE,
                -halfW, -halfH, -halfW + renderWidth, halfH,
                0f, 0f, uEnd, 1f
            )
        }

        // Layer 3: Absorption overlay (only if mob has absorption)
        if (mob.absorptionAmount > 0f) {
            val absRatio = (mob.absorptionAmount / mob.maxHealth).coerceIn(0f, 1f)
            val absWidth = absRatio * (FILL_REGION_PX / TEX_WIDTH) * BAR_WIDTH
            val absLeft = -halfW + (INSET_PX / TEX_WIDTH) * BAR_WIDTH

            renderQuad(
                bufferSource, matrix, pose,
                ABSORPTION_TEXTURE,
                absLeft, -halfH, absLeft + absWidth, halfH,
                0f, 0f, 1f, 1f
            )
        }

        // Render effect icons above the health bar
        renderEffects(mob, bufferSource, matrix, pose, halfW, halfH, partialTick)

        stack.popPose()
        bufferSource.endBatch()
    }

    /**
     * Renders active status effect icons left-to-right directly above the health bar.
     *
     * Each icon renders 4 layers as world-space textured quads:
     * 1. Frame (effect_frame.png)
     * 2. Mob effect icon (vanilla texture)
     * 3. Duration overlay (green, draining bottom-up)
     * 4. Roman numeral (for amplifier >= 1)
     */
    private fun renderEffects(
        mob: Monster,
        bufferSource: MultiBufferSource.BufferSource,
        matrix: Matrix4f,
        pose: com.mojang.blaze3d.vertex.PoseStack.Pose,
        halfW: Float,
        halfH: Float,
        partialTick: Float
    ) {
        // Collect and filter effects: exclude infinite-duration effects
        val effects = mob.activeEffects
            .filter { !it.isInfiniteDuration }
            .sortedWith(EffectsHudRenderer.effectComparator)

        if (effects.isEmpty()) {
            // Clean up duration tracking for this mob if no effects
            originalDurations.remove(mob.id)
            return
        }

        // Effect frame world-space size: proportional to health bar height
        // Health bar is BAR_HEIGHT tall (~0.293), frame is 44px in design, bar is 64px in texture
        val frameWorldSize = BAR_HEIGHT * (EffectsHudRenderer.BASE_FRAME_SIZE.toFloat() / TEX_HEIGHT)

        // Calculate total width of all effect icons and centering offset
        val totalWidth = effects.size * frameWorldSize
        val startX = -totalWidth / 2f
        // Effects render directly above the health bar (top edge = halfH)
        val baseY = halfH

        // Get or create duration map for this mob
        val mobDurations = originalDurations.getOrPut(mob.id) { mutableMapOf() }
        val activeKeys = mutableSetOf<String>()

        for ((index, effectInstance) in effects.withIndex()) {
            val effectKey = effectInstance.effect.unwrapKey().orElse(null) ?: continue
            val loc = effectKey.identifier()
            val effectName = loc.toString()
            activeKeys.add(effectName)

            val iconX = startX + index * frameWorldSize
            val iconY = baseY

            // Layer 1: Frame
            renderQuad(
                bufferSource, matrix, pose,
                EffectsHudRenderer.FRAME_TEXTURE,
                iconX, iconY, iconX + frameWorldSize, iconY + frameWorldSize,
                0f, 0f, 1f, 1f
            )

            // Layer 2: Mob effect icon (inset within frame)
            val iconOffset = frameWorldSize * (EffectsHudRenderer.ICON_OFFSET.toFloat() / EffectsHudRenderer.BASE_FRAME_SIZE.toFloat())
            val iconSize = frameWorldSize * (EffectsHudRenderer.ICON_SIZE.toFloat() / EffectsHudRenderer.BASE_FRAME_SIZE.toFloat())
            val iconTexture = Identifier.fromNamespaceAndPath(
                loc.namespace,
                "textures/mob_effect/${loc.path}.png"
            )
            renderQuad(
                bufferSource, matrix, pose,
                iconTexture,
                iconX + iconOffset, iconY + iconOffset,
                iconX + iconOffset + iconSize, iconY + iconOffset + iconSize,
                0f, 0f, 1f, 1f
            )

            // Layer 3: Duration overlay (green, filling from bottom upward)
            val durationRatio = computeDurationRatio(effectInstance, effectName, mobDurations, partialTick)
            if (durationRatio > 0f) {
                val overlayHeight = iconSize * durationRatio
                renderColoredQuad(
                    bufferSource, matrix, pose,
                    iconX + iconOffset,
                    iconY + iconOffset + iconSize - overlayHeight,
                    iconX + iconOffset + iconSize,
                    iconY + iconOffset + iconSize,
                    EffectsHudRenderer.OVERLAY_COLOR
                )
            }

            // Layer 4: Roman numeral (for amplifier >= 1, <= 9)
            val amplifier = effectInstance.amplifier
            if (amplifier in 1..9) {
                val numeralOffsetX = frameWorldSize * (EffectsHudRenderer.NUMERAL_X.toFloat() / EffectsHudRenderer.BASE_FRAME_SIZE.toFloat())
                val numeralOffsetY = frameWorldSize * (EffectsHudRenderer.NUMERAL_Y.toFloat() / EffectsHudRenderer.BASE_FRAME_SIZE.toFloat())
                val numeralWorldW = frameWorldSize * (EffectsHudRenderer.NUMERAL_SRC_WIDTH.toFloat() / EffectsHudRenderer.BASE_FRAME_SIZE.toFloat())
                val numeralWorldH = frameWorldSize * (EffectsHudRenderer.NUMERAL_SRC_HEIGHT.toFloat() / EffectsHudRenderer.BASE_FRAME_SIZE.toFloat())

                // UV coordinates into the 13x90 numeral spritesheet
                val vStart = (amplifier * EffectsHudRenderer.NUMERAL_SRC_HEIGHT).toFloat() / EffectsHudRenderer.NUMERAL_SHEET_HEIGHT.toFloat()
                val vEnd = vStart + EffectsHudRenderer.NUMERAL_SRC_HEIGHT.toFloat() / EffectsHudRenderer.NUMERAL_SHEET_HEIGHT.toFloat()

                renderQuad(
                    bufferSource, matrix, pose,
                    EffectsHudRenderer.NUMERALS_TEXTURE,
                    iconX + numeralOffsetX, iconY + numeralOffsetY,
                    iconX + numeralOffsetX + numeralWorldW, iconY + numeralOffsetY + numeralWorldH,
                    0f, vStart, 1f, vEnd
                )
            }
        }

        // Clean up stale effect entries for this mob
        mobDurations.keys.retainAll(activeKeys)
    }

    /**
     * Computes the duration drain ratio for a mob's effect instance, matching the
     * EffectsHudRenderer logic: track original duration, compute sub-tick interpolated ratio.
     */
    private fun computeDurationRatio(
        effectInstance: MobEffectInstance,
        effectName: String,
        mobDurations: MutableMap<String, Int>,
        partialTick: Float
    ): Float {
        val remaining = effectInstance.duration
        val stored = mobDurations[effectName]
        if (stored == null || remaining > stored) {
            mobDurations[effectName] = remaining
        }
        val original = mobDurations[effectName]!!.toFloat()
        return if (original <= 0f) {
            0f
        } else {
            val effectiveRemaining = (remaining.toFloat() - (1.0f - partialTick)).coerceAtLeast(0f)
            (effectiveRemaining / original).coerceIn(0f, 1f)
        }
    }

    /**
     * Renders a single textured quad with 4 vertices.
     * Uses entityTranslucent render type for proper alpha blending.
     */
    private fun renderQuad(
        bufferSource: MultiBufferSource.BufferSource,
        matrix: Matrix4f,
        pose: com.mojang.blaze3d.vertex.PoseStack.Pose,
        texture: Identifier,
        x1: Float, y1: Float, x2: Float, y2: Float,
        u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        val buffer = bufferSource.getBuffer(RenderTypes.entityTranslucent(texture))

        // Bottom-left
        buffer.addVertex(matrix, x1, y1, 0f)
            .setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v2)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)

        // Bottom-right
        buffer.addVertex(matrix, x2, y1, 0f)
            .setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v2)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)

        // Top-right
        buffer.addVertex(matrix, x2, y2, 0f)
            .setColor(1f, 1f, 1f, 1f)
            .setUv(u2, v1)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)

        // Top-left
        buffer.addVertex(matrix, x1, y2, 0f)
            .setColor(1f, 1f, 1f, 1f)
            .setUv(u1, v1)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)
    }

    /**
     * Renders a solid-colored quad for the duration overlay.
     * Uses entityTranslucent with a white pixel texture and vertex coloring for ARGB.
     */
    private fun renderColoredQuad(
        bufferSource: MultiBufferSource.BufferSource,
        matrix: Matrix4f,
        pose: com.mojang.blaze3d.vertex.PoseStack.Pose,
        x1: Float, y1: Float, x2: Float, y2: Float,
        argbColor: Int
    ) {
        // Extract ARGB components
        val a = ((argbColor shr 24) and 0xFF) / 255f
        val r = ((argbColor shr 16) and 0xFF) / 255f
        val g = ((argbColor shr 8) and 0xFF) / 255f
        val b = (argbColor and 0xFF) / 255f

        // Use the frame texture as a proxy (we only need vertex coloring, UVs map to a solid area)
        // entityTranslucent with the frame texture — the overlay will tint with the green color
        // Using a 1x1 white pixel would be ideal, but we can use any texture with vertex color override
        val buffer = bufferSource.getBuffer(RenderTypes.entityTranslucent(EffectsHudRenderer.FRAME_TEXTURE))

        // Map all UVs to the center of the frame texture to get a roughly solid sample
        val uMid = 0.5f
        val vMid = 0.5f

        buffer.addVertex(matrix, x1, y1, 0.001f)
            .setColor(r, g, b, a)
            .setUv(uMid, vMid)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)

        buffer.addVertex(matrix, x2, y1, 0.001f)
            .setColor(r, g, b, a)
            .setUv(uMid, vMid)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)

        buffer.addVertex(matrix, x2, y2, 0.001f)
            .setColor(r, g, b, a)
            .setUv(uMid, vMid)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)

        buffer.addVertex(matrix, x1, y2, 0.001f)
            .setColor(r, g, b, a)
            .setUv(uMid, vMid)
            .setOverlay(0)
            .setLight(15728880)
            .setNormal(pose, 0f, 0f, 1f)
    }
}
