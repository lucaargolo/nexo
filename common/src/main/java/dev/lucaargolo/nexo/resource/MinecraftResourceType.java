package dev.lucaargolo.nexo.resource;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.resource.language.LanguageResource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.resource.font.MinecraftFontResource;
import dev.lucaargolo.nexo.resource.image.MinecraftImageResource;
import dev.lucaargolo.nexo.resource.language.MinecraftLanguageResource;
import dev.lucaargolo.nexo.resource.model.MinecraftModelResource;
import dev.lucaargolo.nexo.resource.shader.MinecraftShaderResource;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MinecraftResourceType<T extends Resource<T>> {

    private static final Map<Resource.Type<?>, MinecraftResourceType<?>> TYPES = new HashMap<>();

    public static final MinecraftResourceType<ModelResource> MODEL = new MinecraftResourceType<>(
            Resource.Type.MODEL,
            MinecraftModelResource::lookup,
            MinecraftModelResource::register,
            MinecraftModelResource::register
    );

    public static final MinecraftResourceType<ImageResource> IMAGE = new MinecraftResourceType<>(
            Resource.Type.IMAGE,
            MinecraftImageResource::lookup,
            MinecraftImageResource::register,
            MinecraftImageResource::register
    );

    public static final MinecraftResourceType<ShaderResource> SHADER = new MinecraftResourceType<>(
            Resource.Type.SHADER,
            MinecraftShaderResource::lookup,
            MinecraftShaderResource::register
    );

    public static final MinecraftResourceType<FontResource> FONT = new MinecraftResourceType<>(
            Resource.Type.FONT,
            MinecraftFontResource::lookup,
            MinecraftFontResource::register,
            MinecraftFontResource::register
    );

    public static final MinecraftResourceType<LanguageResource> LANGUAGE = new MinecraftResourceType<>(
            Resource.Type.LANGUAGE,
            MinecraftLanguageResource::lookup,
            MinecraftLanguageResource::register,
            MinecraftLanguageResource::register
    );

    private final BiFunction<NexoMinecraft<?, ?, ?, ?>, Location, T> lookup;
    private final BiFunction<NexoMinecraft<?, ?, ?, ?>, Location, T> registrar;
    private final TriFunction<NexoMinecraft<?, ?, ?, ?>, Location, byte[], T> dataRegistrar;

    private MinecraftResourceType(
            Resource.Type<T> type,
            BiFunction<NexoMinecraft<?, ?, ?, ?>, Location, T> lookup,
            TriFunction<NexoMinecraft<?, ?, ?, ?>, Location, byte[], T> registrar
    ) {
        this.lookup = lookup;
        this.registrar = (nexo, location) -> {
            T resource = lookup.apply(nexo, location);
            if (resource == null) {
                throw new IllegalStateException("Could not register resource " + location);
            }
            return registrar.apply(nexo, location, resource.data());
        };
        this.dataRegistrar = registrar;
        TYPES.put(type, this);
    }

    private MinecraftResourceType(
            Resource.Type<T> type,
            BiFunction<NexoMinecraft<?, ?, ?, ?>, Location, T> lookup,
            BiFunction<NexoMinecraft<?, ?, ?, ?>, Location, T> registrar,
            TriFunction<NexoMinecraft<?, ?, ?, ?>, Location, byte[], T> dataRegistrar
    ) {
        this.lookup = lookup;
        this.registrar = registrar;
        this.dataRegistrar = dataRegistrar;
        TYPES.put(type, this);
    }

    @Nullable
    public T lookup(NexoMinecraft<?, ?, ?, ?> nexo, Location location) {
        return lookup.apply(nexo, location);
    }

    @NotNull
    public T register(NexoMinecraft<?, ?, ?, ?> nexo, Location location) {
        return registrar.apply(nexo, location);
    }

    @NotNull
    public T register(NexoMinecraft<?, ?, ?, ?> nexo, Location location, byte[] data) {
        return dataRegistrar.apply(nexo, location, data);
    }

    public static <T extends Resource<T>> MinecraftResourceType<T> of(Resource.Type<T> type) {
        MinecraftResourceType<?> t = TYPES.get(type);
        if (t == null) {
            throw new UnsupportedOperationException("Unsupported resource type: " + type);
        }
        Class<MinecraftResourceType<T>> clazz = Nexo.type(MinecraftResourceType.class);
        return clazz.cast(t);
    }

}
