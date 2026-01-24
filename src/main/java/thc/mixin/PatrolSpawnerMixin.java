package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.stage.StageManager;

/**
 * Gate illager patrol spawning to stage 2+.
 *
 * Part of THC stage progression - illager patrols don't appear until server
 * reaches stage 2. This gives players time to establish defenses before
 * facing organized illager groups.
 *
 * Server starts at stage 1, so patrols are blocked initially.
 * When stage advances to 2 (via evoker kill), patrols resume.
 */
@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin {

    /**
     * Block patrol spawns if server stage is below 2.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$gatePatrolsToStage2(
            ServerLevel level,
            boolean spawnEnemies,
            CallbackInfo ci) {

        int currentStage = StageManager.getCurrentStage(level.getServer());
        if (currentStage < 2) {
            ci.cancel();
        }
        // If stage >= 2, allow vanilla patrol spawn logic to proceed
    }
}
