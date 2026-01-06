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

	private THCAttachments() {
	}

	public static void init() {
		// Ensures static initialization has run.
	}
}
