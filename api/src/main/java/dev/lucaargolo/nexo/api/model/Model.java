package dev.lucaargolo.nexo.api.model;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.model.loader.MinecraftModelLoader;
import dev.lucaargolo.nexo.api.model.loader.ModelLoader;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public record Model(
    @NotNull List<Cube> cubes,
    @NotNull Map<String, Location> textures,
    @NotNull Map<Location, Transform> transforms,
    boolean shade
) {

    private static final @NotNull List<ModelLoader> LOADERS = new CopyOnWriteArrayList<>();

    public Model(
        @NotNull List<Cube> cubes,
        @NotNull Map<String, Location> textures,
        @NotNull Map<Location, Transform> transforms,
        boolean shade
    ) {
        this.cubes = Collections.unmodifiableList(cubes);
        this.textures = Collections.unmodifiableMap(textures);
        this.transforms = Collections.unmodifiableMap(transforms);
        this.shade = shade;
    }

    public Model(
            @NotNull List<Cube> cubes,
            @NotNull Map<String, Location> textures,
            @NotNull Map<Location, Transform> transforms
    ) {
        this(cubes, textures, transforms, true);
    }

    public @Nullable Transform getTransform(@NotNull Location location) {
        return transforms.get(location);
    }

    public static @NotNull Model full(@NotNull Nexo nexo, @NotNull Location texture) {
        return new Model(List.of(
                new Cube(0, 0, 0, 16, 16, 16, Map.of(
                        Orientation.UP, Face.simple("all"),
                        Orientation.DOWN, Face.simple("all"),
                        Orientation.NORTH, Face.simple("all"),
                        Orientation.SOUTH, Face.simple("all"),
                        Orientation.EAST, Face.simple("all"),
                        Orientation.WEST, Face.simple("all")
                ))
        ), Map.of("all", texture), Map.of(
            Location.of("minecraft", "gui"),                   new Transform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0),   new Vector3f(0.625f, 0.625f, 0.625f)),
            Location.of("minecraft", "ground"),                new Transform(new Vector3f(0, 0, 0),    new Vector3f(0, 3, 0),   new Vector3f(0.25f, 0.25f, 0.25f)),
            Location.of("minecraft", "fixed"),                 new Transform(new Vector3f(0, 0, 0),    new Vector3f(0, 0, 0),   new Vector3f(0.5f, 0.5f, 0.5f)),
            Location.of("minecraft", "thirdperson_righthand"), new Transform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
            Location.of("minecraft", "firstperson_righthand"), new Transform(new Vector3f(0, 45, 0),  new Vector3f(0, 0, 0),   new Vector3f(0.4f, 0.4f, 0.4f)),
            Location.of("minecraft", "firstperson_lefthand"),  new Transform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0),   new Vector3f(0.4f, 0.4f, 0.4f))
        ));
    }

    public static @Nullable Model load(@NotNull Nexo nexo, @NotNull Location path) {
        byte[] data = nexo.loadResource(path);
        if (data == null) return null;
        try {
            return load(nexo, path, data);
        } catch (Exception e) {
            nexo.getLogger().error("Failed to parse model: {}", path, e);
            return null;
        }
    }

    public static @Nullable Model load(@NotNull Nexo nexo, @NotNull Location path, @NotNull byte[] data) {
        for (ModelLoader loader : LOADERS) {
            Model result = loader.tryLoad(nexo, path, data);
            if (result != null) return result;
        }
        return null;
    }

    public record Transform(
        @NotNull Vector3f rotation,
        @NotNull Vector3f translation,
        @NotNull Vector3f scale
    ) {}

    static {
        Model.registerLoader(new MinecraftModelLoader());
    }

    public static void registerLoader(@NotNull ModelLoader loader) {
        LOADERS.add(loader);
    }

}
