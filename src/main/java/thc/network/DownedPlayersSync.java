package thc.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import thc.downed.DownedState;

/**
 * Broadcasts downed player locations to all clients for body rendering.
 * Sends list of all downed players within 50 blocks to each client.
 * Uses delta sync - only sends when list changes.
 */
public final class DownedPlayersSync {
    private static final double SYNC_RANGE_SQ = 50.0 * 50.0;  // 50 blocks squared
    private static final Map<UUID, List<DownedPlayersPayload.DownedPlayerEntry>> LAST_SENT = new HashMap<>();

    private DownedPlayersSync() {}

    /**
     * Sync downed player locations to a client.
     */
    public static void sync(ServerPlayer player, Iterable<ServerPlayer> allPlayers) {
        List<DownedPlayersPayload.DownedPlayerEntry> entries = buildEntryList(player, allPlayers);
        sendIfChanged(player, entries);
    }

    public static void clear(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
    }

    private static List<DownedPlayersPayload.DownedPlayerEntry> buildEntryList(
            ServerPlayer viewer, Iterable<ServerPlayer> allPlayers) {
        Vec3 viewerPos = viewer.position();
        List<DownedPlayersPayload.DownedPlayerEntry> entries = new ArrayList<>();

        for (ServerPlayer other : allPlayers) {
            if (!DownedState.isDowned(other)) continue;
            if (other.getUUID().equals(viewer.getUUID())) continue;  // Don't include self

            Vec3 downedLoc = DownedState.getDownedLocation(other);
            if (downedLoc == null) continue;

            // Check if within sync range
            double distSq = viewerPos.distanceToSqr(downedLoc);
            if (distSq > SYNC_RANGE_SQ) continue;

            entries.add(new DownedPlayersPayload.DownedPlayerEntry(
                other.getUUID(),
                downedLoc.x,
                downedLoc.y,
                downedLoc.z,
                other.getName().getString()
            ));
        }

        return entries;
    }

    private static void sendIfChanged(ServerPlayer player, List<DownedPlayersPayload.DownedPlayerEntry> current) {
        List<DownedPlayersPayload.DownedPlayerEntry> previous = LAST_SENT.get(player.getUUID());
        if (entriesEqual(current, previous)) {
            return;
        }
        LAST_SENT.put(player.getUUID(), new ArrayList<>(current));

        if (current.isEmpty()) {
            ServerPlayNetworking.send(player, DownedPlayersPayload.EMPTY);
        } else {
            ServerPlayNetworking.send(player, new DownedPlayersPayload(current));
        }
    }

    private static boolean entriesEqual(
            List<DownedPlayersPayload.DownedPlayerEntry> a,
            List<DownedPlayersPayload.DownedPlayerEntry> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            DownedPlayersPayload.DownedPlayerEntry ea = a.get(i);
            DownedPlayersPayload.DownedPlayerEntry eb = b.get(i);
            if (!ea.uuid().equals(eb.uuid())) return false;
            // Location comparison with small epsilon for floating point
            if (Math.abs(ea.x() - eb.x()) > 0.01) return false;
            if (Math.abs(ea.y() - eb.y()) > 0.01) return false;
            if (Math.abs(ea.z() - eb.z()) > 0.01) return false;
        }
        return true;
    }
}
