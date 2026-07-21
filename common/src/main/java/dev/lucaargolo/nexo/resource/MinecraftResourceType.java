package dev.lucaargolo.nexo.resource;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.resource.model.MinecraftModelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MinecraftResourceType<T extends Resource<T>> {

    private static final Map<Resource.Type<?>, MinecraftResourceType<?>> TYPES = new HashMap<>();

    public static final MinecraftResourceType<ModelResource> MINECRAFT_MODEL = new MinecraftResourceType<>(
            Resource.Type.MINECRAFT_MODEL,
            MinecraftModelResource::lookup,
            MinecraftModelResource::register
    );

    private final BiFunction<NexoMinecraft, Location, T> lookup;
    private final BiFunction<NexoMinecraft, T, T> registrar;

    private MinecraftResourceType(
            Resource.Type<T> type,
            BiFunction<NexoMinecraft, Location, T> lookup,
            BiFunction<NexoMinecraft, T, T> registrar
    ) {
        this.lookup = lookup;
        this.registrar = registrar;
        TYPES.put(type, this);
    }

    @Nullable
    public T lookup(NexoMinecraft nexo, Location location) {
        return lookup.apply(nexo, location);
    }

    @NotNull
    public T register(NexoMinecraft nexo, T resource) {
        return registrar.apply(nexo, resource);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Resource<T>> MinecraftResourceType<T> of(Resource.Type<T> type) {
        MinecraftResourceType<?> t = TYPES.get(type);
        if (t == null) {
            throw new UnsupportedOperationException("Unsupported resource type: " + type);
        }
        return (MinecraftResourceType<T>) t;
    }

    public static Collection<MinecraftResourceType<?>> all() {
        return TYPES.values();
    }

}
