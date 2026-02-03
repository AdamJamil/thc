package thc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Broadcasts all downed player locations to clients for body rendering.
 * Sent to all players each tick with list of nearby downed players.
 */
public record DownedPlayersPayload(List<DownedPlayerEntry> entries) implements CustomPacketPayload {
    public static final Type<DownedPlayersPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thc", "downed_players"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DownedPlayersPayload> STREAM_CODEC =
        StreamCodec.ofMember(DownedPlayersPayload::write, DownedPlayersPayload::read);

    public static final DownedPlayersPayload EMPTY = new DownedPlayersPayload(List.of());

    public record DownedPlayerEntry(UUID uuid, double x, double y, double z, String name) {
        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeUUID(uuid);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeUtf(name);
        }

        public static DownedPlayerEntry read(RegistryFriendlyByteBuf buf) {
            return new DownedPlayerEntry(
                buf.readUUID(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readUtf()
            );
        }
    }

    private static DownedPlayersPayload read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<DownedPlayerEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(DownedPlayerEntry.read(buf));
        }
        return new DownedPlayersPayload(entries);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (DownedPlayerEntry entry : entries) {
            entry.write(buf);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
