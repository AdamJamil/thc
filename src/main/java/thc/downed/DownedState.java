package thc.downed;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import thc.THCAttachments;

/**
 * State accessor for player downed status.
 * A player is "downed" when they would have died but are instead placed in spectator mode,
 * tethered to their death location, awaiting revival from a teammate.
 */
public final class DownedState {
	private DownedState() {
	}

	private static AttachmentTarget target(ServerPlayer player) {
		return (AttachmentTarget) player;
	}

	/**
	 * Gets the location where the player was downed.
	 * @return The downed location, or null if player is not downed.
	 */
	public static Vec3 getDownedLocation(ServerPlayer player) {
		return target(player).getAttached(THCAttachments.DOWNED_LOCATION);
	}

	/**
	 * Sets the downed location for the player.
	 * @param location The position where the player was downed.
	 */
	public static void setDownedLocation(ServerPlayer player, Vec3 location) {
		target(player).setAttached(THCAttachments.DOWNED_LOCATION, location);
	}

	/**
	 * Checks if the player is currently downed.
	 * A player is downed if they have a downed location AND are in spectator mode.
	 * @return true if the player is downed, false otherwise.
	 */
	public static boolean isDowned(ServerPlayer player) {
		Vec3 loc = getDownedLocation(player);
		return loc != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
	}

	/**
	 * Clears the downed state for the player.
	 * Called when the player is revived.
	 */
	public static void clearDowned(ServerPlayer player) {
		target(player).setAttached(THCAttachments.DOWNED_LOCATION, null);
	}
}
