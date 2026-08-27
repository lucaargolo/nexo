package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.util.DepthMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface AbstractMinecraftGraphics3D extends AbstractMinecraftGraphics2D, Graphics3D {

    @Override
    default void translate(float x, float y, float z) {
        requireOutsidePrimitive("change the matrix");
        matrixTranslate(x, y, z);
    }

    @Override
    default void rotate(float angle, float axisX, float axisY, float axisZ) {
        requireOutsidePrimitive("change the matrix");
        matrixRotate(angle, axisX, axisY, axisZ);
    }

    @Override
    default void rotate(@NotNull Vector3f axis, float angle) {
        rotate(angle, axis.x(), axis.y(), axis.z());
    }

    @Override
    default void scale(float x, float y, float z) {
        requireOutsidePrimitive("change the matrix");
        matrixScale(x, y, z);
    }

    @Override
    default void depthMode(@NotNull DepthMode mode) {
        requireOutsidePrimitive("change render state");
        state().depthMode = mode;
    }

    @Override
    default @NotNull DepthMode depthMode() {
        return state().depthMode;
    }

    @Override
    default void lightmap(float u, float v) {
        requireOutsidePrimitive("change render state");
        state().light = ((int) u) | (((int) v) << 16);
    }

    @Override
    default void normal(float nx, float ny, float nz) {
        requireOutsidePrimitive("change render state");
        state().normal.set(nx, ny, nz);
    }

    @Override
    default void normal(@NotNull Vector3f normal) {
        normal(normal.x(), normal.y(), normal.z());
    }


    @Override
    default void drawCube(float x, float y, float z, float sizeX, float sizeY, float sizeZ) {
        Vector3f p000 = new Vector3f(x, y, z);
        Vector3f p001 = new Vector3f(x, y, z + sizeZ);
        Vector3f p010 = new Vector3f(x, y + sizeY, z);
        Vector3f p011 = new Vector3f(x, y + sizeY, z + sizeZ);
        Vector3f p100 = new Vector3f(x + sizeX, y, z);
        Vector3f p101 = new Vector3f(x + sizeX, y, z + sizeZ);
        Vector3f p110 = new Vector3f(x + sizeX, y + sizeY, z);
        Vector3f p111 = new Vector3f(x + sizeX, y + sizeY, z + sizeZ);
        drawQuad(p001, p000, p100, p101);
        drawQuad(p000, p010, p110, p100);
        drawQuad(p101, p100, p110, p111);
        drawQuad(p001, p011, p010, p000);
        drawQuad(p011, p111, p110, p010);
        drawQuad(p001, p101, p111, p011);
    }

    @Override
    default void drawQuad(
            @NotNull Vector3f v0,
            @NotNull Vector3f v1,
            @NotNull Vector3f v2,
            @NotNull Vector3f v3
    ) {
        Vector3f computedNormal = new Vector3f(v1).sub(v0).cross(new Vector3f(v2).sub(v0)).normalize();
        begin(PrimitiveType.QUADS, VertexFormat.POSITION_TEX_NORMAL);
        vertex(v0.x(), v0.y(), v0.z(), 0.0F, 1.0F, computedNormal.x(), computedNormal.y(), computedNormal.z());
        vertex(v1.x(), v1.y(), v1.z(), 0.0F, 0.0F, computedNormal.x(), computedNormal.y(), computedNormal.z());
        vertex(v2.x(), v2.y(), v2.z(), 1.0F, 0.0F, computedNormal.x(), computedNormal.y(), computedNormal.z());
        vertex(v3.x(), v3.y(), v3.z(), 1.0F, 1.0F, computedNormal.x(), computedNormal.y(), computedNormal.z());
        end();
    }


    @Override
    default void perspective(float fov, float aspect, float near, float far) {
        mulMatrix(new Matrix4f().perspective((float) Math.toRadians(fov), aspect, near, far));
    }

    @Override
    default void ortho(float left, float right, float bottom, float top, float near, float far) {
        mulMatrix(new Matrix4f().ortho(left, right, bottom, top, near, far));
    }

    @Override
    default void lookAt(@NotNull Vector3f eye, @NotNull Vector3f center, @NotNull Vector3f up) {
        mulMatrix(new Matrix4f().lookAt(eye, center, up));
    }

}
