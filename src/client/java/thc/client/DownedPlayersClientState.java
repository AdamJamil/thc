package thc.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import thc.network.DownedPlayersPayload;

/**
 * Client-side cache of all downed players for body rendering.
 */
public final class DownedPlayersClientState {
    private static final Map<UUID, DownedPlayerInfo> downedPlayers = new HashMap<>();

    private DownedPlayersClientState() {}

    public record DownedPlayerInfo(double x, double y, double z, String name) {}

    public static void update(List<DownedPlayersPayload.DownedPlayerEntry> entries) {
        downedPlayers.clear();
        for (DownedPlayersPayload.DownedPlayerEntry entry : entries) {
            downedPlayers.put(entry.uuid(), new DownedPlayerInfo(
                entry.x(), entry.y(), entry.z(), entry.name()
            ));
        }
    }

    public static Map<UUID, DownedPlayerInfo> getDownedPlayers() {
        return Collections.unmodifiableMap(downedPlayers);
    }

    public static void clear() {
        downedPlayers.clear();
    }
}
