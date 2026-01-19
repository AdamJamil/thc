package thc;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class THCAttachments {
	public static final AttachmentType<Double> MAX_HEALTH = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "max_health"),
		builder -> {
			builder.initializer(() -> 8.0D);
			builder.persistent(Codec.DOUBLE);
			builder.copyOnDeath();
		}
	);
	public static final AttachmentType<Double> BUCKLER_POISE = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "buckler_poise"),
		builder -> builder.initializer(() -> 0.0D)
	);
	public static final AttachmentType<Long> BUCKLER_RAISE_TICK = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "buckler_raise_tick"),
		builder -> builder.initializer(() -> -1L)
	);
	public static final AttachmentType<Boolean> BUCKLER_BROKEN = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "buckler_broken"),
		builder -> builder.initializer(() -> Boolean.FALSE)
	);
	public static final AttachmentType<Long> BUCKLER_LAST_FULL_TICK = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "buckler_last_full_tick"),
		builder -> builder.initializer(() -> -1L)
	);
	public static final AttachmentType<Double> BUCKLER_MAX_POISE = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "buckler_max_poise"),
		builder -> builder.initializer(() -> 0.0D)
	);
	public static final AttachmentType<Boolean> BELL_ACTIVATED = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "bell_activated"),
		builder -> {
			builder.initializer(() -> Boolean.FALSE);
			builder.persistent(Codec.BOOL);
		}
	);
	public static final AttachmentType<Boolean> WIND_CHARGE_BOOSTED = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "wind_charge_boosted"),
		builder -> builder.initializer(() -> Boolean.FALSE)
	);

	private THCAttachments() {
	}

	public static void init() {
		// Ensures static initialization has run.
	}
}
