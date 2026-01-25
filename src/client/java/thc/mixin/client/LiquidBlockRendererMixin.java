package thc.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.entity.IronBoat;

import java.util.List;

/**
 * Mixin to prevent lava from rendering inside iron boats.
 *
 * Unlike water (which uses a mask because it renders AFTER entities),
 * lava renders in the opaque pass BEFORE entities. Therefore, we must
 * skip rendering lava blocks whose centers fall within an iron boat's
 * bounding box.
 */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void thc$skipLavaInsideIronBoat(
            BlockAndTintGetter blockAndTintGetter,
            BlockPos blockPos,
            BlockState blockState,
            FluidState fluidState,
            CallbackInfo ci) {

        // Only check for lava
        if (!fluidState.is(FluidTags.LAVA)) {
            return;
        }

        // Need ClientLevel to query entities
        if (!(blockAndTintGetter instanceof ClientLevel clientLevel)) {
            return;
        }

        // Search for iron boats near this block
        AABB searchBox = new AABB(blockPos).inflate(2.0);
        List<IronBoat> boats = clientLevel.getEntities(
            EntityTypeTest.forClass(IronBoat.class),
            searchBox,
            boat -> true
        );

        // Check if block center is inside any iron boat
        double centerX = blockPos.getX() + 0.5;
        double centerY = blockPos.getY() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;

        for (IronBoat boat : boats) {
            if (boat.getBoundingBox().contains(centerX, centerY, centerZ)) {
                ci.cancel();  // Skip rendering this lava
                return;
            }
        }
    }
}
