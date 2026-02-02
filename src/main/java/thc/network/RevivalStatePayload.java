package thc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/**
 * Syncs revival state to client for HUD rendering.
 * Sent to revivers who are looking at a downed location.
 */
public record RevivalStatePayload(
    UUID downedPlayerUUID,     // UUID of downed player being revived
    double downedX,            // Downed location X
    double downedY,            // Downed location Y
    double downedZ,            // Downed location Z
    double progress            // Revival progress 0.0 to 1.0
) implements CustomPacketPayload {
    public static final Type<RevivalStatePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thc", "revival_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RevivalStatePayload> STREAM_CODEC =
        StreamCodec.ofMember(RevivalStatePayload::write, RevivalStatePayload::new);

    // "Clear" payload - no active revival target
    public static final RevivalStatePayload CLEAR = new RevivalStatePayload(
        new UUID(0, 0), 0.0, 0.0, 0.0, 0.0
    );

    public RevivalStatePayload(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.downedPlayerUUID);
        buf.writeDouble(this.downedX);
        buf.writeDouble(this.downedY);
        buf.writeDouble(this.downedZ);
        buf.writeDouble(this.progress);
    }

    public boolean isClear() {
        return downedPlayerUUID.getMostSignificantBits() == 0
            && downedPlayerUUID.getLeastSignificantBits() == 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
