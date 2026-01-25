package thc.client

import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.resources.Identifier
import thc.entity.IronBoat

/**
 * Basic renderer for IronBoat.
 * Registers the entity for rendering - actual model/texture handled by resource files.
 */
class IronBoatRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<IronBoat, EntityRenderState>(context) {

    override fun createRenderState(): EntityRenderState {
        return EntityRenderState()
    }
}
