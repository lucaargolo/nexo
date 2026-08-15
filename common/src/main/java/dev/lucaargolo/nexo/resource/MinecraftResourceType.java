package dev.lucaargolo.nexo.resource;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.resource.image.ImageResource;
import dev.lucaargolo.nexo.api.resource.model.ModelResource;
import dev.lucaargolo.nexo.api.resource.shader.ShaderResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.resource.font.MinecraftTTFFontResource;
import dev.lucaargolo.nexo.resource.image.MinecraftPNGImageResource;
import dev.lucaargolo.nexo.resource.model.MinecraftGLTFModelResource;
import dev.lucaargolo.nexo.resource.model.MinecraftMetaModelResource;
import dev.lucaargolo.nexo.resource.model.MinecraftOBJModelResource;
import dev.lucaargolo.nexo.resource.shader.MinecraftFSHShaderResource;
import dev.lucaargolo.nexo.resource.shader.MinecraftVSHShaderResource;
import org.apache.commons.lang3.function.TriFunction;
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
            MinecraftMetaModelResource::lookup,
            MinecraftMetaModelResource::register
    );

    public static final MinecraftResourceType<ModelResource.GLTF> GLTF_MODEL = new MinecraftResourceType<>(
            Resource.Type.GLTF_MODEL,
            MinecraftGLTFModelResource::lookup,
            MinecraftGLTFModelResource::register
    );

    public static final MinecraftResourceType<ModelResource.OBJ> OBJ_MODEL = new MinecraftResourceType<>(
            Resource.Type.OBJ_MODEL,
            MinecraftOBJModelResource::lookup,
            MinecraftOBJModelResource::register
    );

    public static final MinecraftResourceType<ImageResource.PNG> IMAGE_PNG = new MinecraftResourceType<>(
            Resource.Type.PNG_IMAGE,
            MinecraftPNGImageResource::lookup,
            MinecraftPNGImageResource::register
    );

    public static final MinecraftResourceType<ShaderResource.VSH> VERTEX_SHADER = new MinecraftResourceType<>(
            Resource.Type.VSH_SHADER,
            MinecraftVSHShaderResource::lookup,
            MinecraftVSHShaderResource::register
    );

    public static final MinecraftResourceType<ShaderResource.FSH> FRAGMENT_SHADER = new MinecraftResourceType<>(
            Resource.Type.FSH_SHADER,
            MinecraftFSHShaderResource::lookup,
            MinecraftFSHShaderResource::register
    );

    public static final MinecraftResourceType<FontResource.TTF> FONT_TTF = new MinecraftResourceType<>(
            Resource.Type.FONT_TTF,
            MinecraftTTFFontResource::lookup,
            MinecraftTTFFontResource::register
    );

    private final BiFunction<NexoMinecraft, Location, T> lookup;
    private final TriFunction<NexoMinecraft, Location, byte[], T> registrar;

    private MinecraftResourceType(
            Resource.Type<T> type,
            BiFunction<NexoMinecraft, Location, T> lookup,
            TriFunction<NexoMinecraft, Location, byte[], T> registrar
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
    public T register(NexoMinecraft nexo, Location location) {
        T resource = lookup.apply(nexo, location);
        if (resource == null) {
            throw new IllegalStateException("Could not register resource " + location);
        }
        return resource;
    }

    @NotNull
    public T register(NexoMinecraft nexo, Location location, byte[] data) {
        return registrar.apply(nexo, location, data);
    }

    public static <T extends Resource<T>> MinecraftResourceType<T> of(Resource.Type<T> type) {
        MinecraftResourceType<?> t = TYPES.get(type);
        if (t == null) {
            throw new UnsupportedOperationException("Unsupported resource type: " + type);
        }
        Class<MinecraftResourceType<T>> clazz = Nexo.type(MinecraftResourceType.class);
        return clazz.cast(t);
    }

    public static Collection<MinecraftResourceType<?>> all() {
        return TYPES.values();
    }

}
