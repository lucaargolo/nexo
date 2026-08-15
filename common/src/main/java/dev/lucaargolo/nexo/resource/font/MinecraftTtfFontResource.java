package dev.lucaargolo.nexo.resource.font;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinecraftTtfFontResource extends FontResource.TTF {

    private static final Map<Location, TTF> RESOURCE_MAP = new ConcurrentHashMap<>();

    public MinecraftTtfFontResource(@NotNull Location location, @NotNull Supplier<byte @Nullable []> supplier) {
        super(location, supplier);
    }

    public static @Nullable TTF lookup(@NotNull NexoMinecraft nexo, @NotNull Location location) {
        TTF resource = RESOURCE_MAP.get(location);
        if (resource == null) {
            resource = RESOURCE_MAP.get(canonical(location));
        }
        return resource;
    }

    @NotNull
    public static TTF register(@NotNull NexoMinecraft nexo, @NotNull TTF resource) {
        RESOURCE_MAP.put(canonical(resource.location()), resource);
        return resource;
    }

    private static @NotNull Location canonical(@NotNull Location location) {
        return location.withPath(loc -> loc.path().replace("fonts/", "").replace(".ttf", "").replace(".otf", ""));
    }
}
