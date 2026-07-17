package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.StaticRenderer;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

public final class MinecraftBakedGraphics3D implements Graphics3D {

    private static final Location MISSING_TEXTURE = Location.of("minecraft", "missingno");

    private final Function<Material, TextureAtlasSprite> textureGetter;
    private final Deque<Matrix4f> matrices = new ArrayDeque<>();
    private final Deque<State> states = new ArrayDeque<>();
    private final List<BakedQuad> quads = new ArrayList<>();
    private final List<Vertex> vertices = new ArrayList<>();

    private Matrix4f matrix = new Matrix4f();
    private State state = new State();
    private @Nullable PrimitiveType primitive;
    private @Nullable VertexFormat format;
    private @Nullable TextureAtlasSprite particle;

    private MinecraftBakedGraphics3D(@Nullable Function<Material, TextureAtlasSprite> textureGetter) {
        this.textureGetter = textureGetter;
    }

    public static <U> @NotNull MinecraftBakedGraphics3D bake(
            @NotNull StaticRenderer<Graphics3D, U> renderer,
            @NotNull U unit,
            @NotNull Function<Material, TextureAtlasSprite> textureGetter,
            @NotNull Matrix4f modelTransform
    ) {
        MinecraftBakedGraphics3D graphics = new MinecraftBakedGraphics3D(textureGetter);
        graphics.matrix.translate(0.5F, 0.5F, 0.5F)
                .mul(modelTransform)
                .translate(-0.5F, -0.5F, -0.5F);
        renderer.render(graphics, unit);
        graphics.requireComplete();
        return graphics;
    }

    @NotNull
    public List<BakedQuad> quads() {
        return List.copyOf(quads);
    }

    @Nullable
    public TextureAtlasSprite particle() {
        return particle;
    }

    private void requireComplete() {
        if (primitive != null) {
            throw new IllegalStateException("Renderer ended with an open " + primitive + " primitive");
        }
        if (!matrices.isEmpty()) {
            throw new IllegalStateException("Renderer ended with " + matrices.size() + " unclosed matrix states");
        }
        if (!states.isEmpty()) {
            throw new IllegalStateException("Renderer ended with " + states.size() + " unclosed render states");
        }
    }

    @Override
    public void pushMatrix() {
        matrices.push(new Matrix4f(matrix));
    }

    @Override
    public void popMatrix() {
        if (matrices.isEmpty()) throw new IllegalStateException("Cannot pop an empty matrix stack");
        matrix = matrices.pop();
    }

    @Override
    public void pushState() {
        states.push(new State(state));
    }

    @Override
    public void popState() {
        if (states.isEmpty()) throw new IllegalStateException("Cannot pop an empty render-state stack");
        state = states.pop();
    }

    @Override
    public void translate(float x, float y) {
        translate(x, y, 0.0F);
    }

    @Override
    public void translate(float x, float y, float z) {
        matrix.translate(x, y, z);
    }

