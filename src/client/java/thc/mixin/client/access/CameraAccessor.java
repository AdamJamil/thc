package thc.mixin.client.access;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose Camera's private fields.
 * Used for calculating render positions relative to camera and billboard rotations.
 */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    Vec3 getPosition();

    @Accessor("yRot")
    float getYRot();

    @Accessor("xRot")
    float getXRot();
}
