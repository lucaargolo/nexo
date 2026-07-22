package dev.lucaargolo.nexo.resource.model;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.model.NexoUnbakedModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class GltfModelResource extends ModelResource.GLTF {

    private static final Map<Location, GLTF> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private GltfModelResource(Location location, boolean resolved, Supplier<Model> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return model != null || resolved;
    }

    public static GLTF lookup(NexoMinecraft nexo, Location location) {
        Model model = lookupModel(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new GltfModelResource(location, model != null, model != null ? () -> model : () -> lookupModel(nexo, location)));
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
    public static GLTF register(@NotNull NexoMinecraft nexo, @NotNull GLTF resource) {
        Location location = resource.location().withPath(l -> {
            String path = l.path().replace("models/", "");
            if (path.endsWith(".gltf")) return path.replace(".gltf", "");
            if (path.endsWith(".glb")) return path.replace(".glb", "");
            return path;
        });
        RESOURCE_MAP.put(location, resource);
        Model model = resource.model();
        ItemTransforms transforms = NexoUnbakedModel.getItemTransforms(model::transform);
        BlockModel blockModel = new BlockModel(
                null,
                List.of(),
                Map.of(),
                model.shade(),
                null,
                transforms,
                List.of()
        );
        nexo.getRenderingHandler().registerResourceModel(NexoMinecraft.rl(resource.location()), () -> blockModel);
        return resource;
    }

}
