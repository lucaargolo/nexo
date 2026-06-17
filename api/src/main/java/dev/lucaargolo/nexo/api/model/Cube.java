package dev.lucaargolo.nexo.api.model;

import dev.lucaargolo.nexo.api.util.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public record Cube(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, @NotNull Map<Direction, Face> faces) {

    public Cube(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, @NotNull Map<Direction, Face> faces) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
        this.faces = Collections.unmodifiableMap(faces);
    }
}
