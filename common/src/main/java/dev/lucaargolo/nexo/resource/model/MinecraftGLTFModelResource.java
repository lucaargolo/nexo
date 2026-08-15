package dev.lucaargolo.nexo.resource.model;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinecraftGLTFModelResource extends ModelResource.GLTF {

    private static final Map<Location, GLTF> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftGLTFModelResource(Location location, boolean resolved, Supplier<Model> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return model != null || resolved;
    }

    public static GLTF lookup(NexoMinecraft nexo, Location location) {
        Model model = lookupModel(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new MinecraftGLTFModelResource(location, model != null, model != null ? () -> model : () -> lookupModel(nexo, location)));
    }

    @Nullable
    private static Model lookupModel(NexoMinecraft nexo, Location location) {
        Model model = Optional.ofNullable(nexo.loadResource(location)).map(data -> Model.load(nexo, location, data)).orElse(null);
        if (model != null) {
            return model;
        }
        NexoMinecraft.LOGGER.debug("Could not find GLTF model for location {}", location);
        if (!location.path().contains("models/")) {
            model = lookupModel(nexo, location.withPathPrefix("models/"));
            if (model != null) {
                return model;
            }
        }
        if (!location.path().endsWith(".gltf") && !location.path().endsWith(".glb")) {
            model = lookupModel(nexo, location.withPathSuffix(".gltf"));
            if (model != null) {
                return model;
            }
            return lookupModel(nexo, location.withPathSuffix(".glb"));
        }
        return null;
    }

    @NotNull
    //TODO: Actually provide the model back to Minecraft
    public static ModelResource.GLTF register(@NotNull NexoMinecraft nexo, @NotNull Location location, byte[] data) {
        ModelResource.GLTF model = new MinecraftGLTFModelResource(location, true, () -> Model.load(nexo, location, data));
        RESOURCE_MAP.put(location, model);
        return model;
    }

}
