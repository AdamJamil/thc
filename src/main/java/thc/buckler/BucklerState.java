package thc.buckler;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.world.entity.LivingEntity;
import thc.THCAttachments;

public final class BucklerState {
	private BucklerState() {
	}

	private static AttachmentTarget target(LivingEntity entity) {
		return (AttachmentTarget) entity;
	}

	public static double getPoise(LivingEntity entity) {
		Double value = target(entity).getAttachedOrCreate(THCAttachments.BUCKLER_POISE);
		return value == null ? 0.0D : value;
	}

	public static void setPoise(LivingEntity entity, double value) {
		target(entity).setAttached(THCAttachments.BUCKLER_POISE, value);
	}

	public static long getRaiseTick(LivingEntity entity) {
		Long value = target(entity).getAttachedOrCreate(THCAttachments.BUCKLER_RAISE_TICK);
		return value == null ? -1L : value;
	}

	public static void setRaiseTick(LivingEntity entity, long value) {
		target(entity).setAttached(THCAttachments.BUCKLER_RAISE_TICK, value);
	}

	public static boolean isBroken(LivingEntity entity) {
		Boolean value = target(entity).getAttachedOrCreate(THCAttachments.BUCKLER_BROKEN);
		return value != null && value;
	}

	public static void setBroken(LivingEntity entity, boolean broken) {
		target(entity).setAttached(THCAttachments.BUCKLER_BROKEN, broken);
	}

	public static long getLastFullTick(LivingEntity entity) {
		Long value = target(entity).getAttachedOrCreate(THCAttachments.BUCKLER_LAST_FULL_TICK);
		return value == null ? -1L : value;
	}

	public static void setLastFullTick(LivingEntity entity, long value) {
		target(entity).setAttached(THCAttachments.BUCKLER_LAST_FULL_TICK, value);
	}

	public static double getMaxPoise(LivingEntity entity) {
		Double value = target(entity).getAttachedOrCreate(THCAttachments.BUCKLER_MAX_POISE);
		return value == null ? 0.0D : value;
	}

	public static void setMaxPoise(LivingEntity entity, double value) {
		target(entity).setAttached(THCAttachments.BUCKLER_MAX_POISE, value);
	}
}
