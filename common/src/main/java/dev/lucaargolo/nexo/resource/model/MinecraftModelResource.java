package dev.lucaargolo.nexo.resource.model;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MinecraftModelResource extends ModelResource {

    private static final Map<Location, ModelResource> RESOURCE_MAP = new ConcurrentHashMap<>();

    private MinecraftModelResource(Location location, Model model) {
        super(location, model);
    }

    @Nullable
    public static ModelResource lookup(NexoMinecraft nexo, Location location) {
        ModelResource resource = RESOURCE_MAP.get(location);
        if (resource != null) return resource;

        Model model = Model.load(nexo, location);
        if (model != null) {
            resource = new MinecraftModelResource(location, model);
            RESOURCE_MAP.put(location, resource);
            return resource;
        }

        if (nexo.getMod(location.namespace()) != null) return null;

        Location mcLocation = Location.of(location.namespace(), "models/" + location.path());
        byte[] data = nexo.loadResource(mcLocation);
        if (data != null) {
            model = Model.load(nexo, mcLocation, data);
            if (model != null) {
                resource = new MinecraftModelResource(location, model);
                RESOURCE_MAP.put(location, resource);
                return resource;
            }
        }
        return null;
    }

    @NotNull
    public static ModelResource register(NexoMinecraft nexo, ModelResource resource) {
        RESOURCE_MAP.put(resource.location(), resource);
        BlockModel blockModel = BlockModel.fromString("{}");
        blockModel.name = resource.location().toString();
        nexo.getRenderingHandler().registerResourceModel(
            NexoMinecraft.rl(resource.location()),
            () -> blockModel
        );
        return resource;
    }

}
