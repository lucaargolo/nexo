package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NexoAtlas {

    private static final Map<Location, Map<Location, Path>> REGISTRY = new ConcurrentHashMap<>();

    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");

    public static void register(@NotNull Location atlas, @NotNull Location texture, @NotNull Path path) {
        REGISTRY.computeIfAbsent(atlas, k -> new ConcurrentHashMap<>()).put(texture, path);
    }

    public static @NotNull Map<Location, Path> getRegistered(@NotNull Location atlas) {
        Map<Location, Path> sprites = REGISTRY.get(atlas);
        return sprites != null ? Collections.unmodifiableMap(sprites) : Collections.emptyMap();
    }

}
