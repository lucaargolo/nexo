package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface MinecraftGraphics2D extends Graphics2D {

    int NO_LIGHT_OVERRIDE = Integer.MIN_VALUE;

    State state();
    PrimitiveType primitive();

    @Override
    default void translate(float x, float y) {
        requireOutsidePrimitive("change the matrix");
        matrixTranslate(x, y, 0.0F);
    }

    @Override
    default void rotate(float angle) {
        requireOutsidePrimitive("change the matrix");
        matrixRotate(angle, 0.0F, 0.0F, 1.0F);
    }

    @Override
    default void scale(float x, float y) {
        requireOutsidePrimitive("change the matrix");
        matrixScale(x, y, 1.0F);
    }

    @Override
    default void mulMatrix(@NotNull Matrix4f matrix) {
        requireOutsidePrimitive("change the matrix");
        matrixMul(matrix);
    }

    @Override
    default @NotNull Matrix4f matrix() {
        return matrixGet();
    }

    void matrixTranslate(float x, float y, float z);
    void matrixRotate(float angle, float axisX, float axisY, float axisZ);
    void matrixScale(float x, float y, float z);
    void matrixMul(@NotNull Matrix4f matrix);
    @NotNull Matrix4f matrixGet();

    @Override
    default void color(float r, float g, float b, float a) {
        requireOutsidePrimitive("change render state");
        state().color[0] = r;
        state().color[1] = g;
        state().color[2] = b;
        state().color[3] = a;
    }

    @Override
    default void color(float @NotNull [] rgba) {
        if (rgba.length != 4) {
            throw new IllegalArgumentException("A color requires exactly four components");
        }
        color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    @Override
    default float @NotNull [] color() {
        return state().color.clone();
    }


    @Override
    default void lineWidth(float width) {
        requireOutsidePrimitive("change render state");
        if (width <= 0.0F) {
            throw new IllegalArgumentException("Line width must be positive");
        }
        state().width = width;
    }

    @Override
    default float lineWidth() {
        return state().width;
    }

    @Override
    default @Nullable Material<?> material() {
        return state().material;
    }

    @Override
    default void drawLine(float x1, float y1, float x2, float y2) {
        strokePolyline(new float[][]{{x1, y1}, {x2, y2}}, false);
    }

    default void strokePolyline(float @NotNull [] @NotNull [] points, boolean closed) {
        int count = points.length;
        if (count < 2) {
            return;
        }
        float half = state().width * 0.5F;
        int segmentCount = closed ? count : count - 1;

        float[] nx = new float[segmentCount];
        float[] ny = new float[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            float[] a = points[i];
            float[] b = points[(i + 1) % count];
            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length > 1.0E-5F) {
                nx[i] = -dy / length;
                ny[i] = dx / length;
            }
        }

        float[] ox = new float[count];
        float[] oy = new float[count];
        for (int j = 0; j < count; j++) {
            int prev = closed ? (j - 1 + count) % count : j - 1;
            boolean hasPrev = prev >= 0;
            boolean hasNext = closed ? true : j < segmentCount;
            if (!hasPrev && !hasNext) {
                continue;
            }
            float pxn = hasPrev ? nx[prev] : 0.0F;
            float pyn = hasPrev ? ny[prev] : 0.0F;
            float qxn = hasNext ? nx[j] : 0.0F;
            float qyn = hasNext ? ny[j] : 0.0F;
            float sx = pxn + qxn;
            float sy = pyn + qyn;
            float slen = (float) Math.sqrt(sx * sx + sy * sy);
            if (slen > 1.0E-5F) {
                float bx = sx / slen;
                float by = sy / slen;
                float dot = Math.max(bx * (hasPrev ? pxn : qxn) + by * (hasPrev ? pyn : qyn), 0.25F);
                float scale = half / dot;
                ox[j] = bx * scale;
                oy[j] = by * scale;
            } else {
                float n = hasPrev ? pxn : qxn;
                float m = hasPrev ? pyn : qyn;
                ox[j] = n * half;
                oy[j] = m * half;
            }
        }

        begin(PrimitiveType.QUADS, VertexLayout.POSITION);
        for (int i = 0; i < segmentCount; i++) {
            float[] a = points[i];
            float[] b = points[(i + 1) % count];
            float ax = a[0] + ox[i];
            float ay = a[1] + oy[i];
            float bx0 = a[0] - ox[i];
            float by0 = a[1] - oy[i];
            float cx = b[0] + ox[(i + 1) % count];
            float cy = b[1] + oy[(i + 1) % count];
            float dx0 = b[0] - ox[(i + 1) % count];
            float dy0 = b[1] - oy[(i + 1) % count];
            vertex(ax, ay, 0.0F);
            vertex(bx0, by0, 0.0F);
            vertex(dx0, dy0, 0.0F);
            vertex(cx, cy, 0.0F);
        }
        end();
    }

    default void beginShape(@NotNull PrimitiveType type) {
        begin(type, texture() != null ? VertexLayout.POSITION_TEX : VertexLayout.POSITION);
    }

    default void shapeVertex(float x, float y, float minX, float minY, float maxX, float maxY) {
        if (texture() == null) {
            vertex(x, y, 0.0F);
            return;
        }
        float u = maxX > minX ? (x - minX) / (maxX - minX) : 0.0F;
        float v = maxY > minY ? (y - minY) / (maxY - minY) : 0.0F;
        vertex(x, y, 0.0F, u, v);
    }

    @Override
    default void drawCircle(float x, float y, float radius) {
        drawEllipse(x, y, radius * 2.0F, radius * 2.0F);
    }

    @Override
    default void fillCircle(float x, float y, float radius) {
        fillEllipse(x, y, radius * 2.0F, radius * 2.0F);
    }

    @Override
    default void clip(float x, float y, float width, float height) {
        throw unsupported("clip regions");
    }

    @Override
    default void disableClip() {
    }

    @Override
    default void scissor(int x, int y, int width, int height) {
        throw unsupported("scissor regions");
    }

    @Override
    default void disableScissor() {
    }

    void begin(@NotNull PrimitiveType type, @NotNull VertexLayout format);

    void vertex(float @NotNull ... data);

    void end();

    default @Nullable Location texture() {
        return state().material != null ? state().material.location() : null;
    }

    default @Nullable Shader shader() {
        return state().material != null ? state().material.shader() : null;
    }

    default @NotNull BlendMode blendMode() {
        return state().material != null ? state().material.blendMode() : BlendMode.DISABLED;
    }

    default @NotNull CullMode cullMode() {
        return state().material != null ? state().material.cullMode() : defaultCullMode();
    }

    default @NotNull CullMode defaultCullMode() {
        return CullMode.BACK;
    }

    default void requireOutsidePrimitive(@NotNull String operation) {
        if (primitive() != null) {
            throw new IllegalStateException("Cannot " + operation + " inside begin/end");
        }
    }

    static @NotNull UnsupportedOperationException unsupported(@NotNull String operation) {
        return new UnsupportedOperationException("Minecraft rendering does not support " + operation);
    }

    final class State {

        @Nullable Material<?> material;
        float[] color = {1.0F, 1.0F, 1.0F, 1.0F};
        float width = 1.0F;

        DepthMode depth = DepthMode.ENABLED;
        int light = NO_LIGHT_OVERRIDE;

        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

        State() {
        }

        State(@NotNull State other) {
            material = other.material;
            color = other.color.clone();
            width = other.width;
            depth = other.depth;
            light = other.light;
            normal = new Vector3f(other.normal);
        }

    }
}
