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

public class ObjModelResource extends ModelResource.OBJ {

    private static final Map<Location, OBJ> RESOURCE_MAP = new ConcurrentHashMap<>();

    private ObjModelResource(Location location, Supplier<Model> supplier) {
        super(location, supplier);
    }

    public static OBJ lookup(NexoMinecraft nexo, Location location) {
        return RESOURCE_MAP.computeIfAbsent(location, l -> new ModelResource.OBJ(location, () -> {
            Model model = lookupModel(nexo, location);
            if (model != null) {
                return model;
            } else {
                NexoMinecraft.LOGGER.error("Could not find OBJ model for location {}", location);
                return Model.MISSING_MODEL.model();
            }
        }));
    }

    @Nullable
    private static Model lookupModel(NexoMinecraft nexo, Location location) {
        Model model = Optional.ofNullable(nexo.loadResource(location)).map(data -> Model.load(nexo, location, data)).orElse(null);
        if (model != null) {
            return model;
        }
        NexoMinecraft.LOGGER.debug("Could not find OBJ model for location {}", location);
        if (!location.path().contains("models/")) {
            model = lookupModel(nexo, location.withPathPrefix("models/"));
            if (model != null) {
                return model;
            }
        }
        if (!location.path().endsWith(".obj")) {
            return lookupModel(nexo, location.withPathSuffix(".obj"));
        }
        return null;
    }

    @NotNull
    public static OBJ register(@NotNull NexoMinecraft nexo, @NotNull OBJ resource) {
        Location location = resource.location().withPath(l -> {
            return l.path().replace("models/", "").replace(".obj", "");
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
