package thc.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import thc.buckler.BucklerState;

public final class BucklerSync {
	private static final Map<UUID, BucklerSyncState> LAST_SENT = new HashMap<>();

	private BucklerSync() {
	}

	public static void sync(ServerPlayer player) {
		BucklerSyncState current = BucklerSyncState.fromPlayer(player);
		BucklerSyncState previous = LAST_SENT.get(player.getUUID());
		if (current.equals(previous)) {
			return;
		}
		LAST_SENT.put(player.getUUID(), current);
		send(player, current);
	}

	public static void clear(ServerPlayer player) {
		LAST_SENT.remove(player.getUUID());
	}

	private static void send(ServerPlayer player, BucklerSyncState state) {
		ServerPlayNetworking.send(
			player,
			new BucklerStatePayload(state.poise(), state.maxPoise(), state.broken(), state.lastFullTick())
		);
	}

	private record BucklerSyncState(double poise, double maxPoise, boolean broken, long lastFullTick) {
		static BucklerSyncState fromPlayer(ServerPlayer player) {
			double poise = BucklerState.getPoise(player);
			double maxPoise = BucklerState.getMaxPoise(player);
			long lastFullTick = BucklerState.getLastFullTick(player);
			boolean broken = BucklerState.isBroken(player);
			if (!Double.isFinite(poise)) {
				poise = 0.0D;
			}
			if (!Double.isFinite(maxPoise)) {
				maxPoise = 0.0D;
			}
			return new BucklerSyncState(poise, maxPoise, broken, lastFullTick);
		}
	}
}
