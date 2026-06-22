package dev.lucaargolo.nexo.api.model;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.model.loader.ModelLoader;
import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private static final List<ModelLoader> LOADERS = new CopyOnWriteArrayList<>();

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

    @Override
    public @NotNull Map<String, Location> textures() {
        return textures;
    }

    @Override
    public @NotNull List<Cube> cubes() {
        return cubes;
    }

    public @Nullable Transform getTransform(@NotNull Location location) {
        return transforms.get(location);
    }

    public static void registerLoader(@NotNull ModelLoader loader) {
        LOADERS.add(loader);
    }

    @NotNull
    public static Model full(Nexo nexo, @NotNull Location texture) {
        return new Model(List.of(
                new Cube(0, 0, 0, 16, 16, 16, Map.of(
                        Orientation.UP, Face.simple("all"),
                        Orientation.DOWN, Face.simple("all"),
                        Orientation.NORTH, Face.simple("all"),
                        Orientation.SOUTH, Face.simple("all"),
                        Orientation.EAST, Face.simple("all"),
                        Orientation.WEST, Face.simple("all")
                ))
        ), Map.of("all", texture), Map.of());
    }

    @Nullable
    public static Model load(Nexo nexo, @NotNull Location path) {
        try {
            //TODO: This needs to check if its a nexo file or a minecraft file, kinda like Nexo.loadModel does but for getting resources. I think we can actually remove the load logic (first nexo then minecrafT= from there and bring it here and only keep the actual resource loading (reading the actual files and returning the byte data) in there.
            byte[] data = Files.readAllBytes(Paths.get(""));
            return load(nexo, path, data);
        } catch (IOException e) {
            nexo.getLogger().error("Failed to read model: {}", path, e);
        } catch (Exception e) {
            nexo.getLogger().error("Failed to parse model: {}", path, e);
        }
        nexo.getLogger().warn("No loader could handle: {}", path);
        return null;
    }

    @Nullable
    public static Model load(Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
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

}
