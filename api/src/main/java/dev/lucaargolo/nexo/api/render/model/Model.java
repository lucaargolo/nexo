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
        byte @NotNull [] data,
        @NotNull List<Mesh> meshes,
        @NotNull Map<String, Material<?>> materials,
        @NotNull Map<Location, Transform> transforms,
        boolean shade
) {

    private static final @NotNull List<ModelLoader> LOADERS = new CopyOnWriteArrayList<>();

    public Model(
            byte @NotNull [] data,
            @NotNull List<Mesh> meshes,
            @NotNull Map<String, Material<?>> materials,
            @NotNull Map<Location, Transform> transforms,
            boolean shade
    ) {
        this.data = data;
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

    public static @Nullable Model load(@NotNull Nexo nexo, @NotNull Location path) {
        Model model = loadResource(nexo, path);
        if (model != null) {
            return model;
        }

        if (!path.path().contains("models/")) {
            model = loadResource(nexo, path.withPathPrefix("models/"));
            if (model != null) {
                return model;
            }
        }

        nexo.getLogger().debug("Could not find model for location {}", path);
        return null;
    }

    public static @Nullable Model load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        for (ModelLoader loader : LOADERS) {
            if (!loader.supports(path)) {
                continue;
            }
            Model model = load(loader, nexo, path, data);
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    private static @Nullable Model loadResource(@NotNull Nexo nexo, @NotNull Location path) {
        for (ModelLoader loader : LOADERS) {
            for (Location resolvedPath : loader.resolve(path)) {
                byte[] data = nexo.loadResource(resolvedPath);
                if (data == null) {
                    continue;
                }
                Model model = load(loader, nexo, resolvedPath, data);
                if (model != null) {
                    return model;
                }
            }
        }
        return null;
    }

    private static @Nullable Model load(
            @NotNull ModelLoader loader,
            @NotNull Nexo nexo,
            @NotNull Location path,
            byte @NotNull [] data
    ) {
        try {
            return loader.load(nexo, path, data);
        } catch (Exception e) {
            nexo.getLogger().error("Failed to parse model {} with {}", path, loader.getClass().getSimpleName(), e);
            return null;
        }
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
