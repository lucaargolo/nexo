package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.model.loader.GltfModelLoader;
import dev.lucaargolo.nexo.api.render.model.loader.MinecraftModelLoader;
import dev.lucaargolo.nexo.api.render.model.loader.ModelLoader;
import dev.lucaargolo.nexo.api.render.model.loader.ObjModelLoader;
import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Model {

    private static final @NotNull List<ModelLoader> LOADERS = new CopyOnWriteArrayList<>();
    public static final @NotNull Location WHITE_TEXTURE = Location.of("nexo", "generated/model/white.png");
    private static final byte @NotNull [] WHITE_TEXTURE_DATA = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR4nGP4DwQACfsD/fteaysAAAAASUVORK5CYII="
    );

    private final @NotNull List<Mesh> meshes;
    private final @NotNull Map<String, ModelMaterial> materials;
    private final @NotNull Map<Location, Transform> transforms;
    private final boolean shade;
    private final @NotNull Map<Location, byte[]> embeddedTextures;

    public Model(
            @NotNull List<Mesh> meshes,
            @NotNull Map<String, ModelMaterial> materials,
            @NotNull Map<Location, Transform> transforms,
            boolean shade,
            @NotNull Map<Location, byte[]> embeddedTextures
    ) {
        this.meshes = List.copyOf(meshes);
        this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
        for (Mesh mesh : this.meshes) {
            if (!this.materials.containsKey(mesh.material())) {
                throw new IllegalArgumentException("Mesh references unknown material '" + mesh.material() + "'");
            }
        }
        this.transforms = Collections.unmodifiableMap(new LinkedHashMap<>(transforms));
        Map<Location, byte[]> textureData = new LinkedHashMap<>();
        embeddedTextures.forEach((location, data) -> textureData.put(location, data.clone()));
        if (materials.values().stream().anyMatch(material -> material.texture() == null)) {
            textureData.putIfAbsent(WHITE_TEXTURE, WHITE_TEXTURE_DATA.clone());
        }
        this.embeddedTextures = Collections.unmodifiableMap(textureData);
        this.shade = shade;
    }

    public Model(
            @NotNull List<Mesh> meshes,
            @NotNull Map<String, ModelMaterial> materials,
            @NotNull Map<Location, Transform> transforms,
            boolean shade
    ) {
        this(meshes, materials, transforms, shade, Map.of());
    }

    public @NotNull List<Mesh> meshes() {
        return meshes;
    }

    public @NotNull Map<String, ModelMaterial> materials() {
        return materials;
    }

    public @NotNull Map<Location, Transform> transforms() {
        return transforms;
    }

    public boolean shade() {
        return shade;
    }

    public @Nullable Transform getTransform(@NotNull Location location) {
        return transforms.get(location);
    }

    public @NotNull Map<Location, byte[]> embeddedTextures() {
        Map<Location, byte[]> copy = new LinkedHashMap<>();
        embeddedTextures.forEach((location, data) -> copy.put(location, data.clone()));
        return Collections.unmodifiableMap(copy);
    }

    public static @NotNull Model full(@NotNull Location texture) {
        float[] vertices = MinecraftModelLoader.boxVertices(0, 0, 0, 16, 16, 16);
        return new Model(List.of(new Mesh(PrimitiveType.QUADS, "all", vertices)), Map.of(
                "all", new ModelMaterial(texture, new float[]{1, 1, 1, 1}, CullMode.BACK, BlendMode.DISABLED)
        ), Map.of(
                Location.of("minecraft", "gui"),                   new Transform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0),   new Vector3f(0.625f, 0.625f, 0.625f)),
                Location.of("minecraft", "ground"),                new Transform(new Vector3f(0, 0, 0),    new Vector3f(0, 3, 0),   new Vector3f(0.25f, 0.25f, 0.25f)),
                Location.of("minecraft", "fixed"),                 new Transform(new Vector3f(0, 0, 0),    new Vector3f(0, 0, 0),   new Vector3f(0.5f, 0.5f, 0.5f)),
                Location.of("minecraft", "thirdperson_righthand"), new Transform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
                Location.of("minecraft", "firstperson_righthand"), new Transform(new Vector3f(0, 45, 0),  new Vector3f(0, 0, 0),   new Vector3f(0.4f, 0.4f, 0.4f)),
                Location.of("minecraft", "firstperson_lefthand"),  new Transform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0),   new Vector3f(0.4f, 0.4f, 0.4f))
        ), true);
    }

    public static @Nullable Model load(@NotNull Nexo nexo, @NotNull Location path) {
        byte[] data = nexo.loadResource(path);
        return data == null ? null : load(nexo, path, data);
    }

    public static @Nullable Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        for (ModelLoader loader : LOADERS) {
            if (!loader.supports(path)) continue;
            try {
                return loader.load(nexo, path, data);
            } catch (Exception e) {
                nexo.getLogger().error("Failed to parse model {} with {}", path, loader.getClass().getSimpleName(), e);
            }
        }
        return null;
    }

    static {
        registerLoader(new MinecraftModelLoader());
        registerLoader(new ObjModelLoader());
        registerLoader(new GltfModelLoader());
    }

    public static void registerLoader(@NotNull ModelLoader loader) {
        LOADERS.add(loader);
    }
}
