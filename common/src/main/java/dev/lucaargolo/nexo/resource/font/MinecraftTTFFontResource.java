package dev.lucaargolo.nexo.resource.font;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinecraftTTFFontResource extends FontResource.TTF {

    private static final Map<Location, TTF> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftTTFFontResource(Location location, boolean resolved, Supplier<byte[]> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return data != null || resolved;
    }

    public static TTF lookup(NexoMinecraft nexo, Location location) {
        byte[] data = lookupFont(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new MinecraftTTFFontResource(location, data != null, data != null ? () -> data : () -> lookupFont(nexo, location)));
    }

    private static byte @Nullable [] lookupFont(NexoMinecraft nexo, Location location) {
        byte[] data = nexo.loadResource(location);
        if (data != null) {
            return data;
        }
        NexoMinecraft.LOGGER.debug("Could not find ttf font for location {}", location);
        if (!location.path().contains("font/")) {
            data = lookupFont(nexo, location.withPathPrefix("font/"));
            if (data != null) {
                return data;
            }
        }
        if(!location.path().endsWith(".ttf")) {
            data = lookupFont(nexo, location.withPathSuffix(".ttf"));
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    @NotNull
    //TODO: Actually provide the TTF back to Minecraft
    public static TTF register(@NotNull NexoMinecraft nexo, @NotNull Location location, byte[] data) {
        TTF ttf = new MinecraftTTFFontResource(location, true, () -> data);
        RESOURCE_MAP.put(location, ttf);
        return ttf;
    }

}
