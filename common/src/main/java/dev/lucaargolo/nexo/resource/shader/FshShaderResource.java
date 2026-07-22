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

public class FshShaderResource extends ShaderResource.FSH {

    private static final Map<Location, FSH> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private FshShaderResource(Location location, boolean resolved, Supplier<String> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return source != null || resolved;
    }

    public static FSH lookup(NexoMinecraft nexo, Location location) {
        String shader = lookupShader(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new FshShaderResource(location, shader != null, shader != null ? () -> shader : () -> lookupShader(nexo, location)));
    }

    @Nullable
    private static String lookupShader(NexoMinecraft nexo, Location location) {
        byte[] data = nexo.loadResource(location);
        if (data != null) {
            return new String(data, StandardCharsets.UTF_8);
        }
        NexoMinecraft.LOGGER.debug("Could not find fragment shader for location {}", location);
        if (!location.path().contains("shaders/")) {
            String source = lookupShader(nexo, location.withPathPrefix("shaders/"));
            if (source != null) {
                return source;
            }
        }
        if (!location.path().endsWith(".fsh")) {
            return lookupShader(nexo, location.withPathSuffix(".fsh"));
        }
        return null;
    }

    @NotNull
    public static FSH register(@NotNull NexoMinecraft nexo, @NotNull FSH resource) {
        Location location = resource.location().withPath(l -> {
            return l.path().replace("shaders/", "").replace(".fsh", "");
        });
        RESOURCE_MAP.put(location, resource);
        return resource;
    }

}
