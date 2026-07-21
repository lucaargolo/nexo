package dev.lucaargolo.nexo.resource.image;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PNGImageResource extends ImageResource.PNG {

    private static final Map<Location, PNG> RESOURCE_MAP = new ConcurrentHashMap<>();

    private PNGImageResource(Location location, Supplier<byte[]> supplier) {
        super(location, supplier);
    }

    public static PNG lookup(NexoMinecraft nexo, Location location) {
        return RESOURCE_MAP.computeIfAbsent(location, l -> new PNG(location, () -> {
            return lookupImage(nexo, location);
        }));
    }

    private static byte @Nullable [] lookupImage(NexoMinecraft nexo, Location location) {
        byte[] data = nexo.loadResource(location);
        if (data != null) {
            return data;
        }
        NexoMinecraft.LOGGER.debug("Could not find png image for location {}", location);
        if (!location.path().contains("textures/")) {
            data = lookupImage(nexo, location.withPathPrefix("textures/"));
            if (data != null) {
                return data;
            }
        }
        if(!location.path().endsWith(".png")) {
            data = lookupImage(nexo, location.withPathSuffix(".png"));
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    @NotNull
    public static PNG register(@NotNull NexoMinecraft nexo, @NotNull PNG resource) {
        Location location = resource.location().withPath(l -> {
            return l.path().replace("textures/", "").replace(".png", "");
        });
        RESOURCE_MAP.put(location, resource);
        return resource;
    }

}
