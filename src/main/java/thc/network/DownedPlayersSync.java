package thc.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thc.downed.DownedState;

/**
 * Broadcasts downed player locations to all clients for body rendering.
 * Sends list of all downed players within 50 blocks to each client.
 * Uses delta sync - only sends when list changes.
 */
public final class DownedPlayersSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("thc.DownedPlayersSync");
    private static final double SYNC_RANGE_SQ = 50.0 * 50.0;  // 50 blocks squared
    private static final Map<UUID, List<DownedPlayersPayload.DownedPlayerEntry>> LAST_SENT = new HashMap<>();
    private static int debugTick = 0;

    private DownedPlayersSync() {}

    /**
     * Sync downed player locations to a client.
     */
    public static void sync(ServerPlayer player, Iterable<ServerPlayer> allPlayers) {
        debugTick++;
        boolean shouldLog = (debugTick % 100 == 0);  // Log every 5 seconds

        if (shouldLog) {
            LOGGER.info("[SYNC] Syncing to player: {}", player.getName().getString());
        }

        List<DownedPlayersPayload.DownedPlayerEntry> entries = buildEntryList(player, allPlayers, shouldLog);

        if (shouldLog) {
            LOGGER.info("[SYNC] Found {} downed players for {}", entries.size(), player.getName().getString());
        }

        sendIfChanged(player, entries, shouldLog);
    }

    public static void clear(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
    }

    private static List<DownedPlayersPayload.DownedPlayerEntry> buildEntryList(
            ServerPlayer viewer, Iterable<ServerPlayer> allPlayers, boolean shouldLog) {
        Vec3 viewerPos = viewer.position();
        List<DownedPlayersPayload.DownedPlayerEntry> entries = new ArrayList<>();

        int totalPlayers = 0;
        int downedCount = 0;

        for (ServerPlayer other : allPlayers) {
            totalPlayers++;

            boolean isDowned = DownedState.isDowned(other);
            if (shouldLog) {
                LOGGER.info("[BUILD] Checking player {} - isDowned={}", other.getName().getString(), isDowned);
            }

            if (!isDowned) continue;
            downedCount++;

            // TODO: Remove this debug - temporarily showing own body for testing
            // if (other.getUUID().equals(viewer.getUUID())) {
            //     if (shouldLog) LOGGER.info("[BUILD] Skipping self");
            //     continue;
            // }

            Vec3 downedLoc = DownedState.getDownedLocation(other);
            if (shouldLog) {
                LOGGER.info("[BUILD] Downed location for {}: {}", other.getName().getString(), downedLoc);
            }
            if (downedLoc == null) continue;

            // Check if within sync range
            double distSq = viewerPos.distanceToSqr(downedLoc);
            if (shouldLog) {
                LOGGER.info("[BUILD] Distance^2 from viewer: {} (max: {})", distSq, SYNC_RANGE_SQ);
            }
            if (distSq > SYNC_RANGE_SQ) continue;

            entries.add(new DownedPlayersPayload.DownedPlayerEntry(
                other.getUUID(),
                downedLoc.x,
                downedLoc.y,
                downedLoc.z,
                other.getYRot(),
                other.getName().getString()
            ));
            if (shouldLog) {
                LOGGER.info("[BUILD] Added entry for {} at ({}, {}, {})",
                    other.getName().getString(), downedLoc.x, downedLoc.y, downedLoc.z);
            }
        }

        if (shouldLog) {
            LOGGER.info("[BUILD] Total players: {}, downed: {}, entries: {}",
                totalPlayers, downedCount, entries.size());
        }

        return entries;
    }

    private static void sendIfChanged(ServerPlayer player, List<DownedPlayersPayload.DownedPlayerEntry> current, boolean shouldLog) {
        List<DownedPlayersPayload.DownedPlayerEntry> previous = LAST_SENT.get(player.getUUID());
        boolean areEqual = entriesEqual(current, previous);

        if (shouldLog) {
            LOGGER.info("[SEND] Previous entries: {}, current: {}, equal: {}",
                previous == null ? "null" : previous.size(), current.size(), areEqual);
        }

        if (areEqual) {
            return;
        }
        LAST_SENT.put(player.getUUID(), new ArrayList<>(current));

        if (current.isEmpty()) {
            if (shouldLog) LOGGER.info("[SEND] Sending EMPTY payload to {}", player.getName().getString());
            ServerPlayNetworking.send(player, DownedPlayersPayload.EMPTY);
        } else {
            if (shouldLog) LOGGER.info("[SEND] Sending payload with {} entries to {}",
                current.size(), player.getName().getString());
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
