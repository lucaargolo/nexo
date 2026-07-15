package dev.lucaargolo.nexo.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.NexoHolder;
import dev.lucaargolo.nexo.util.NexoUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
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
    private static final ConcurrentHashMap<Location, NexoHolder<DataComponentType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<DataBase<?>, NexoHolder<DataComponentType<?>>> CONVERT = new Bijection<>() {
        @Override
        public NexoHolder<DataComponentType<?>> forward(DataBase<?> feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public DataBase<?> backward(NexoHolder<DataComponentType<?>> holder) {
            return FEATURE_MAP.get(holder.location());
        }
    };

    @NotNull
    private final NexoHolder<DataComponentType<D>> holder;

    private MinecraftData(@NotNull NexoHolder<DataComponentType<D>> holder) {
        super(holder.location());
        this.holder = holder;
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
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), holder.nexo().getRegistry());
        holder.get().streamCodec().encode(buf, value);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buffer), holder.nexo().getRegistry());
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

    public static DataBase<?> lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static DataBase<?> register(NexoRegistryHandler<?> helper, DataBase<?> data) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(data.location().namespace(), data.location().path());
        NexoHolder<DataComponentType<?>> holder = helper.registerBuiltinFeature(BuiltInRegistries.DATA_COMPONENT_TYPE, id, MinecraftFeatureType.DATA.craft(helper, data));
        FEATURE_MAP.put(data.location(), data);
        HOLDER_MAP.put(data.location(), holder);
        helper.registerDataAttachment(data);
        return data;
    }

    public static NexoHolder<DataComponentType<?>> index(NexoRegistryHandler<?> helper, DataComponentType<?> type) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type));
        Holder<DataComponentType<?>> h = BuiltInRegistries.DATA_COMPONENT_TYPE.getHolder(id).orElseThrow();
        NexoHolder<DataComponentType<?>> holder = new NexoHolder<>(helper.nexo(), h, NexoUtils.type(DataComponentType.class));
        FEATURE_MAP.put(holder.location(), new MinecraftData<>(holder));
        HOLDER_MAP.put(holder.location(), holder);
        return holder;
    }

    public static <T> DataComponentType<T> craft(NexoRegistryHandler<?> helper, DataBase<T> data) {
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
    }

}
