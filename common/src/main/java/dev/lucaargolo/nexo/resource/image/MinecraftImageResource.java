package dev.lucaargolo.nexo.resource.image;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.image.Image;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MinecraftImageResource extends ImageResource {

    private static final Map<Location, ImageResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftImageResource(Location location, boolean resolved, Supplier<Image> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return image != null || resolved;
    }

    public static ImageResource lookup(NexoMinecraft nexo, Location location) {
        ImageResource registered = RESOURCE_MAP.get(location);
        if (registered != null) {
            return registered;
        }

        Image loaded = Image.load(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, key -> new MinecraftImageResource(
                location,
                loaded != null,
                loaded != null ? () -> loaded : () -> Image.load(nexo, location)
        ));
    }

    public static @NotNull ImageResource register(@NotNull NexoMinecraft nexo, @NotNull Location location) {
        return register(nexo, location, Image.load(nexo, location));
    }

    public static @NotNull ImageResource register(
            @NotNull NexoMinecraft nexo,
            @NotNull Location location,
            byte @NotNull [] data
    ) {
        return register(nexo, location, Image.load(nexo, location, data));
    }

    private static @NotNull ImageResource register(
            @NotNull NexoMinecraft nexo,
            @NotNull Location location,
            @Nullable Image loaded
    ) {
        if (loaded == null) {
            throw new IllegalArgumentException("Could not load image " + location);
        }

        ImageResource resource = new MinecraftImageResource(location, true, () -> loaded);
        RESOURCE_MAP.put(location, resource);
        nexo.registerImageResource(resource);
        return resource;
    }
}
