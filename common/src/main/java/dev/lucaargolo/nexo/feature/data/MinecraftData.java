package dev.lucaargolo.nexo.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.util.NexoHolder;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinecraftData<D> extends DataBase<D> {

    private static final ConcurrentHashMap<Location, DataBase<?>> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, NexoHolder<DataComponentType<?>, ? extends DataComponentType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    @NotNull
    private final NexoMinecraft nexo;
    @NotNull
    private final NexoHolder<DataComponentType<?>, DataComponentType<D>> holder;

    private MinecraftData(@NotNull NexoMinecraft nexo, @NotNull NexoHolder<DataComponentType<?>, DataComponentType<D>> holder) {
        super(holder.location());
        this.nexo = nexo;
        this.holder = holder;
    }

    private MinecraftData(@NotNull NexoMinecraft nexo, @NotNull ResourceKey<DataComponentType<?>> key, @NotNull Supplier<DataComponentType<D>> supplier) {
        this(nexo, new NexoHolder<>(nexo, key, supplier));
    }

    @Override
    public @NotNull D initial() {
        throw new UnsupportedOperationException("Cannot get initial value from MinecraftData");
    }

    @Override
    public @NotNull List<@NotNull Tag> tags() {
        return this.holder.tags().map(key -> new Tag(NexoMinecraft.id(key.location()))).toList();
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull D value) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.nexo.getRegistry());
        holder.get().streamCodec().encode(buf, value);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buffer), this.nexo.getRegistry());
        return holder.get().streamCodec().decode(buf);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D value) {
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

    public static DataBase<?> lookup(NexoRegistryHandler<?> helper, Location location) {
        return FEATURE_MAP.computeIfAbsent(location, l -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
            MinecraftData<?> data = BuiltInRegistries.DATA_COMPONENT_TYPE.getHolder(id).map(h -> new MinecraftData<>(helper.nexo(), h.key(), h::value)).orElse(null);
            if(data != null) HOLDER_MAP.put(location, data.holder);
            return data;
        });
    }

    public static <T> DataBase<T> register(NexoRegistryHandler<?> helper, ResourceLocation id, DataBase<T> data) {
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

        FEATURE_MAP.put(data.location(), data);
        HOLDER_MAP.put(data.location(), holder);
        helper.registerDataAttachment(data);
        return data;
    }

    public static DataComponentType<?> craft(DataBase<?> data) {
        return Objects.requireNonNull(HOLDER_MAP.get(data.location()).get());
    }


}
