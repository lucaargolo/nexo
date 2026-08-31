package dev.lucaargolo.nexo.feature.packet;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.packet.Packet;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.Bijection;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftPacket {

    private static final Map<Location, Packet<?, ?>> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, Holder<Packet<?, ?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static final ResourceKey<Registry<Packet<?, ?>>> REGISTRY = ResourceKey.createRegistryKey(
            NexoMinecraft.rl(Location.of(NexoMinecraft.MOD_ID, "packet"))
    );

    public static final Bijection<Packet<?, ?>, Holder<Packet<?, ?>>> CONVERT = new Bijection<>() {
        @Override
        public Holder<Packet<?, ?>> forward(Packet<?, ?> packet) {
            return HOLDER_MAP.get(packet.location());
        }

        @Override
        public Packet<?, ?> backward(Holder<Packet<?, ?>> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private MinecraftPacket() {
    }

    public static @Nullable Packet<?, ?> lookup(@NotNull Location location) {
        return FEATURE_MAP.get(location);
    }

    public static @NotNull Packet<?, ?> register(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Packet<?, ?> packet) {
        Packet<?, ?> registered = FEATURE_MAP.putIfAbsent(packet.location(), packet);
        if (registered != null) {
            return registered;
        }
        Holder<Packet<?, ?>> holder = nexo.getRegistryHandler().registerBuiltinFeature(REGISTRY, NexoMinecraft.rl(packet.location()), () -> packet);
        HOLDER_MAP.put(packet.location(), holder);
        return packet;
    }

    public static @NotNull Packet<?, ?> index(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<Packet<?, ?>> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, key -> holder.value());
    }

    public static @NotNull Packet<?, ?> craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Packet<?, ?> packet) {
        return packet;
    }

}
