package thc.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BucklerStatePayload(double poise, double maxPoise, boolean broken, long lastFullTick)
	implements CustomPacketPayload {
	public static final Type<BucklerStatePayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thc", "buckler_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BucklerStatePayload> STREAM_CODEC =
		StreamCodec.ofMember(BucklerStatePayload::write, BucklerStatePayload::new);

	public BucklerStatePayload(RegistryFriendlyByteBuf buf) {
		this(buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readLong());
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeDouble(this.poise);
		buf.writeDouble(this.maxPoise);
		buf.writeBoolean(this.broken);
		buf.writeLong(this.lastFullTick);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
