package dev.lucaargolo.nexo.render;

import dev.lucaargolo.nexo.NexoMinecraft;
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

public final class MinecraftBakedGraphics3D extends AbstractMinecraftGraphics3D {

    private static final Location MISSING_TEXTURE = Location.of("minecraft", "missingno");

    private final Function<Material, TextureAtlasSprite> textureGetter;
    private final Deque<Matrix4f> matrices = new ArrayDeque<>();
    private final List<BakedQuad> quads = new ArrayList<>();
    private final List<Vertex> vertices = new ArrayList<>();

    private Matrix4f matrix = new Matrix4f();
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
    protected void matrixTranslate(float x, float y, float z) {
        matrix.translate(x, y, z);
    }

    @Override
    protected void matrixRotate(float angle, float axisX, float axisY, float axisZ) {
        matrix.rotate((float) Math.toRadians(angle), axisX, axisY, axisZ);
    }

    @Override
    protected void matrixScale(float x, float y, float z) {
        matrix.scale(x, y, z);
    }

    @Override
    protected void matrixMul(@NotNull Matrix4f m) {
        matrix.mul(m);
    }

    @Override
    protected @NotNull Matrix4f matrixGet() {
        return new Matrix4f(matrix);
    }


    @Override
    public void blendMode(@NotNull BlendMode mode) {
        if (mode != BlendMode.DISABLED) throw unsupported("blend mode " + mode);
        state.blendMode = mode;
    }

    @Override
    public void lineWidth(float width) {
        if (width != 1.0F) throw unsupported("line width");
        state.lineWidth = width;
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
    public void depthMode(@NotNull DepthMode mode) {
        if (mode != DepthMode.ENABLED) throw unsupported("depth mode " + mode);
        state.depthMode = mode;
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
    public void lightmap(float u, float v) {
        if (u != 0.0F || v != 0.0F) throw unsupported("custom lightmap coordinates");
        state.lightU = u;
        state.lightV = v;
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
    public void drawText(@NotNull String text, float x, float y) {
        throw unsupported("text");
    }

    @Override
    public void drawLine(float x1, float y1, float z1, float x2, float y2, float z2) {
        throw unsupported("lines");
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

    private void requireVertexMultiple(@NotNull PrimitiveType type, int multiple) {
        if (vertices.isEmpty() || vertices.size() % multiple != 0) {
            throw new IllegalStateException(type + " needs a non-empty multiple of " + multiple + " vertices");
        }
    }

    private @NotNull TextureAtlasSprite sprite(@Nullable Location texture) {
        Location actual = texture != null ? texture.withoutExtension() : MISSING_TEXTURE;
        ResourceLocation location = NexoMinecraft.rl(actual);
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

    protected static @NotNull UnsupportedOperationException unsupported(@NotNull String operation) {
        return new UnsupportedOperationException(
                "Minecraft baked models cannot represent renderer operation: " + operation
        );
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
