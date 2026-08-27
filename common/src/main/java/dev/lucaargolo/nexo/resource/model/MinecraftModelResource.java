package dev.lucaargolo.nexo.resource.model;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MinecraftModelResource extends ModelResource {

    private static final Map<Location, ModelResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftModelResource(Location location, boolean resolved, Supplier<Model> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return model != null || resolved;
    }

    public static ModelResource lookup(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Location location) {
        ModelResource registered = RESOURCE_MAP.get(location);
        if (registered != null) {
            return registered;
        }

        Model loaded = Model.load(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, key -> new MinecraftModelResource(
                location,
                loaded != null,
                loaded != null ? () -> loaded : () -> Model.load(nexo, location)
        ));
    }

    public static @NotNull ModelResource register(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Location location) {
        return register(nexo, location, Model.load(nexo, location));
    }

    public static @NotNull ModelResource register(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Location location,
            byte @NotNull [] data
    ) {
        return register(nexo, location, Model.load(nexo, location, data));
    }

    private static @NotNull ModelResource register(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Location location,
            @Nullable Model loaded
    ) {
        if (loaded == null) {
            throw new IllegalArgumentException("Could not load model " + location);
        }

        ModelResource resource = new MinecraftModelResource(location, true, () -> loaded);
        RESOURCE_MAP.put(location, resource);
        nexo.getRenderingHandler().registerModel(resource);
        return resource;
    }

}
