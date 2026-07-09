package dev.lucaargolo.nexo.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.data.NexoData;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
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

public class MinecraftData<D> extends NexoData<D> implements MinecraftFeature<NexoData<D>, DataComponentType<?>> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final Location location;
    @NotNull
    private final Holder<DataComponentType<?>> holder;
    @Nullable
    private final NexoData<D> delegate;

    public MinecraftData(@NotNull NexoMinecraft nexo, @NotNull Holder<DataComponentType<?>> holder, @Nullable NexoData<D> delegate) {
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
        this.location = NexoMinecraft.id(holder.unwrapKey().orElseThrow().location());
    }

    public MinecraftData(@NotNull NexoMinecraft nexo, @NotNull Holder<DataComponentType<?>> holder) {
        this(nexo, holder, null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull Holder<DataComponentType<?>> holder() {
        return this.holder;
    }

    @Override
    public @Nullable NexoData<D> delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull Location location() {
        return this.location;
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull D value) {
        if (delegate != null) {
            return delegate.write(value);
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.nexo.getRegistry());
        componentType().streamCodec().encode(buf, value);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        if (delegate != null) {
            return delegate.read(buffer);
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buffer), this.nexo.getRegistry());
        return componentType().streamCodec().decode(buf);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D value) {
        if (delegate != null) {
            return delegate.serialize(value);
        }
        Codec<D> codec = componentType().codec();
        if (codec != null) {
            return JsonOps.INSTANCE.withEncoder(codec).apply(value).getOrThrow();
        } else {
            JsonObject json = new JsonObject();
            ByteBuffer encoded = Base64.getEncoder().encode(this.write(value));
            json.addProperty("data", StandardCharsets.UTF_8.decode(encoded).toString());
            return json;
        }
    }

    @Override
    public @NotNull D deserialize(@NotNull JsonElement element) {
        if (delegate != null) {
            return delegate.deserialize(element);
        }
        Codec<D> codec = componentType().codec();
        if (codec != null) {
            return JsonOps.INSTANCE.withDecoder(codec).apply(element).getOrThrow().getFirst();
        } else {
            JsonObject json = element.getAsJsonObject();
            String encoded = json.getAsJsonPrimitive("data").getAsString();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return this.read(ByteBuffer.wrap(decoded));
        }
    }

    public static <T> MinecraftData<T> register(NexoRegistryHandler<?> helper, ResourceLocation id, NexoData<T> data) {
        Holder<DataComponentType<?>> holder = helper.registerBuiltinFeature(BuiltInRegistries.DATA_COMPONENT_TYPE, id, () -> {
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
        return new MinecraftData<>(helper.nexo(), holder, data);
    }

    @SuppressWarnings("unchecked")
    private DataComponentType<D> componentType() {
        return (DataComponentType<D>) holder.value();
    }

}
