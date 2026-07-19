package dev.lucaargolo.nexo.api.render.model.loader;

import dev.lucaargolo.nexo.api.render.model.Mesh;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

final class FloatBuilder {

    private float @NotNull [] values = new float[Mesh.VERTEX_STRIDE * 8];
    private int size;

    void add(
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v,
            float normalX, float normalY, float normalZ
    ) {
        ensureCapacity(Mesh.VERTEX_STRIDE);
        values[size++] = x;
        values[size++] = y;
        values[size++] = z;
        values[size++] = red;
        values[size++] = green;
        values[size++] = blue;
        values[size++] = alpha;
        values[size++] = u;
        values[size++] = v;
        values[size++] = normalX;
        values[size++] = normalY;
        values[size++] = normalZ;
    }

    private void ensureCapacity(int addition) {
        if (size + addition > values.length) {
            values = Arrays.copyOf(values, Math.max(values.length * 2, size + addition));
        }
    }

    float @NotNull [] toArray() {
        return Arrays.copyOf(values, size);
    }
}
