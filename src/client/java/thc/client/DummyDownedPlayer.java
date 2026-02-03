package thc.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

/**
 * A fake player entity used for rendering downed player bodies.
 * Extends RemotePlayer so Minecraft auto-loads skin from GameProfile.
 */
public class DummyDownedPlayer extends RemotePlayer {
    public DummyDownedPlayer(ClientLevel level, GameProfile profile) {
        super(level, profile);
        setPos(0, 0, 0);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        // Disable automatic nametag - it rotates with the body and looks wrong
        // The red particles already indicate the downed player location
        return false;
    }
}
