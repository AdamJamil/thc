package thc.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thc.network.DownedPlayersPayload;

/**
 * Client-side cache of all downed players for body rendering.
 */
public final class DownedPlayersClientState {
    private static final Logger LOGGER = LoggerFactory.getLogger("thc.DownedPlayersClientState");
    private static final Map<UUID, DownedPlayerInfo> downedPlayers = new HashMap<>();

    private DownedPlayersClientState() {}

    public record DownedPlayerInfo(double x, double y, double z, float yaw, String name) {}

    public static void update(List<DownedPlayersPayload.DownedPlayerEntry> entries) {
        LOGGER.info("[CLIENT STATE] Received update with {} entries", entries.size());
        downedPlayers.clear();
        for (DownedPlayersPayload.DownedPlayerEntry entry : entries) {
            LOGGER.info("[CLIENT STATE] Adding downed player: {} at ({}, {}, {}) yaw={}",
                entry.name(), entry.x(), entry.y(), entry.z(), entry.yaw());
            downedPlayers.put(entry.uuid(), new DownedPlayerInfo(
                entry.x(), entry.y(), entry.z(), entry.yaw(), entry.name()
            ));
        }
        LOGGER.info("[CLIENT STATE] Now tracking {} downed players", downedPlayers.size());
    }

    public static Map<UUID, DownedPlayerInfo> getDownedPlayers() {
        return Collections.unmodifiableMap(downedPlayers);
    }

    public static void clear() {
        downedPlayers.clear();
    }
}
