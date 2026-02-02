package thc;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
	public static final AttachmentType<Map<UUID, Double>> MOB_THREAT = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "mob_threat"),
		builder -> builder.initializer(HashMap::new)
	);
	public static final AttachmentType<Long> THREAT_LAST_DECAY = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "threat_last_decay"),
		builder -> builder.initializer(() -> 0L)
	);
	public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "player_class"),
		builder -> {
			builder.initializer(() -> null);
			builder.persistent(Codec.STRING);
			builder.copyOnDeath();
		}
	);
	public static final AttachmentType<Integer> BOON_LEVEL = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "boon_level"),
		builder -> {
			builder.initializer(() -> 0);
			builder.persistent(Codec.INT);
			builder.copyOnDeath();
		}
	);
	public static final AttachmentType<String> SPAWN_REGION = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "spawn_region"),
		builder -> {
			builder.initializer(() -> null);
			builder.persistent(Codec.STRING);
		}
	);
	public static final AttachmentType<Boolean> SPAWN_COUNTED = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "spawn_counted"),
		builder -> {
			builder.initializer(() -> Boolean.FALSE);
			builder.persistent(Codec.BOOL);
		}
	);
	/**
	 * Tracks the source of fire damage for custom damage rates.
	 * Values: "flame" (Flame enchantment), "fire_aspect" (Fire Aspect enchantment), or null (normal fire)
	 * Non-persistent - resets on entity reload/respawn.
	 */
	public static final AttachmentType<String> FIRE_SOURCE = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "fire_source"),
		builder -> builder.initializer(() -> null)
	);
	/**
	 * Tracks the location where a player was downed (for tether enforcement and revival).
	 * Non-persistent - downed state should not survive server restart.
	 * Null when player is not downed.
	 */
	public static final AttachmentType<Vec3> DOWNED_LOCATION = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("thc", "downed_location"),
		builder -> builder.initializer(() -> null)
	);

	private THCAttachments() {
	}

	public static void init() {
		// Ensures static initialization has run.
	}
}
