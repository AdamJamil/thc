package thc.mixin.client;

import net.minecraft.client.renderer.entity.BoatRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder mixin for IronBoat rendering.
 * The IronBoatRenderer extends BoatRenderer and uses the oak boat model.
 * In a future update, this could be used to customize texture handling.
 *
 * Note: Iron boat will render with oak boat texture until MC 1.21 rendering
 * API changes are better understood.
 */
@Mixin(BoatRenderer.class)
public abstract class IronBoatRendererMixin {
    // Currently no injections needed - IronBoatRenderer handles via inheritance
    // The boat will be visible but may use oak boat texture
}
