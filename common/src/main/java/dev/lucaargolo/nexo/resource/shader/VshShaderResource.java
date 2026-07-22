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

public class VshShaderResource extends ShaderResource.VSH {

    private static final Map<Location, VSH> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private VshShaderResource(Location location, boolean resolved, Supplier<String> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return source != null || resolved;
    }

    public static VSH lookup(NexoMinecraft nexo, Location location) {
        String shader = lookupShader(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new VshShaderResource(location, shader != null, shader != null ? () -> shader : () -> lookupShader(nexo, location)));
    }

    @Nullable
    private static String lookupShader(NexoMinecraft nexo, Location location) {
        byte[] data = nexo.loadResource(location);
        if (data != null) {
            return new String(data, StandardCharsets.UTF_8);
        }
        NexoMinecraft.LOGGER.debug("Could not find vertex shader for location {}", location);
        if (!location.path().contains("shaders/")) {
            String source = lookupShader(nexo, location.withPathPrefix("shaders/"));
            if (source != null) {
                return source;
            }
        }
        if (!location.path().endsWith(".vsh")) {
            return lookupShader(nexo, location.withPathSuffix(".vsh"));
        }
        return null;
    }

    @NotNull
    public static VSH register(@NotNull NexoMinecraft nexo, @NotNull VSH resource) {
        Location location = resource.location().withPath(l -> {
            return l.path().replace("shaders/", "").replace(".vsh", "");
        });
        RESOURCE_MAP.put(location, resource);
        return resource;
    }

}
