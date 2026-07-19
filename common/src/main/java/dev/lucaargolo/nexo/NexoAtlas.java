package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NexoAtlas {

    private static final Map<Location, Map<Location, Path>> REGISTRY = new ConcurrentHashMap<>();
    private static final Map<Location, Map<Location, byte[]>> EMBEDDED_REGISTRY = new ConcurrentHashMap<>();

    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");

    public static void register(@NotNull Location atlas, @NotNull Location texture, @NotNull Path path) {
        REGISTRY.computeIfAbsent(atlas, k -> new ConcurrentHashMap<>()).put(texture, path);
    }

    public static void register(@NotNull Location atlas, @NotNull Location texture, byte @NotNull [] data) {
        EMBEDDED_REGISTRY.computeIfAbsent(atlas, k -> new ConcurrentHashMap<>()).put(texture, data.clone());
    }

    public static @NotNull Map<Location, Path> getRegistered(@NotNull Location atlas) {
        Map<Location, Path> sprites = REGISTRY.get(atlas);
        return sprites != null ? Collections.unmodifiableMap(sprites) : Collections.emptyMap();
    }

    public static @NotNull Map<Location, byte[]> getEmbedded(@NotNull Location atlas) {
        Map<Location, byte[]> sprites = EMBEDDED_REGISTRY.get(atlas);
        if (sprites == null) return Collections.emptyMap();
        Map<Location, byte[]> result = new LinkedHashMap<>();
        sprites.forEach((location, data) -> result.put(location, data.clone()));
        return Collections.unmodifiableMap(result);
    }

}
