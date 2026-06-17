package dev.lucaargolo.nexo.api.model;

import dev.lucaargolo.nexo.api.Location;
import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record Model(@NotNull Map<String, Location> textures, @NotNull List<Cube> cubes, boolean ambientOcclusion) {

    public Model(@NotNull Map<String, Location> textures, @NotNull List<Cube> cubes, boolean ambientOcclusion) {
        this.textures = Collections.unmodifiableMap(textures);
        this.cubes = Collections.unmodifiableList(cubes);
        this.ambientOcclusion = ambientOcclusion;
    }

    @Override
    public @NotNull Map<String, Location> textures() {
        return textures;
    }

    @Override
    public @NotNull List<Cube> cubes() {
        return cubes;
    }

    public static Model full(@NotNull Location texture) {
        return new Model(Map.of("all", texture), List.of(
                new Cube(0, 0, 0, 16, 16, 16, Map.of(
                        Orientation.UP, Face.simple("all"),
                        Orientation.DOWN, Face.simple("all"),
                        Orientation.NORTH, Face.simple("all"),
                        Orientation.SOUTH, Face.simple("all"),
                        Orientation.EAST, Face.simple("all"),
                        Orientation.WEST, Face.simple("all")
                ))
        ), true);
    }

}
