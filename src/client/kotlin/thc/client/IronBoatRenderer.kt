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

/**
 * Renderer for IronBoat that extends vanilla BoatRenderer.
 * Uses the oak boat model geometry but with a custom iron texture.
 *
 * Lava masking is handled by LiquidBlockRendererMixin which skips rendering
 * lava blocks inside iron boat bounding boxes. Water masking uses the same
 * approach as vanilla boats (waterMask render type).
 */
class IronBoatRenderer(context: EntityRendererProvider.Context) :
    BoatRenderer(context, ModelLayers.OAK_BOAT) {

    companion object {
        // Custom texture for iron boat
        val IRON_BOAT_TEXTURE: Identifier = Identifier.fromNamespaceAndPath("thc", "textures/entity/boat/iron.png")
    }

    // Water patch model - same as vanilla, for water masking
    // We need our own because parent's waterPatchModel is private
    private val waterPatchModel: Model.Simple = Model.Simple(
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
     * Override to render water mask patch when floating (same as vanilla boats).
     * Lava is handled differently via LiquidBlockRendererMixin.
     */
    override fun submitTypeAdditions(
        state: BoatRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int
    ) {
        // Only render water mask if not fully submerged (same as vanilla)
        if (!state.isUnderWater) {
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
}
