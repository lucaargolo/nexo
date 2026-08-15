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

public class MinecraftMetaModelResource extends ModelResource.Minecraft {

    private static final Map<Location, Minecraft> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftMetaModelResource(Location location, boolean resolved, Supplier<Model> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return model != null || resolved;
    }

    public static Minecraft lookup(NexoMinecraft nexo, Location location) {
        Model model = lookupModel(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new MinecraftMetaModelResource(location, model != null, model != null ? () -> model : () -> lookupModel(nexo, location)));
    }


    @Nullable
    private static Model lookupModel(NexoMinecraft nexo, Location location) {
        Model model = Optional.ofNullable(nexo.loadResource(location)).map(data -> Model.load(nexo, location, data)).orElse(null);
        if(model != null) {
            return model;
        }
        NexoMinecraft.LOGGER.debug("Could not find Minecraft model for location {}", location);
        if (!location.path().contains("models/")) {
            model = lookupModel(nexo, location.withPathPrefix("models/"));
            if(model != null) {
                return model;
            }
        }
        if(!location.path().endsWith(".json")) {
            return lookupModel(nexo, location.withPathSuffix(".json"));
        }
        return null;
    }

    @NotNull
    //TODO: Actually provide the model back to Minecraft
    public static ModelResource.Minecraft register(@NotNull NexoMinecraft nexo, @NotNull Location location, byte[] data) {
        ModelResource.Minecraft model = new MinecraftMetaModelResource(location, true, () -> Model.load(nexo, location, data));
        RESOURCE_MAP.put(location, model);
        return model;
    }

}
