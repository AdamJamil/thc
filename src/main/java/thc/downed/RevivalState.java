package thc.downed;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import thc.THCAttachments;

/**
 * State accessor for revival progress.
 * Progress stored on the DOWNED player, not the reviver.
 */
public final class RevivalState {
	private RevivalState() {
	}

	private static AttachmentTarget target(ServerPlayer player) {
		return (AttachmentTarget) player;
	}

	public static double getProgress(ServerPlayer player) {
		Double value = target(player).getAttachedOrCreate(THCAttachments.REVIVAL_PROGRESS);
		return value == null ? 0.0D : value;
	}

	public static void setProgress(ServerPlayer player, double value) {
		target(player).setAttached(THCAttachments.REVIVAL_PROGRESS, value);
	}

	public static void addProgress(ServerPlayer player, double amount) {
		setProgress(player, Math.min(1.0, getProgress(player) + amount));
	}

	public static void clearProgress(ServerPlayer player) {
		setProgress(player, 0.0D);
	}
}
