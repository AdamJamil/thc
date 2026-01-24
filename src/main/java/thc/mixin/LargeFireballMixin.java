package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Expand fire spread radius for Ghast fireball explosions.
 *
 * <p>FR-09: Ghast fireball ground impacts spread fire noticeably further
 * (6 block radius vs vanilla 3).
 *
 * <p>Note: This applies to ALL LargeFireball impacts including deflected fireballs,
 * which is intentional per CONTEXT.md.
 */
@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin {

    /**
     * Place additional fire blocks after vanilla explosion completes.
     *
     * <p>Vanilla fire spread covers ~3 block radius. THC adds fire in outer
     * ring from 3-6 blocks from impact center. 33% placement chance matches
     * vanilla probability.
     */
    @Inject(method = "onHit", at = @At("TAIL"))
    private void thc$expandFireSpread(HitResult hitResult, CallbackInfo ci) {
        LargeFireball self = (LargeFireball) (Object) this;

        if (!(self.level() instanceof ServerLevel level)) return;

        // Get impact position
        BlockPos impactPos = BlockPos.containing(hitResult.getLocation());

        // Place fire in expanded radius ring (3-6 blocks from center)
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    // Calculate distance from center
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    // Skip inner vanilla radius and beyond outer radius
                    if (dist < 3.0 || dist > 6.0) continue;

                    BlockPos firePos = impactPos.offset(dx, dy, dz);

                    // Only place fire on air blocks above solid blocks
                    if (level.getBlockState(firePos).isAir()) {
                        BlockPos below = firePos.below();
                        if (level.getBlockState(below).isSolid()) {
                            // 33% chance matches vanilla fire spread probability
                            if (level.random.nextFloat() < 0.33f) {
                                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
    }
}
