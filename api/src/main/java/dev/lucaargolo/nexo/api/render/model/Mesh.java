package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable render-ready geometry. Vertex data is interleaved as position
 * (3), color (4), texture coordinate (2), and normal (3).
 */
public final class Mesh {

    public static final int VERTEX_STRIDE = 12;

    private final @NotNull PrimitiveType primitiveType;
    private final @NotNull String material;
    private final float @NotNull [] vertices;

    public Mesh(
            @NotNull PrimitiveType primitiveType,
            @NotNull String material,
            float @NotNull [] vertices
    ) {
        this.primitiveType = Objects.requireNonNull(primitiveType, "primitiveType");
        this.material = Objects.requireNonNull(material, "material");
        Objects.requireNonNull(vertices, "vertices");
        if (vertices.length == 0 || vertices.length % VERTEX_STRIDE != 0) {
            throw new IllegalArgumentException(
                    "Mesh data must contain a non-empty multiple of " + VERTEX_STRIDE + " floats"
            );
        }
        validateVertexCount(primitiveType, vertices.length / VERTEX_STRIDE);
        this.vertices = vertices.clone();
    }

    public @NotNull PrimitiveType primitiveType() {
        return primitiveType;
    }

    public @NotNull String material() {
        return material;
    }

    public int vertexCount() {
        return vertices.length / VERTEX_STRIDE;
    }

    public float @NotNull [] vertices() {
        return vertices.clone();
    }

    float @NotNull [] vertexData() {
        return vertices;
    }

    private static void validateVertexCount(@NotNull PrimitiveType type, int count) {
        switch (type) {
            case QUADS -> requireMultiple(type, count, 4);
            case TRIANGLES -> requireMultiple(type, count, 3);
            case LINES -> requireMultiple(type, count, 2);
            case TRIANGLE_STRIP, TRIANGLE_FAN -> requireMinimum(type, count, 3);
            case LINE_STRIP, LINE_LOOP -> requireMinimum(type, count, 2);
            case POINTS -> requireMinimum(type, count, 1);
        }
    }

    private static void requireMultiple(@NotNull PrimitiveType type, int count, int multiple) {
        if (count % multiple != 0) {
            throw new IllegalArgumentException(type + " needs a multiple of " + multiple + " vertices");
        }
    }

    private static void requireMinimum(@NotNull PrimitiveType type, int count, int minimum) {
        if (count < minimum) {
            throw new IllegalArgumentException(type + " needs at least " + minimum + " vertices");
        }
    }
}
