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

    // Create our own water/lava patch model for fluid masking
    // We need our own because parent's waterPatchModel is private
    private val fluidPatchModel: Model.Simple = Model.Simple(
        context.bakeLayer(ModelLayers.BOAT_WATER_PATCH)
    ) { RenderTypes.waterMask() }

    /**
     * Override renderType to use the iron boat texture instead of oak.
     * The parent class computes texture from ModelLayerLocation but we need custom texture.
     */
    override fun renderType(): RenderType {
        return model().renderType(IRON_BOAT_TEXTURE)
    }

    /**
     * Override to render the fluid mask patch when floating on lava.
     * The vanilla implementation only renders this for water, but iron boats
     * need it for lava too to prevent lava rendering inside the hull.
     */
    override fun submitTypeAdditions(
        state: BoatRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int
    ) {
        // Render the fluid patch if not submerged (same condition as vanilla)
        // This masks both water AND lava from appearing inside the boat
        if (!state.isUnderWater) {
            collector.submitModel(
                fluidPatchModel,
                Unit.INSTANCE,
                poseStack,
                fluidPatchModel.renderType(IRON_BOAT_TEXTURE),
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
