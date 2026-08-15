package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AbstractMinecraftGraphics2D implements Graphics2D {

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
        requireOutsidePrimitive("change the matrix");
        matrixTranslate(x, y, 0.0F);
    }

    @Override
    public void rotate(float angle) {
        requireOutsidePrimitive("change the matrix");
        matrixRotate(angle, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public void scale(float x, float y) {
        requireOutsidePrimitive("change the matrix");
        matrixScale(x, y, 1.0F);
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
    public void drawLine(float x1, float y1, float x2, float y2) {
        strokePolyline(new float[][]{{x1, y1}, {x2, y2}}, false);
    }

    /**
     * Strokes a polyline as filled quads honoring {@link #lineWidth(float)}.
     * Closed loops get mitered joins at every vertex, open strips get butt caps.
     * Minecraft ignores {@code RenderSystem.lineWidth} on modern renderers, so wide lines
     * must be expanded into geometry instead of relying on GL line width.
     */
    protected void strokePolyline(float @NotNull [] @NotNull [] points, boolean closed) {
        int count = points.length;
        if (count < 2) return;
        float half = state.lineWidth * 0.5F;
        int segmentCount = closed ? count : count - 1;

        // Unit normals of every segment, perpendicular to its direction
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

        // Mitered offset per vertex, capped at four times the half width
        float[] ox = new float[count];
        float[] oy = new float[count];
        for (int j = 0; j < count; j++) {
            int prev = closed ? (j - 1 + count) % count : j - 1;
            boolean hasPrev = prev >= 0;
            boolean hasNext = closed ? true : j < segmentCount;
            if (!hasPrev && !hasNext) continue;
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
                // Reversed direction: fall back to a single side
                float n = hasPrev ? pxn : qxn;
                float m = hasPrev ? pyn : qyn;
                ox[j] = n * half;
                oy[j] = m * half;
            }
        }

        begin(PrimitiveType.QUADS, VertexFormat.POSITION);
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

    @Override
    public void textureBounds(float minX, float minY, float maxX, float maxY) {
        state.textureBounds = new float[]{minX, minY, maxX, maxY};
    }

    @Override
    public void resetTextureBounds() {
        state.textureBounds = null;
    }

    protected void beginShape(@NotNull PrimitiveType type) {
        begin(type, state.texture != null ? VertexFormat.POSITION_TEX : VertexFormat.POSITION);
    }

    protected void shapeVertex(float x, float y, float minX, float minY, float maxX, float maxY) {
        if (state.texture == null) {
            vertex(x, y, 0.0F);
            return;
        }
        float[] bounds = state.textureBounds;
        if (bounds != null) {
            minX = bounds[0];
            minY = bounds[1];
            maxX = bounds[2];
            maxY = bounds[3];
        }
        float u = maxX > minX ? (x - minX) / (maxX - minX) : 0.0F;
        float v = maxY > minY ? (y - minY) / (maxY - minY) : 0.0F;
        vertex(x, y, 0.0F, u, v);
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



    public abstract void begin(@NotNull PrimitiveType type, @NotNull VertexFormat format);

    public abstract void vertex(float @NotNull ... data);

    public abstract void end();


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
        protected float @Nullable [] textureBounds;
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
            textureBounds = other.textureBounds != null ? other.textureBounds.clone() : null;
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
