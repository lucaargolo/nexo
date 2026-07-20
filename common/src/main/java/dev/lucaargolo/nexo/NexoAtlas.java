package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NexoAtlas {

    public static final Location BLOCK_ATLAS = Location.of("minecraft", "textures/atlas/blocks.png");

    private final Map<Location, Map<Location, Path>> registry = new ConcurrentHashMap<>();
    private final Map<Location, Map<Location, byte[]>> embeddedRegistry = new ConcurrentHashMap<>();

    public void register(@NotNull Location atlas, @NotNull Location texture, @NotNull Path path) {
        registry.computeIfAbsent(atlas, k -> new ConcurrentHashMap<>()).put(texture, path);
    }

    public void register(@NotNull Location atlas, @NotNull Location texture, byte @NotNull [] data) {
        embeddedRegistry.computeIfAbsent(atlas, k -> new ConcurrentHashMap<>()).put(texture, data.clone());
    }

    public @NotNull Map<Location, Path> getRegistered(@NotNull Location atlas) {
        Map<Location, Path> sprites = registry.get(atlas);
        return sprites != null ? Collections.unmodifiableMap(sprites) : Collections.emptyMap();
    }

    public @NotNull Map<Location, byte[]> getEmbedded(@NotNull Location atlas) {
        Map<Location, byte[]> sprites = embeddedRegistry.get(atlas);
        if (sprites == null) return Collections.emptyMap();
        Map<Location, byte[]> result = new LinkedHashMap<>();
        sprites.forEach((location, data) -> result.put(location, data.clone()));
        return Collections.unmodifiableMap(result);
    }

}
