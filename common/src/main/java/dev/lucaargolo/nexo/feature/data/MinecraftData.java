package dev.lucaargolo.nexo.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.feature.MinecraftFeature;
import dev.lucaargolo.nexo.util.NexoHolder;
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

public class MinecraftData<D> extends DataBase<D> implements MinecraftFeature<DataBase<D>, DataComponentType<?>> {

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<DataComponentType<?>, DataComponentType<D>> holder;
    @Nullable
    private final DataBase<D> delegate;

    public MinecraftData(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<DataComponentType<?>, DataComponentType<D>> holder, @Nullable DataBase<D> delegate) {
        super(holder.location());
        this.nexo = nexo;
        this.delegate = delegate;
        this.holder = holder;
    }

    public MinecraftData(@NotNull NexoMinecraft nexo, @NotNull Holder<DataComponentType<?>> holder) {
        this(nexo, new NexoHolder<>(nexo, holder, DataComponentType.class), null);
    }

    @Override
    public @NotNull NexoMinecraft nexo() {
        return this.nexo;
    }

    @Override
    public @NotNull NexoHolder<DataComponentType<?>, DataComponentType<D>> holder() {
        return this.holder;
    }

    @Override
    public @Nullable DataBase<D> delegate() {
        return this.delegate;
    }

    @Override
    public @NotNull D initial() {
        if (this.delegate != null) {
            return this.delegate.initial();
        }
        throw new UnsupportedOperationException("Cannot get initial value from MinecraftData without a delegate");
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
        holder.get().streamCodec().encode(buf, value);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        if (delegate != null) {
            return delegate.read(buffer);
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buffer), this.nexo.getRegistry());
        return holder.get().streamCodec().decode(buf);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D value) {
        if (delegate != null) {
            return delegate.serialize(value);
        }
        Codec<D> codec = holder.get().codec();
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
        Codec<D> codec = holder.get().codec();
        if (codec != null) {
            return JsonOps.INSTANCE.withDecoder(codec).apply(element).getOrThrow().getFirst();
        } else {
            JsonObject json = element.getAsJsonObject();
            String encoded = json.getAsJsonPrimitive("data").getAsString();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return this.read(ByteBuffer.wrap(decoded));
        }
    }

    public static <T> MinecraftData<T> register(NexoRegistryHandler<?> helper, ResourceLocation id, DataBase<T> data) {
        NexoHolder<DataComponentType<?>, DataComponentType<T>> holder = helper.registerBuiltinFeature(BuiltInRegistries.DATA_COMPONENT_TYPE, id, () -> {
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

}
