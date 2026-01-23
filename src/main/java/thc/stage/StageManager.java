package thc.stage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import thc.THCAttachments;

/**
 * Static utility for stage and boon level CRUD operations.
 * Stage is server-wide (stored in StageData SavedData).
 * Boon level is per-player (stored in BOON_LEVEL attachment).
 */
public final class StageManager {
	private StageManager() {
	}

	/**
	 * Get the current server-wide stage (1-5).
	 */
	public static int getCurrentStage(MinecraftServer server) {
		return StageData.getServerState(server).getCurrentStage();
	}

	/**
	 * Advance the server to the next stage.
	 * Increments all online players' boon levels and broadcasts announcement.
	 * @return true if stage advanced, false if already at max (5)
	 */
	public static boolean advanceStage(MinecraftServer server) {
		StageData state = StageData.getServerState(server);
		if (!state.advanceStage()) return false;

		// Increment all online players' boon levels
		int newStage = state.getCurrentStage();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			incrementBoonLevel(player);
		}

		// Broadcast to all players via actionbar
		Component message = Component.literal("Trial complete. The world has advanced to Stage " + newStage + ".")
			.withStyle(ChatFormatting.RED);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.displayClientMessage(message, true); // true = actionbar
		}

		return true;
	}

	/**
	 * Get a player's current boon level.
	 */
	public static int getBoonLevel(ServerPlayer player) {
		Integer level = player.getAttached(THCAttachments.BOON_LEVEL);
		return level != null ? level : 0;
	}

	/**
	 * Increment a player's boon level by 1.
	 */
	public static void incrementBoonLevel(ServerPlayer player) {
		int current = getBoonLevel(player);
		player.setAttached(THCAttachments.BOON_LEVEL, current + 1);
	}

	/**
	 * Set a player's boon level to a specific value.
	 * Used for late-joiner initialization.
	 */
	public static void setBoonLevel(ServerPlayer player, int level) {
		player.setAttached(THCAttachments.BOON_LEVEL, level);
	}
}
