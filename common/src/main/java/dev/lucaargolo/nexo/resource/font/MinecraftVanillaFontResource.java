package dev.lucaargolo.nexo.resource.font;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftVanillaFontResource extends FontResource.Minecraft {

    private static final Map<Location, Minecraft> RESOURCE_MAP = new ConcurrentHashMap<>();

    public MinecraftVanillaFontResource(@NotNull Location location) {
        super(location);
    }

    public static @Nullable Minecraft lookup(@NotNull NexoMinecraft nexo, @NotNull Location location) {
        if (location.equals(FontResource.Minecraft.DEFAULT_LOCATION)) {
            return RESOURCE_MAP.computeIfAbsent(location, MinecraftVanillaFontResource::new);
        }
        return RESOURCE_MAP.get(location);
    }

    @NotNull
    public static Minecraft register(@NotNull NexoMinecraft nexo, @NotNull Minecraft resource) {
        RESOURCE_MAP.put(resource.location(), resource);
        return resource;
    }
}
