package dev.lucaargolo.nexo.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.util.Location;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class MinecraftData<D> extends NexoData<D> {

    @NotNull
    private final Location location;
    @NotNull
    private final Holder<DataComponentType<?>> holder;
    @Nullable
    private final NexoData<D> delegate;

    public MinecraftData(Holder<DataComponentType<?>> holder, NexoData<D> delegate) {
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftData(Holder<DataComponentType<?>> holder) {
        this(holder, null);
    }

    public @NotNull Holder<DataComponentType<?>> getHolder() {
        return holder;
    }

    @Nullable
    public NexoData<D> getDelegate() {
        return delegate;
    }

    @Override
    public @NotNull Location location() {
        return location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull D data) {
        if (delegate != null) {
            return delegate.write(data);
        }
        DataComponentType<D> type = (DataComponentType<D>) holder.value();
        RegistryFriendlyByteBuf buf = NexoMinecraft.getHelper().befriend(Unpooled.buffer());
        type.streamCodec().encode(buf, data);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        if (delegate != null) {
            return delegate.read(buffer);
        }
        DataComponentType<D> type = (DataComponentType<D>) holder.value();
        RegistryFriendlyByteBuf buf = NexoMinecraft.getHelper().befriend(Unpooled.wrappedBuffer(buffer));
        return type.streamCodec().decode(buf);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D data) {
        if (delegate != null) {
            return delegate.serialize(data);
        }
        DataComponentType<D> type = (DataComponentType<D>) holder.value();
        Codec<D> codec = type.codec();
        if (codec != null) {
            return JsonOps.INSTANCE.withEncoder(codec).apply(data).getOrThrow();
        } else {
            JsonObject json = new JsonObject();
            ByteBuffer encoded = Base64.getEncoder().encode(this.write(data));
            json.addProperty("data", StandardCharsets.UTF_8.decode(encoded).toString());
            return json;
        }
    }

    @Override
    public @NotNull D deserialize(@NotNull JsonElement element) {
        if (delegate != null) {
            return delegate.deserialize(element);
        }
        DataComponentType<D> type = (DataComponentType<D>) holder.value();
        Codec<D> codec = type.codec();
        if (codec != null) {
            return JsonOps.INSTANCE.withDecoder(codec).apply(element).getOrThrow().getFirst();
        } else {
            JsonObject json = element.getAsJsonObject();
            String encoded = json.getAsJsonPrimitive("data").getAsString();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return this.read(ByteBuffer.wrap(decoded));
        }
    }

    public static <T> MinecraftData<T> register(ResourceLocation id, NexoData<T> data) {
        Holder<DataComponentType<?>> holder = NexoMinecraft.getHelper().registerFeature(BuiltInRegistries.DATA_COMPONENT_TYPE, id, () -> {
            DataComponentType.Builder<T> builder = DataComponentType.builder();
            if (data.persistent()) {
                Codec<T> codec = NexoMinecraft.createCodec(data);
                builder.persistent(codec);
            }
            if (data.synced()) {
                StreamCodec<RegistryFriendlyByteBuf, T> codec = NexoMinecraft.createPacketCodec(data);
                builder.networkSynchronized(codec);
            }
            return builder.build();
        });
        return new MinecraftData<>(holder, data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static MinecraftData of(Holder<DataComponentType<?>> holder) {
        return new MinecraftData(holder);
    }

}
