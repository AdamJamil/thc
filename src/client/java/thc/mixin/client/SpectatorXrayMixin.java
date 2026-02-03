package thc.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.client.DownedPlayersClientState;

import java.util.UUID;

/**
 * Disables spectator X-ray vision for downed players.
 */
@Mixin(Camera.class)
public abstract class SpectatorXrayMixin {
    @Shadow
    private boolean detached;

    @Inject(method = "setup", at = @At("TAIL"))
    private void thc$disableDownedXray(Level level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        // Only disable X-ray if the LOCAL player is downed
        // This prevents X-ray vision while still allowing spectator flight
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            UUID localUuid = localPlayer.getUUID();
            if (DownedPlayersClientState.getDownedPlayers().containsKey(localUuid)) {
                this.detached = false;
            }
        }
    }
}
