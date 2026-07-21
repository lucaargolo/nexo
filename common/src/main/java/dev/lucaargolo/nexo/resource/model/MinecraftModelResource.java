package dev.lucaargolo.nexo.resource.model;

import com.mojang.datafixers.util.Either;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.model.NexoUnbakedModel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinecraftModelResource extends ModelResource {

    private static final Map<Location, ModelResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private MinecraftModelResource(Location location, Supplier<Model> supplier) {
        super(location, supplier);
    }

    public static ModelResource lookup(NexoMinecraft nexo, Location location) {
        return RESOURCE_MAP.computeIfAbsent(location, l -> new ModelResource(location, () -> {
            Model model = lookupModel(nexo, location);
            if (model != null) {
                return model;
            }else{
                NexoMinecraft.LOGGER.error("Could not find model for location {}", location);
                return Model.MISSING_MODEL.model();
            }
        }));
    }

    @Nullable
    private static Model lookupModel(NexoMinecraft nexo, Location location) {
        byte[] data = nexo.loadResource(location);
        Model model;
        if (data != null) {
            model = Model.load(nexo, location, data);
            if(model != null) {
                return model;
            }
        }

        if (!location.path().contains("models/")) {
            return lookupModel(nexo, location.withPathPrefix("models/"));
        }
        if(!location.path().endsWith(".json")) {
            return lookupModel(nexo, location.withPathSuffix(".json"));
        }
        return null;
    }

    @NotNull
    public static ModelResource register(@NotNull NexoMinecraft nexo, @NotNull ModelResource resource) {
        Location location = resource.location().withPath(l -> {
            return l.path().replace("models/", "").replace(".json", "");
        });
        RESOURCE_MAP.put(location, resource);
        Model model = resource.model();
        //TODO populate elements and textureMap from Model
        List<BlockElement> elements = new ArrayList<>();
        Map<String, Either<Material, String>> textureMap = new HashMap<>();
        ItemTransforms transforms = NexoUnbakedModel.getItemTransforms(model::transform);
        BlockModel blockModel = new BlockModel(
            null,
                elements,
                textureMap,
                model.shade(),
                null,
                transforms,
                List.of()
        );
        nexo.getRenderingHandler().registerResourceModel(NexoMinecraft.rl(resource.location()), () -> blockModel);
        return resource;
    }

}
