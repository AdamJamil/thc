package thc.mixin.client;

import net.minecraft.client.renderer.entity.BoatRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin placeholder for potential future BoatRenderer customizations.
 *
 * Current iron boat rendering is handled via IronBoatRenderer which extends
 * BoatRenderer and overrides:
 * - renderType() for custom texture
 * - submitTypeAdditions() for lava fluid masking
 *
 * This mixin is kept for potential future needs but has no active injections.
 */
@Mixin(BoatRenderer.class)
public abstract class IronBoatRendererMixin {
    // No injections currently needed - IronBoatRenderer handles via inheritance
}
