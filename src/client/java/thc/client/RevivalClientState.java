package thc.client;

import java.util.UUID;

/**
 * Client-side cache of revival state for HUD rendering.
 */
public final class RevivalClientState {
    private static UUID downedUUID = null;
    private static double downedX;
    private static double downedY;
    private static double downedZ;
    private static double progress;

    private RevivalClientState() {}

    public static void update(UUID uuid, double x, double y, double z, double newProgress) {
        if (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0) {
            // Clear state
            downedUUID = null;
            progress = 0.0;
        } else {
            downedUUID = uuid;
            downedX = x;
            downedY = y;
            downedZ = z;
            progress = newProgress;
        }
    }

    public static boolean hasTarget() {
        return downedUUID != null;
    }

    public static UUID getDownedUUID() {
        return downedUUID;
    }

    public static double getDownedX() {
        return downedX;
    }

    public static double getDownedY() {
        return downedY;
    }

    public static double getDownedZ() {
        return downedZ;
    }

    public static double getProgress() {
        return progress;
    }
}
