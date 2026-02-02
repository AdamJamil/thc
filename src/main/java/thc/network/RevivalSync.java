package thc.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import thc.downed.DownedState;
import thc.downed.RevivalState;

/**
 * Syncs revival state to clients for HUD rendering.
 * Only sends updates when state changes (delta sync).
 */
public final class RevivalSync {
    private static final Map<UUID, RevivalSyncState> LAST_SENT = new HashMap<>();

    private RevivalSync() {}

    /**
     * Sync revival state for a potential reviver.
     * Finds the closest downed player within 2 blocks that the player is looking at.
     */
    public static void sync(ServerPlayer player, Iterable<ServerPlayer> allPlayers) {
        // Don't sync to downed players
        if (DownedState.isDowned(player)) {
            sendIfChanged(player, RevivalSyncState.CLEAR);
            return;
        }

        // Find closest downed player within 2 blocks that player is looking at
        RevivalSyncState target = findLookTarget(player, allPlayers);
        sendIfChanged(player, target);
    }

    public static void clear(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
    }

    private static void sendIfChanged(ServerPlayer player, RevivalSyncState current) {
        RevivalSyncState previous = LAST_SENT.get(player.getUUID());
        if (current.equals(previous)) {
            return;
        }
        LAST_SENT.put(player.getUUID(), current);

        if (current == RevivalSyncState.CLEAR) {
            ServerPlayNetworking.send(player, RevivalStatePayload.CLEAR);
        } else {
            ServerPlayNetworking.send(player, new RevivalStatePayload(
                current.downedUUID(),
                current.downedX(),
                current.downedY(),
                current.downedZ(),
                current.progress()
            ));
        }
    }

    private static RevivalSyncState findLookTarget(ServerPlayer player, Iterable<ServerPlayer> allPlayers) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();

        RevivalSyncState closest = RevivalSyncState.CLEAR;
        double closestDistSq = Double.MAX_VALUE;

        for (ServerPlayer other : allPlayers) {
            if (!DownedState.isDowned(other)) continue;

            Vec3 downedLoc = DownedState.getDownedLocation(other);
            if (downedLoc == null) continue;

            // Check if within 2 blocks (squared distance = 4.0)
            double distSq = eyePos.distanceToSqr(downedLoc);
            if (distSq > 4.0) continue;

            // Check if player is roughly looking at the downed location
            // Use a generous cone (60 degree half-angle = cos(60) = 0.5)
            Vec3 toTarget = downedLoc.subtract(eyePos).normalize();
            double dot = lookVec.dot(toTarget);
            if (dot < 0.5) continue;  // Not looking toward target

            // Track closest valid target
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                double progress = RevivalState.getProgress(other);
                closest = new RevivalSyncState(
                    other.getUUID(),
                    downedLoc.x,
                    downedLoc.y,
                    downedLoc.z,
                    progress
                );
            }
        }

        return closest;
    }

    private record RevivalSyncState(UUID downedUUID, double downedX, double downedY, double downedZ, double progress) {
        static final RevivalSyncState CLEAR = new RevivalSyncState(new UUID(0, 0), 0.0, 0.0, 0.0, 0.0);
    }
}
