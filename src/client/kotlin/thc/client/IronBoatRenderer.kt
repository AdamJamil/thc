package thc.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.Model
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.BoatRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.BoatRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Unit
import thc.entity.IronBoat

/**
 * Renderer for IronBoat that extends vanilla BoatRenderer.
 * Uses the oak boat model geometry but with a custom iron texture.
 * Also renders a fluid mask patch to prevent lava from appearing inside the boat.
 */
class IronBoatRenderer(context: EntityRendererProvider.Context) :
    BoatRenderer(context, ModelLayers.OAK_BOAT) {

    companion object {
        // Custom texture for iron boat
        val IRON_BOAT_TEXTURE: Identifier = Identifier.fromNamespaceAndPath("thc", "textures/entity/boat/iron.png")

        // Flag to track if we're currently rendering an iron boat
        // Used by extractRenderState to detect iron boats
        @JvmField
        @Volatile
        var renderingIronBoat = false
    }

    // Create our own patch model for fluid masking
    // We need our own because parent's waterPatchModel is private
    // For water: use waterMask() which works with translucent water rendering
    // For lava: use entitySolid() which renders an opaque surface to cover lava
    private val waterPatchModel: Model.Simple = Model.Simple(
        context.bakeLayer(ModelLayers.BOAT_WATER_PATCH)
    ) { RenderTypes.waterMask() }

    // Opaque patch model for lava - renders a solid surface that covers lava
    private val lavaPatchModel: Model.Simple = Model.Simple(
        context.bakeLayer(ModelLayers.BOAT_WATER_PATCH)
    ) { identifier -> RenderTypes.entitySolid(identifier) }

    /**
     * Override renderType to use the iron boat texture instead of oak.
     * The parent class computes texture from ModelLayerLocation but we need custom texture.
     */
    override fun renderType(): RenderType {
        return model().renderType(IRON_BOAT_TEXTURE)
    }

    /**
     * Override to render fluid covering patches when floating.
     * For iron boats, we render:
     * 1. An opaque solid patch to cover lava (lava is rendered before entities)
     * 2. A water mask patch to handle water (water is rendered after entities)
     */
    override fun submitTypeAdditions(
        state: BoatRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int
    ) {
        // Only render patches if not fully submerged
        if (!state.isUnderWater) {
            // Render opaque lava patch - this physically covers lava with a solid surface
            // Uses entitySolid render type which draws an opaque floor inside the boat
            collector.submitModel(
                lavaPatchModel,
                Unit.INSTANCE,
                poseStack,
                lavaPatchModel.renderType(IRON_BOAT_TEXTURE),
                light,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
            )

            // Also render water mask patch for when on water (same as vanilla boats)
            collector.submitModel(
                waterPatchModel,
                Unit.INSTANCE,
                poseStack,
                waterPatchModel.renderType(IRON_BOAT_TEXTURE),
                light,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
            )
        }
    }

    override fun extractRenderState(
        entity: net.minecraft.world.entity.vehicle.boat.AbstractBoat,
        state: BoatRenderState,
        partialTick: Float
    ) {
        // Set flag to identify iron boat rendering (may be used by other systems)
        renderingIronBoat = entity is IronBoat
        super.extractRenderState(entity, state, partialTick)
    }
}
