package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.render.util.DepthMode;
import dev.lucaargolo.nexo.api.render.util.PrimitiveType;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public interface Graphics3D extends Graphics2D {

    void translate(float x, float y, float z);
    void rotate(float angle, float axisX, float axisY, float axisZ);
    void rotate(@NotNull Vector3f axis, float angle);
    void scale(float x, float y, float z);

    /**
     * Returns the camera position in the current local coordinate system.
     */
    @NotNull Vector3f cameraPosition();

    void depthMode(@NotNull DepthMode mode);
    @NotNull DepthMode depthMode();
    void depthMask(boolean write);

    void cullMode(@NotNull CullMode mode);
    @NotNull CullMode cullMode();

    void lightmap(float u, float v);
    void normal(float nx, float ny, float nz);
    void normal(@NotNull Vector3f normal);

    void drawLine(float x1, float y1, float z1, float x2, float y2, float z2);
    void drawCube(float x, float y, float z, float sizeX, float sizeY, float sizeZ);
    void drawQuad(@NotNull Vector3f v0, @NotNull Vector3f v1,
                  @NotNull Vector3f v2, @NotNull Vector3f v3);

    void begin(@NotNull PrimitiveType type, @NotNull VertexFormat format);
    void vertex(float @NotNull ... data);
    void end();

    void perspective(float fov, float aspect, float near, float far);
    void ortho(float left, float right, float bottom, float top, float near, float far);
    void lookAt(@NotNull Vector3f eye, @NotNull Vector3f center, @NotNull Vector3f up);
}
