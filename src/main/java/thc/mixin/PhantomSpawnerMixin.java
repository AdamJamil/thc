package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disable natural phantom spawning from insomnia.
 *
 * Part of THC difficulty design - phantoms removed entirely from natural
 * spawning. Players can still encounter phantoms via spawn eggs or commands.
 *
 * PhantomSpawner is a custom spawner (not biome-based) that triggers based
 * on player insomnia statistics. HEAD cancellation prevents all spawn attempts
 * while leaving insomnia stat unchanged.
 */
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {

    /**
     * Block all phantom natural spawn attempts.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$disablePhantomSpawns(
            ServerLevel level,
            boolean spawnEnemies,
            CallbackInfo ci) {
        ci.cancel();
    }
}
