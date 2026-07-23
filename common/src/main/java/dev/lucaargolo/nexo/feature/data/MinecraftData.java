package dev.lucaargolo.nexo.feature.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.feature.MinecraftFeatureType;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Bijection;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftData<D> extends DataBase<D> {

    private static final ConcurrentHashMap<Location, DataBase<?>> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Location, Holder<DataComponentType<?>>> HOLDER_MAP = new ConcurrentHashMap<>();

    public static Bijection<DataBase<?>, Holder<DataComponentType<?>>> CONVERT = new Bijection<>() {
        @Override
        public Holder<DataComponentType<?>> forward(DataBase<?> feature) {
            return HOLDER_MAP.get(feature.location());
        }

        @Override
        public DataBase<?> backward(Holder<DataComponentType<?>> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private final @NotNull NexoRegistryHandler<?> helper;
    private final @NotNull Holder<?> holder;

    private MinecraftData(@NotNull NexoRegistryHandler<?> helper, @NotNull Holder<?> holder) {
        super(NexoMinecraft.id(holder), MinecraftRoleType.uncraft(helper, Type.DATA, holder));
        this.helper = helper;
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
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.helper.getRegistry());
        componentType().streamCodec().encode(buf, value);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(buffer), this.helper.getRegistry());
        return componentType().streamCodec().decode(buf);
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D value) {
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

    private @NotNull DataComponentType<D> componentType() {
        Class<DataComponentType<D>> clazz = Nexo.type(DataComponentType.class);
        return clazz.cast(this.holder.value());
    }

    public static DataBase<?> lookup(Location location) {
        return FEATURE_MAP.get(location);
    }

    public static DataBase<?> register(NexoRegistryHandler<?> helper, DataBase<?> data) {
        DataBase<?> registered = FEATURE_MAP.get(data.location());
        if (registered != null) {
            return registered;
        }
        ResourceLocation id = NexoMinecraft.rl(data.location());
        FEATURE_MAP.put(data.location(), data);
        helper.registerBuiltinFeature(BuiltInRegistries.DATA_COMPONENT_TYPE, id, MinecraftFeatureType.DATA.craft(helper, data));
        helper.registerDataAttachment(data);
        return data;
    }

    public static DataBase<?> index(NexoRegistryHandler<?> helper, Holder<DataComponentType<?>> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, l -> new MinecraftData<>(helper, holder));
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
