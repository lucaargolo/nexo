package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AbstractMinecraftGraphics3D implements Graphics3D {

    protected final Deque<State> states = new ArrayDeque<>();
    protected State state = new State();
    protected @Nullable PrimitiveType primitive;
    protected @Nullable VertexFormat format;


    @Override
    public void pushState() {
        requireOutsidePrimitive("change render state");
        states.push(new State(state));
    }

    @Override
    public void popState() {
        requireOutsidePrimitive("change render state");
        if (states.isEmpty()) throw new IllegalStateException("Cannot pop an empty render-state stack");
        state = states.pop();
    }


    @Override
    public void translate(float x, float y) {
        translate(x, y, 0.0F);
    }

    @Override
    public void translate(float x, float y, float z) {
        requireOutsidePrimitive("change the matrix");
        matrixTranslate(x, y, z);
    }

    @Override
    public void rotate(float angle) {
        rotate(angle, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public void rotate(float angle, float axisX, float axisY, float axisZ) {
        requireOutsidePrimitive("change the matrix");
        matrixRotate(angle, axisX, axisY, axisZ);
    }

    @Override
    public void rotate(@NotNull Vector3f axis, float angle) {
        rotate(angle, axis.x(), axis.y(), axis.z());
    }

    @Override
    public void scale(float x, float y) {
        scale(x, y, 1.0F);
    }

    @Override
    public void scale(float x, float y, float z) {
        requireOutsidePrimitive("change the matrix");
        matrixScale(x, y, z);
    }

    @Override
    public void mulMatrix(@NotNull Matrix4f matrix) {
        requireOutsidePrimitive("change the matrix");
        matrixMul(matrix);
    }

    @Override
    public @NotNull Matrix4f matrix() {
        return matrixGet();
    }

    protected abstract void matrixTranslate(float x, float y, float z);
    protected abstract void matrixRotate(float angle, float axisX, float axisY, float axisZ);
    protected abstract void matrixScale(float x, float y, float z);
    protected abstract void matrixMul(@NotNull Matrix4f matrix);
    protected abstract @NotNull Matrix4f matrixGet();


    @Override
    public void color(float r, float g, float b, float a) {
        requireOutsidePrimitive("change render state");
        state.color = new float[]{r, g, b, a};
    }

    @Override
    public void color(float @NotNull [] rgba) {
        if (rgba.length != 4) throw new IllegalArgumentException("A color requires exactly four components");
        color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    @Override
    public float @NotNull [] color() {
        return state.color.clone();
    }


    @Override
    public void blendMode(@NotNull BlendMode mode) {
        requireOutsidePrimitive("change render state");
        state.blendMode = mode;
    }

    @Override
    public @NotNull BlendMode blendMode() {
        return state.blendMode;
    }


    @Override
    public void lineWidth(float width) {
        requireOutsidePrimitive("change render state");
        if (width <= 0.0F) throw new IllegalArgumentException("Line width must be positive");
        state.lineWidth = width;
    }

    @Override
    public float lineWidth() {
        return state.lineWidth;
    }


    @Override
    public void bindTexture(@NotNull Location texture) {
        requireOutsidePrimitive("bind a texture");
        state.texture = texture;
    }

    @Override
    public void textureFilter(@NotNull TextureFilter min, @NotNull TextureFilter mag) {
        requireOutsidePrimitive("change texture filtering");
        state.minFilter = min;
        state.magFilter = mag;
    }

    @Override
    public void textureWrap(@NotNull TextureWrap wrapS, @NotNull TextureWrap wrapT) {
        requireOutsidePrimitive("change texture wrapping");
        if (wrapS != TextureWrap.CLAMP || wrapT != TextureWrap.CLAMP) {
            throw unsupported("texture wrapping other than CLAMP");
        }
        state.wrapS = wrapS;
        state.wrapT = wrapT;
    }


    @Override
    public void font(@Nullable Location font) {
        requireOutsidePrimitive("change the font");
        state.font = font;
    }

    @Override
    public @Nullable Location font() {
        return state.font;
    }

    @Override
    public void fontSize(float size) {
        requireOutsidePrimitive("change the font size");
        if (size <= 0.0F) throw new IllegalArgumentException("Font size must be positive");
        state.fontSize = size;
    }

    @Override
    public float fontSize() {
        return state.fontSize;
    }


    @Override
    public void depthMode(@NotNull DepthMode mode) {
        requireOutsidePrimitive("change render state");
        state.depthMode = mode;
    }

    @Override
    public @NotNull DepthMode depthMode() {
        return state.depthMode;
    }

    @Override
    public void depthMask(boolean write) {
        requireOutsidePrimitive("change render state");
        state.depthMask = write;
    }


    @Override
    public void cullMode(@NotNull CullMode mode) {
        requireOutsidePrimitive("change render state");
        state.cullMode = mode;
    }

    @Override
    public @NotNull CullMode cullMode() {
        return state.cullMode;
    }


    @Override
    public void lightmap(float u, float v) {
        requireOutsidePrimitive("change render state");
        state.customLight = true;
        state.lightU = u;
        state.lightV = v;
    }

    @Override
    public void normal(float nx, float ny, float nz) {
        requireOutsidePrimitive("change render state");
        state.normal = new Vector3f(nx, ny, nz);
    }

    @Override
    public void normal(@NotNull Vector3f normal) {
        normal(normal.x(), normal.y(), normal.z());
    }


    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        drawLine(x1, y1, 0.0F, x2, y2, 0.0F);
    }

    @Override
    public void drawCircle(float x, float y, float radius) {
        drawEllipse(x, y, radius * 2.0F, radius * 2.0F);
    }

    @Override
    public void fillCircle(float x, float y, float radius) {
        fillEllipse(x, y, radius * 2.0F, radius * 2.0F);
    }

    @Override
    public void drawTexture(float x, float y, float width, float height) {
        drawTextureRegion(x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    @Override
    public void drawTextureRegion(
            float x,
            float y,
            float width,
            float height,
            float u0,
            float v0,
            float u1,
            float v1
    ) {
        if (state.texture == null) throw new IllegalStateException("Cannot draw a texture before binding one");
        begin(PrimitiveType.QUADS, VertexFormat.POSITION_TEX_NORMAL);
        vertex(x, y + height, 0.0F, u0, v1, 0.0F, 0.0F, 1.0F);
        vertex(x + width, y + height, 0.0F, u1, v1, 0.0F, 0.0F, 1.0F);
        vertex(x + width, y, 0.0F, u1, v0, 0.0F, 0.0F, 1.0F);
        vertex(x, y, 0.0F, u0, v0, 0.0F, 0.0F, 1.0F);
        end();
    }

    @Override
    public void drawCube(float x, float y, float z, float sizeX, float sizeY, float sizeZ) {
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
    public void drawQuad(
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
    public void clip(float x, float y, float width, float height) {
        throw unsupported("clip regions");
    }

    @Override
    public void disableClip() {
    }

    @Override
    public void scissor(int x, int y, int width, int height) {
        throw unsupported("scissor regions");
    }

    @Override
    public void disableScissor() {
    }


    @Override
    public void perspective(float fov, float aspect, float near, float far) {
        mulMatrix(new Matrix4f().perspective((float) Math.toRadians(fov), aspect, near, far));
    }

    @Override
    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        mulMatrix(new Matrix4f().ortho(left, right, bottom, top, near, far));
    }

    @Override
    public void lookAt(@NotNull Vector3f eye, @NotNull Vector3f center, @NotNull Vector3f up) {
        mulMatrix(new Matrix4f().lookAt(eye, center, up));
    }


    protected void requireOutsidePrimitive(@NotNull String operation) {
        if (primitive != null) throw new IllegalStateException("Cannot " + operation + " inside begin/end");
    }

    protected static int channel(float value) {
        return Math.round(Math.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

    protected static @NotNull UnsupportedOperationException unsupported(@NotNull String operation) {
        return new UnsupportedOperationException("Minecraft rendering does not support " + operation);
    }


    protected static class State {
        protected float[] color = {1.0F, 1.0F, 1.0F, 1.0F};
        protected BlendMode blendMode = BlendMode.DISABLED;
        protected float lineWidth = 1.0F;
        protected @Nullable Location texture;
        protected @Nullable Object sprite;
        protected TextureFilter minFilter = TextureFilter.NEAREST;
        protected TextureFilter magFilter = TextureFilter.NEAREST;
        protected TextureWrap wrapS = TextureWrap.CLAMP;
        protected TextureWrap wrapT = TextureWrap.CLAMP;
        protected @Nullable Location font;
        protected float fontSize = 9.0F;
        protected DepthMode depthMode = DepthMode.ENABLED;
        protected boolean depthMask = true;
        protected CullMode cullMode = CullMode.BACK;
        protected @Nullable Shader shader;
        protected boolean customLight;
        protected float lightU;
        protected float lightV;
        protected Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

        protected State() {
        }

        protected State(@NotNull State other) {
            color = other.color.clone();
            blendMode = other.blendMode;
            lineWidth = other.lineWidth;
            texture = other.texture;
            sprite = other.sprite;
            minFilter = other.minFilter;
            magFilter = other.magFilter;
            wrapS = other.wrapS;
            wrapT = other.wrapT;
            font = other.font;
            fontSize = other.fontSize;
            depthMode = other.depthMode;
            depthMask = other.depthMask;
            cullMode = other.cullMode;
            shader = other.shader;
            customLight = other.customLight;
            lightU = other.lightU;
            lightV = other.lightV;
            normal = new Vector3f(other.normal);
        }
    }
}