    @Override
    public void rotate(float angle) {
        rotate(angle, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public void rotate(float angle, float axisX, float axisY, float axisZ) {
        matrix.rotate((float) Math.toRadians(angle), axisX, axisY, axisZ);
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
        matrix.scale(x, y, z);
    }

    @Override
    public void mulMatrix(@NotNull Matrix4f matrix) {
        this.matrix.mul(matrix);
    }

    @Override
    public @NotNull Matrix4f matrix() {
        return new Matrix4f(matrix);
    }

    @Override
    public void color(float r, float g, float b, float a) {
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
        if (mode != BlendMode.DISABLED) throw unsupported("blend mode " + mode);
        state.blendMode = mode;
    }

    @Override
    public @NotNull BlendMode blendMode() {
        return state.blendMode;
    }

    @Override
    public void lineWidth(float width) {
        if (width != 1.0F) throw unsupported("line width");
        state.lineWidth = width;
    }

    @Override
    public float lineWidth() {
        return state.lineWidth;
    }

    @Override
    public void clip(float x, float y, float width, float height) {
        throw unsupported("clip");
    }

    @Override
    public void disableClip() {
    }

    @Override
    public void scissor(int x, int y, int width, int height) {
        throw unsupported("scissor");
    }

    @Override
    public void disableScissor() {
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        drawLine(x1, y1, 0.0F, x2, y2, 0.0F);
    }

    @Override
    public void drawRect(float x, float y, float width, float height) {
        throw unsupported("outlined rectangles");
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        drawTextureRegion(x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    @Override
    public void drawCircle(float x, float y, float radius) {
        throw unsupported("outlined circles");
    }

    @Override
    public void fillCircle(float x, float y, float radius) {
        throw unsupported("circles");
    }

    @Override
    public void drawEllipse(float x, float y, float width, float height) {
        throw unsupported("outlined ellipses");
    }

    @Override
    public void fillEllipse(float x, float y, float width, float height) {
        throw unsupported("ellipses");
    }

    @Override
    public void drawRoundedRect(float x, float y, float width, float height, float radius) {
        throw unsupported("outlined rounded rectangles");
    }

    @Override
    public void fillRoundedRect(float x, float y, float width, float height, float radius) {
        throw unsupported("rounded rectangles");
    }

    @Override
    public void drawPolygon(float @NotNull [] x, float @NotNull [] y) {
        throw unsupported("outlined polygons");
    }

    @Override
    public void fillPolygon(float @NotNull [] x, float @NotNull [] y) {
        if (x.length != y.length || x.length < 3) {
            throw new IllegalArgumentException("A polygon needs matching x/y arrays with at least three points");
        }
        begin(PrimitiveType.TRIANGLE_FAN, VertexFormat.POSITION_TEX);
        for (int i = 0; i < x.length; i++) vertex(x[i], y[i], 0.0F, 0.0F, 0.0F);
        end();
    }

    @Override
    public void drawArc(float x, float y, float radius, float startAngle, float endAngle) {
        throw unsupported("arcs");
    }

    @Override
    public void fillArc(float x, float y, float radius, float startAngle, float endAngle) {
        throw unsupported("arcs");
    }

    @Override
    public void bindTexture(@NotNull Location texture) {
        state.texture = texture;
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
        begin(PrimitiveType.QUADS, VertexFormat.POSITION_TEX_NORMAL);
        vertex(x, y + height, 0.0F, u0, v1, 0.0F, 0.0F, 1.0F);
        vertex(x + width, y + height, 0.0F, u1, v1, 0.0F, 0.0F, 1.0F);
        vertex(x + width, y, 0.0F, u1, v0, 0.0F, 0.0F, 1.0F);
        vertex(x, y, 0.0F, u0, v0, 0.0F, 0.0F, 1.0F);
        end();
    }

    @Override
    public void textureFilter(@NotNull TextureFilter min, @NotNull TextureFilter mag) {
        if (min != TextureFilter.NEAREST || mag != TextureFilter.NEAREST) {
            throw unsupported("texture filtering");
        }
        state.minFilter = min;
        state.magFilter = mag;
    }

    @Override
    public void textureWrap(@NotNull TextureWrap wrapS, @NotNull TextureWrap wrapT) {
        if (wrapS != TextureWrap.CLAMP || wrapT != TextureWrap.CLAMP) {
            throw unsupported("texture wrapping");
        }
        state.wrapS = wrapS;
        state.wrapT = wrapT;
    }

    @Override
    public void drawText(@NotNull String text, float x, float y) {
        throw unsupported("text");
    }

    @Override
    public void font(@Nullable Location font) {
        state.font = font;
    }

    @Override
    public @Nullable Location font() {
        return state.font;
    }

    @Override
    public void fontSize(float size) {
        state.fontSize = size;
    }

    @Override
    public float fontSize() {
        return state.fontSize;
    }

    @Override
    public void depthMode(@NotNull DepthMode mode) {
        if (mode != DepthMode.ENABLED) throw unsupported("depth mode " + mode);
        state.depthMode = mode;
    }

    @Override
    public @NotNull DepthMode depthMode() {
        return state.depthMode;
    }

    @Override
    public void depthMask(boolean write) {
        if (!write) throw unsupported("disabled depth writes");
        state.depthMask = write;
    }

    @Override
    public void cullMode(@NotNull CullMode mode) {
        if (mode != CullMode.BACK) throw unsupported("cull mode " + mode);
        state.cullMode = mode;
    }

    @Override
    public @NotNull CullMode cullMode() {
        return state.cullMode;
    }

    @Override
    public void lightmap(float u, float v) {
        if (u != 0.0F || v != 0.0F) throw unsupported("custom lightmap coordinates");
        state.lightU = u;
        state.lightV = v;
    }

    @Override
    public void normal(float nx, float ny, float nz) {
        state.normal = new Vector3f(nx, ny, nz);
    }

    @Override
    public void normal(@NotNull Vector3f normal) {
        state.normal = new Vector3f(normal);
    }

    @Override
    public void drawLine(float x1, float y1, float z1, float x2, float y2, float z2) {
        throw unsupported("lines");
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
    public void begin(@NotNull PrimitiveType type, @NotNull VertexFormat format) {
        if (primitive != null) throw new IllegalStateException("Cannot begin a primitive before ending " + primitive);
        if (type != PrimitiveType.QUADS
                && type != PrimitiveType.TRIANGLES
                && type != PrimitiveType.TRIANGLE_STRIP
                && type != PrimitiveType.TRIANGLE_FAN) {
            throw unsupported(type.name().toLowerCase());
        }
        primitive = type;
        this.format = format;
        vertices.clear();
    }

    @Override
    public void vertex(float @NotNull ... data) {
        if (primitive == null || format == null) throw new IllegalStateException("Cannot add a vertex outside begin/end");
        if (data.length != format.stride()) {
            throw new IllegalArgumentException(format + " needs " + format.stride() + " values, received " + data.length);
        }

        int colorOffset = -1;
        int textureOffset = -1;
        int normalOffset = -1;
        switch (format) {
            case POSITION -> {
            }
            case POSITION_COLOR -> colorOffset = 3;
            case POSITION_TEX -> textureOffset = 3;
            case POSITION_COLOR_TEX -> {
                colorOffset = 3;
                textureOffset = 7;
            }
            case POSITION_TEX_NORMAL -> {
                textureOffset = 3;
                normalOffset = 5;
            }
            case POSITION_COLOR_TEX_NORMAL -> {
                colorOffset = 3;
                textureOffset = 7;
                normalOffset = 9;
            }
        }

        Vector4f position = matrix.transform(new Vector4f(data[0], data[1], data[2], 1.0F));
        if (position.w() != 0.0F && position.w() != 1.0F) position.div(position.w());

        float[] color = colorOffset >= 0
                ? new float[]{data[colorOffset], data[colorOffset + 1], data[colorOffset + 2], data[colorOffset + 3]}
                : state.color.clone();
        float u = textureOffset >= 0 ? data[textureOffset] : 0.0F;
        float v = textureOffset >= 0 ? data[textureOffset + 1] : 0.0F;
        Vector3f normal = normalOffset >= 0
                ? new Vector3f(data[normalOffset], data[normalOffset + 1], data[normalOffset + 2])
                : new Vector3f(state.normal);
        if (normal.lengthSquared() > 0.0F) {
            new Matrix3f(matrix).invert().transpose().transform(normal).normalize();
        }
        vertices.add(new Vertex(new Vector3f(position.x(), position.y(), position.z()), color, u, v, normal));
    }

    @Override
    public void end() {
        if (primitive == null) throw new IllegalStateException("Cannot end without begin");
        PrimitiveType completed = primitive;
        primitive = null;
        format = null;

        if (textureGetter == null) {
            vertices.clear();
            return;
        }

        TextureAtlasSprite sprite = sprite(state.texture);
        if (particle == null) particle = sprite;
        switch (completed) {
            case QUADS -> {
                requireVertexMultiple(completed, 4);
                for (int i = 0; i < vertices.size(); i += 4) {
                    addQuad(sprite, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3));
                }
            }
            case TRIANGLES -> {
                requireVertexMultiple(completed, 3);
                for (int i = 0; i < vertices.size(); i += 3) {
                    addQuad(sprite, vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 2));
                }
            }
            case TRIANGLE_STRIP -> {
                if (vertices.size() < 3) throw new IllegalStateException("TRIANGLE_STRIP needs at least three vertices");
                for (int i = 0; i < vertices.size() - 2; i++) {
                    Vertex a = vertices.get(i);
                    Vertex b = vertices.get(i + 1);
                    Vertex c = vertices.get(i + 2);
                    if ((i & 1) == 0) addQuad(sprite, a, b, c, c);
                    else addQuad(sprite, b, a, c, c);
                }
            }
            case TRIANGLE_FAN -> {
                if (vertices.size() < 3) throw new IllegalStateException("TRIANGLE_FAN needs at least three vertices");
                Vertex center = vertices.getFirst();
                for (int i = 1; i < vertices.size() - 1; i++) {
                    addQuad(sprite, center, vertices.get(i), vertices.get(i + 1), vertices.get(i + 1));
                }
            }
            default -> throw unsupported(completed.name().toLowerCase());
        }
        vertices.clear();
    }

    @Override
    public void perspective(float fov, float aspect, float near, float far) {
        matrix.perspective((float) Math.toRadians(fov), aspect, near, far);
    }

    @Override
    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        matrix.ortho(left, right, bottom, top, near, far);
    }

    @Override
    public void lookAt(@NotNull Vector3f eye, @NotNull Vector3f center, @NotNull Vector3f up) {
        matrix.lookAt(eye, center, up);
    }

    private void requireVertexMultiple(@NotNull PrimitiveType type, int multiple) {
        if (vertices.isEmpty() || vertices.size() % multiple != 0) {
            throw new IllegalStateException(type + " needs a non-empty multiple of " + multiple + " vertices");
        }
    }

    private @NotNull TextureAtlasSprite sprite(@Nullable Location texture) {
        Location actual = texture != null ? texture : MISSING_TEXTURE;
        String path = actual.path();
        int dot = path.lastIndexOf('.');
        if (dot > -1) path = path.substring(0, dot);
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(actual.namespace(), path);
        Material material = new Material(InventoryMenu.BLOCK_ATLAS, location);
        assert textureGetter != null;
        return textureGetter.apply(material);
    }

    private void addQuad(
            @NotNull TextureAtlasSprite sprite,
            @NotNull Vertex v0,
            @NotNull Vertex v1,
            @NotNull Vertex v2,
            @NotNull Vertex v3
    ) {
        Vertex[] face = {v0, v1, v2, v3};
        Vector3f normal = averageNormal(face);
        Direction direction = Direction.getNearest(normal.x(), normal.y(), normal.z());
        int[] packed = new int[32];
        for (int i = 0; i < face.length; i++) packVertex(packed, i * 8, face[i], sprite, normal);
        quads.add(new BakedQuad(packed, -1, direction, sprite, true));
    }

    private static @NotNull Vector3f averageNormal(Vertex @NotNull [] vertices) {
        Vector3f normal = new Vector3f();
        for (Vertex vertex : vertices) normal.add(vertex.normal());
        if (normal.lengthSquared() <= 1.0E-8F) {
            normal.set(vertices[1].position()).sub(vertices[0].position())
                    .cross(new Vector3f(vertices[2].position()).sub(vertices[0].position()));
        }
        if (normal.lengthSquared() <= 1.0E-8F) normal.set(0.0F, 1.0F, 0.0F);
        return normal.normalize();
    }

    private static void packVertex(
            int @NotNull [] target,
            int offset,
            @NotNull Vertex vertex,
            @NotNull TextureAtlasSprite sprite,
            @NotNull Vector3f fallbackNormal
    ) {
        target[offset] = Float.floatToRawIntBits(vertex.position().x());
        target[offset + 1] = Float.floatToRawIntBits(vertex.position().y());
        target[offset + 2] = Float.floatToRawIntBits(vertex.position().z());
        target[offset + 3] = packColor(vertex.color());
        target[offset + 4] = Float.floatToRawIntBits(sprite.getU(vertex.u()));
        target[offset + 5] = Float.floatToRawIntBits(sprite.getV(vertex.v()));
        target[offset + 6] = 0;
        Vector3f normal = vertex.normal().lengthSquared() > 0.0F ? vertex.normal() : fallbackNormal;
        target[offset + 7] = packNormal(normal);
    }

    private static int packColor(float @NotNull [] color) {
        int red = channel(color[0]);
        int green = channel(color[1]);
        int blue = channel(color[2]);
        int alpha = channel(color[3]);
        return red | green << 8 | blue << 16 | alpha << 24;
    }

    private static int packNormal(@NotNull Vector3f normal) {
        int x = Math.round(Math.clamp(normal.x(), -1.0F, 1.0F) * 127.0F) & 0xFF;
        int y = Math.round(Math.clamp(normal.y(), -1.0F, 1.0F) * 127.0F) & 0xFF;
        int z = Math.round(Math.clamp(normal.z(), -1.0F, 1.0F) * 127.0F) & 0xFF;
        return x | y << 8 | z << 16;
    }

    private static int channel(float value) {
        return Math.round(Math.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

    private static @NotNull UnsupportedOperationException unsupported(@NotNull String operation) {
        return new UnsupportedOperationException(
                "Minecraft baked models cannot represent renderer operation: " + operation
        );
    }

    private static final class State {
        private float[] color = {1.0F, 1.0F, 1.0F, 1.0F};
        private BlendMode blendMode = BlendMode.DISABLED;
        private float lineWidth = 1.0F;
        private @Nullable Location texture;
        private TextureFilter minFilter = TextureFilter.NEAREST;
        private TextureFilter magFilter = TextureFilter.NEAREST;
        private TextureWrap wrapS = TextureWrap.CLAMP;
        private TextureWrap wrapT = TextureWrap.CLAMP;
        private @Nullable Location font;
        private float fontSize = 9.0F;
        private DepthMode depthMode = DepthMode.ENABLED;
        private boolean depthMask = true;
        private CullMode cullMode = CullMode.BACK;
        private float lightU;
        private float lightV;
        private Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

        private State() {
        }

        private State(@NotNull State other) {
            color = other.color.clone();
            blendMode = other.blendMode;
            lineWidth = other.lineWidth;
            texture = other.texture;
            minFilter = other.minFilter;
            magFilter = other.magFilter;
            wrapS = other.wrapS;
            wrapT = other.wrapT;
            font = other.font;
            fontSize = other.fontSize;
            depthMode = other.depthMode;
            depthMask = other.depthMask;
            cullMode = other.cullMode;
            lightU = other.lightU;
            lightV = other.lightV;
            normal = new Vector3f(other.normal);
        }
    }

    private record Vertex(
            @NotNull Vector3f position,
            float @NotNull [] color,
            float u,
            float v,
            @NotNull Vector3f normal
    ) {
    }

}
