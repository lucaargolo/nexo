package dev.lucaargolo.nexo.resource;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.resource.image.PNGImageResource;
import dev.lucaargolo.nexo.resource.model.GltfModelResource;
import dev.lucaargolo.nexo.resource.model.MinecraftModelResource;
import dev.lucaargolo.nexo.resource.model.ObjModelResource;
import dev.lucaargolo.nexo.resource.shader.FshShaderResource;
import dev.lucaargolo.nexo.resource.shader.VshShaderResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MinecraftResourceType<T extends Resource<T>> {

    private static final Map<Resource.Type<?>, MinecraftResourceType<?>> TYPES = new HashMap<>();

    public static final MinecraftResourceType<ModelResource.Minecraft> MINECRAFT_MODEL = new MinecraftResourceType<>(
            Resource.Type.MINECRAFT_MODEL,
            MinecraftModelResource::lookup,
            MinecraftModelResource::register
    );

    public static final MinecraftResourceType<ModelResource.GLTF> GLTF_MODEL = new MinecraftResourceType<>(
            Resource.Type.GLTF_MODEL,
            GltfModelResource::lookup,
            GltfModelResource::register
    );

    public static final MinecraftResourceType<ModelResource.OBJ> OBJ_MODEL = new MinecraftResourceType<>(
            Resource.Type.OBJ_MODEL,
            ObjModelResource::lookup,
            ObjModelResource::register
    );

    public static final MinecraftResourceType<ImageResource.PNG> IMAGE_PNG = new MinecraftResourceType<>(
            Resource.Type.PNG_IMAGE,
            PNGImageResource::lookup,
            PNGImageResource::register
    );

    public static final MinecraftResourceType<ShaderResource.VSH> VERTEX_SHADER = new MinecraftResourceType<>(
            Resource.Type.VSH_SHADER,
            VshShaderResource::lookup,
            VshShaderResource::register
    );

    public static final MinecraftResourceType<ShaderResource.FSH> FRAGMENT_SHADER = new MinecraftResourceType<>(
            Resource.Type.FSH_SHADER,
            FshShaderResource::lookup,
            FshShaderResource::register
    );

    private final BiFunction<NexoMinecraft, Location, T> lookup;
    private final BiFunction<NexoMinecraft, T, T> registrar;

    private MinecraftResourceType(
            Resource.Type<T> type,
            BiFunction<NexoMinecraft, Location, T> lookup,
            BiFunction<NexoMinecraft, T, T> registrar
    ) {
        this.lookup = lookup;
        this.registrar = registrar;
        TYPES.put(type, this);
    }

    @Nullable
    public T lookup(NexoMinecraft nexo, Location location) {
        return lookup.apply(nexo, location);
    }

    @NotNull
    public T register(NexoMinecraft nexo, T resource) {
        return registrar.apply(nexo, resource);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Resource<T>> MinecraftResourceType<T> of(Resource.Type<T> type) {
        MinecraftResourceType<?> t = TYPES.get(type);
        if (t == null) {
            throw new UnsupportedOperationException("Unsupported resource type: " + type);
        }
        return (MinecraftResourceType<T>) t;
    }

    public static Collection<MinecraftResourceType<?>> all() {
        return TYPES.values();
    }

}
