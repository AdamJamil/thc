package thc.client

import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.BoatRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.BoatRenderState
import net.minecraft.resources.Identifier
import thc.entity.IronBoat

/**
 * Renderer for IronBoat that extends vanilla BoatRenderer.
 * Uses the oak boat model layer - texture override handled via mixin.
 */
class IronBoatRenderer(context: EntityRendererProvider.Context) :
    BoatRenderer(context, ModelLayers.OAK_BOAT) {

    companion object {
        // Custom texture for iron boat - accessed by mixin
        val IRON_BOAT_TEXTURE: Identifier = Identifier.fromNamespaceAndPath("thc", "textures/entity/boat/iron.png")

        // Flag to track if we're rendering an iron boat - set by mixin
        @JvmField
        @Volatile
        var renderingIronBoat = false
    }

    override fun extractRenderState(
        entity: net.minecraft.world.entity.vehicle.boat.AbstractBoat,
        state: BoatRenderState,
        partialTick: Float
    ) {
        // Set flag before calling super so mixin knows this is iron boat
        renderingIronBoat = entity is IronBoat
        super.extractRenderState(entity, state, partialTick)
    }
}
