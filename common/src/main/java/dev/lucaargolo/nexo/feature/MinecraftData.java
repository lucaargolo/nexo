package dev.lucaargolo.nexo.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.data.IData;
import io.netty.buffer.ByteBuf;
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
import java.util.function.Supplier;

public class MinecraftData<D> extends MinecraftFeature<DataComponentType<D>, IData<D>> implements IData<D> {

    public MinecraftData(Holder<DataComponentType<D>> holder, IData<D> delegate) {
        super(holder, delegate);
    }

    @Override
    public @NotNull ByteBuffer write(@NotNull D data) {
        if(this.getDelegate() != null) {
            return this.getDelegate().write(data);
        }
        DataComponentType<D> type = this.getHolder().value();
        ByteBuf buf = Unpooled.buffer();
        type.streamCodec().encode(new RegistryFriendlyByteBuf(buf, registryAccess), data);
        return buf.nioBuffer();
    }

    @Override
    public @NotNull D read(@NotNull ByteBuffer buffer) {
        if(this.getDelegate() != null) {
            return this.getDelegate().read(buffer);
        }
        DataComponentType<D> type = this.getHolder().value();
        ByteBuf buf = Unpooled.wrappedBuffer(buffer);
        return type.streamCodec().decode(new RegistryFriendlyByteBuf(buf, registryAccess));
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull D data) {
        if(this.getDelegate() != null) {
            return this.getDelegate().serialize(data);
        }
        DataComponentType<D> type = this.getHolder().value();
        Codec<D> codec = type.codec();
        if(codec != null) {
            return JsonOps.INSTANCE.withEncoder(codec).apply(data).getOrThrow();
        }else{
            JsonObject json = new JsonObject();
            ByteBuffer encoded = Base64.getEncoder().encode(this.write(data));
            json.addProperty("data", StandardCharsets.UTF_8.decode(encoded).toString());
            return json;
        }
    }

    @Override
    public @NotNull D deserialize(@NotNull JsonElement element) {
        if(this.getDelegate() != null) {
            return this.getDelegate().deserialize(element);
        }
        DataComponentType<D> type = this.getHolder().value();
        Codec<D> codec = type.codec();
        if(codec != null) {
            return JsonOps.INSTANCE.withDecoder(codec).apply(element).getOrThrow().getFirst();
        }else{
            JsonObject json = element.getAsJsonObject();
            String encoded = json.getAsJsonPrimitive("data").getAsString();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return this.read(ByteBuffer.wrap(decoded));
        }
    }

    public static <T> MinecraftData<T> register(NexoMinecraft nexo, ResourceLocation id, IData<T> data) {
        Holder.Reference<DataComponentType<T>> holder = nexo.getHelper().registerFeature(BuiltInRegistries.DATA_COMPONENT_TYPE, id, () -> {
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

}
