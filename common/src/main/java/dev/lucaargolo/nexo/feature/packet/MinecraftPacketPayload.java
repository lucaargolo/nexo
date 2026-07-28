package dev.lucaargolo.nexo.feature.packet;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public record MinecraftPacketPayload(@NotNull Packet<?, ?> packet, byte @NotNull [] data) implements CustomPacketPayload {

    private static final int MAX_DATA_SIZE = 1 << 20;

    public static final Type<MinecraftPacketPayload> TYPE = new Type<>(NexoMinecraft.rl(Location.of(NexoMinecraft.MOD_ID, "packet")));
    public static final StreamCodec<RegistryFriendlyByteBuf, MinecraftPacketPayload> CODEC = CustomPacketPayload.codec(
            MinecraftPacketPayload::encode,
            MinecraftPacketPayload::decode
    );

    public MinecraftPacketPayload(@NotNull Packet<?, ?> packet) {
        this(packet, encode(packet));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(@NotNull RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(NexoMinecraft.rl(packet.location()));
        if (data.length > MAX_DATA_SIZE) {
            throw new IllegalArgumentException("Packet data exceeds " + MAX_DATA_SIZE + " bytes: " + packet.location());
        }
        buffer.writeVarInt(data.length);
        buffer.writeBytes(data);
    }

    private static @NotNull MinecraftPacketPayload decode(@NotNull RegistryFriendlyByteBuf buffer) {
        Location location = NexoMinecraft.id(buffer.readResourceLocation());
        Packet<?, ?> packet = MinecraftPacket.lookup(location);
        if (packet == null) {
            throw new IllegalArgumentException("Received unregistered packet: " + location);
        }
        int length = buffer.readVarInt();
        if (length < 0 || length > MAX_DATA_SIZE || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid data length " + length + " for packet " + location);
        }
        byte[] data = new byte[length];
        buffer.readBytes(data);
        return new MinecraftPacketPayload(packet, data);
    }

    private static byte @NotNull [] encode(@NotNull Packet<?, ?> packet) {
        ByteBuffer encoded = packet.encode();
        byte[] data = new byte[encoded.remaining()];
        encoded.get(data);
        return data;
    }

}
