package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.render.model.loader.GltfModelLoader;
import dev.lucaargolo.nexo.api.render.model.loader.MinecraftModelLoader;
import dev.lucaargolo.nexo.api.render.model.loader.ModelLoader;
import dev.lucaargolo.nexo.api.render.model.loader.ObjModelLoader;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public record Model(
        @NotNull List<Mesh> meshes,
        @NotNull Map<String, Material<?>> materials,
        @NotNull Map<Location, Transform> transforms,
        boolean shade
) {

    private static final @NotNull List<ModelLoader> LOADERS = new CopyOnWriteArrayList<>();

    public Model(
            @NotNull List<Mesh> meshes,
            @NotNull Map<String, Material<?>> materials,
            @NotNull Map<Location, Transform> transforms,
            boolean shade
    ) {
        this.meshes = List.copyOf(meshes);
        this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
        for (Mesh mesh : this.meshes) {
            if (!this.materials.containsKey(mesh.material())) {
                throw new IllegalArgumentException("Mesh references unknown material '" + mesh.material() + "'");
            }
        }
        this.transforms = Collections.unmodifiableMap(new LinkedHashMap<>(transforms));
        this.shade = shade;
    }

    public @Nullable Transform transform(@NotNull Location location) {
        return transforms.get(location);
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
