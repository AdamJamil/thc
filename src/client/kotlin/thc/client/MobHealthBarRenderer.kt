package thc.client

import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
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

            renderHealthBar(mob, context, cameraPos, partialTick)
        }
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

        stack.popPose()
        bufferSource.endBatch()
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
}
