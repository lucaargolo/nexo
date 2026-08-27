package dev.lucaargolo.nexo.resource.shader;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MinecraftShaderResource extends ShaderResource {

    private static final Map<Location, ShaderResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftShaderResource(Location location, boolean resolved, Supplier<String> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return source != null || resolved;
    }

    public static ShaderResource lookup(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Location location) {
        ShaderResource registered = RESOURCE_MAP.get(location);
        if (registered != null) {
            return registered;
        }

        String source = lookupShader(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, key -> new MinecraftShaderResource(
                location,
                source != null,
                source != null ? () -> source : () -> lookupShader(nexo, location)
        ));
    }

    private static @Nullable String lookupShader(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Location location) {
        byte[] data = nexo.loadResource(location);
        if (data != null) {
            return new String(data, StandardCharsets.UTF_8);
        }

        if (!location.path().contains("shaders/")) {
            data = nexo.loadResource(location.withPathPrefix("shaders/"));
            if (data != null) {
                return new String(data, StandardCharsets.UTF_8);
            }
        }

        NexoMinecraft.LOGGER.debug("Could not find shader for location {}", location);
        return null;
    }

    public static @NotNull ShaderResource register(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull Location location,
            byte @NotNull [] data
    ) {
        ShaderResource shader = new MinecraftShaderResource(
                location,
                true,
                () -> new String(data, StandardCharsets.UTF_8)
        );
        RESOURCE_MAP.put(location, shader);
        return shader;
    }

}
