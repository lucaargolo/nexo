package dev.lucaargolo.nexo.resource.model;

import com.mojang.datafixers.util.Either;
import dev.lucaargolo.nexo.NexoMinecraft;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinecraftModelResource extends ModelResource.Minecraft {

    private static final Map<Location, Minecraft> RESOURCE_MAP = new ConcurrentHashMap<>();

    private final boolean resolved;

    private MinecraftModelResource(Location location, boolean resolved, Supplier<Model> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return model != null || resolved;
    }

    public static Minecraft lookup(NexoMinecraft nexo, Location location) {
        Model model = lookupModel(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new MinecraftModelResource(location, model != null, model != null ? () -> model : () -> lookupModel(nexo, location)));
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
    public static Minecraft register(@NotNull NexoMinecraft nexo, @NotNull Minecraft resource) {
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
