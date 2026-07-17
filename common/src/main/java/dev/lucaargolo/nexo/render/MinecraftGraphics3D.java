package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lucaargolo.nexo.api.render.Graphics3D;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftGraphics3D implements Graphics3D {

    private static final int CURVE_SEGMENTS = 32;
    private static final Map<RenderKey, RenderType> RENDER_TYPES = new ConcurrentHashMap<>();

    private final @NotNull PoseStack poses;
    private final @NotNull MultiBufferSource buffers;
    private final int packedLight;
    private final int packedOverlay;
    private final Deque<State> states = new ArrayDeque<>();

    private State state = new State();
    private @Nullable PrimitiveType primitive;
    private @Nullable VertexFormat format;
    private @Nullable VertexConsumer consumer;
    private float @Nullable [] firstVertex;
    private int vertexCount;
    private int matrixDepth;
    private boolean finished;

    public MinecraftGraphics3D(
            @NotNull PoseStack poses,
            @NotNull MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        this.poses = poses;
        this.buffers = buffers;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        poses.pushPose();
    }

    public void finish() {
        if (finished) return;
        if (primitive != null) throw new IllegalStateException("Renderer ended with an open " + primitive + " primitive");
        if (matrixDepth != 0) throw new IllegalStateException("Renderer ended with " + matrixDepth + " unclosed matrix states");
        if (!states.isEmpty()) throw new IllegalStateException("Renderer ended with " + states.size() + " unclosed render states");
        poses.popPose();
        finished = true;
    }

    @Override
    public void pushMatrix() {
        requireOutsidePrimitive("change the matrix");
        poses.pushPose();
        matrixDepth++;
    }

    @Override
    public void popMatrix() {
        requireOutsidePrimitive("change the matrix");
        if (matrixDepth == 0) throw new IllegalStateException("Cannot pop an empty matrix stack");
        poses.popPose();
        matrixDepth--;
    }

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
        poses.translate(x, y, z);
    }

    @Override
    public void rotate(float angle) {
        rotate(angle, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public void rotate(float angle, float axisX, float axisY, float axisZ) {
        requireOutsidePrimitive("change the matrix");
        poses.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(axisX, axisY, axisZ, angle));
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
        poses.scale(x, y, z);
    }

    @Override
    public void mulMatrix(@NotNull Matrix4f matrix) {
        requireOutsidePrimitive("change the matrix");
        poses.mulPose(matrix);
    }

    @Override
    public @NotNull Matrix4f matrix() {
        return new Matrix4f(poses.last().pose());
    }

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
    public void drawLine(float x1, float y1, float x2, float y2) {
        drawLine(x1, y1, 0.0F, x2, y2, 0.0F);
    }

    @Override
    public void drawRect(float x, float y, float width, float height) {
        begin(PrimitiveType.LINE_LOOP, VertexFormat.POSITION);
        vertex(x, y, 0.0F);
        vertex(x + width, y, 0.0F);
        vertex(x + width, y + height, 0.0F);
        vertex(x, y + height, 0.0F);
        end();
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        begin(PrimitiveType.QUADS, VertexFormat.POSITION);
        vertex(x, y + height, 0.0F);
        vertex(x + width, y + height, 0.0F);
        vertex(x + width, y, 0.0F);
        vertex(x, y, 0.0F);
        end();
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
    public void drawEllipse(float x, float y, float width, float height) {
        begin(PrimitiveType.LINE_LOOP, VertexFormat.POSITION);
        ellipseVertices(x, y, width, height);
        end();
    }

    @Override
    public void fillEllipse(float x, float y, float width, float height) {
        begin(PrimitiveType.TRIANGLE_FAN, VertexFormat.POSITION);
        vertex(x, y, 0.0F);
        ellipseVertices(x, y, width, height);
        end();
    }

    private void ellipseVertices(float x, float y, float width, float height) {
        float radiusX = width * 0.5F;
        float radiusY = height * 0.5F;
        for (int i = 0; i < CURVE_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / CURVE_SEGMENTS;
            vertex(x + (float) Math.cos(angle) * radiusX, y + (float) Math.sin(angle) * radiusY, 0.0F);
        }
    }

    @Override
    public void drawRoundedRect(float x, float y, float width, float height, float radius) {
        roundedRect(x, y, width, height, radius, PrimitiveType.LINE_LOOP);
    }

    @Override
    public void fillRoundedRect(float x, float y, float width, float height, float radius) {
        roundedRect(x, y, width, height, radius, PrimitiveType.TRIANGLE_FAN);
    }

    private void roundedRect(float x, float y, float width, float height, float radius, PrimitiveType type) {
        float actualRadius = Math.max(0.0F, Math.min(radius, Math.min(Math.abs(width), Math.abs(height)) * 0.5F));
        begin(type, VertexFormat.POSITION);
        if (type == PrimitiveType.TRIANGLE_FAN) vertex(x + width * 0.5F, y + height * 0.5F, 0.0F);
        arcVertices(x + width - actualRadius, y + actualRadius, actualRadius, -90.0F, 0.0F, 8);
        arcVertices(x + width - actualRadius, y + height - actualRadius, actualRadius, 0.0F, 90.0F, 8);
        arcVertices(x + actualRadius, y + height - actualRadius, actualRadius, 90.0F, 180.0F, 8);
        arcVertices(x + actualRadius, y + actualRadius, actualRadius, 180.0F, 270.0F, 8);
        end();
    }

    @Override
    public void drawPolygon(float @NotNull [] x, float @NotNull [] y) {
        polygon(x, y, PrimitiveType.LINE_LOOP);
    }

    @Override
    public void fillPolygon(float @NotNull [] x, float @NotNull [] y) {
        polygon(x, y, PrimitiveType.TRIANGLE_FAN);
    }

    private void polygon(float @NotNull [] x, float @NotNull [] y, @NotNull PrimitiveType type) {
        if (x.length != y.length || x.length < 3) {
            throw new IllegalArgumentException("A polygon needs matching x/y arrays with at least three points");
        }
        begin(type, VertexFormat.POSITION);
        for (int i = 0; i < x.length; i++) vertex(x[i], y[i], 0.0F);
        end();
    }

    @Override
    public void drawArc(float x, float y, float radius, float startAngle, float endAngle) {
        begin(PrimitiveType.LINE_STRIP, VertexFormat.POSITION);
        arcVertices(x, y, radius, startAngle, endAngle, CURVE_SEGMENTS);
        end();
    }

    @Override
    public void fillArc(float x, float y, float radius, float startAngle, float endAngle) {
        begin(PrimitiveType.TRIANGLE_FAN, VertexFormat.POSITION);
        vertex(x, y, 0.0F);
        arcVertices(x, y, radius, startAngle, endAngle, CURVE_SEGMENTS);
        end();
    }

    private void arcVertices(float x, float y, float radius, float startAngle, float endAngle, int segments) {
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
            vertex(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius, 0.0F);
        }
    }

    @Override
    public void bindTexture(@NotNull Location texture) {
        requireOutsidePrimitive("bind a texture");
        state.texture = texture;
        state.sprite = sprite(texture);
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
    public void drawText(@NotNull String text, float x, float y) {
        requireOutsidePrimitive("draw text");
        Font minecraftFont = Minecraft.getInstance().font;
        Matrix4f matrix = matrix();
        float scale = state.fontSize / minecraftFont.lineHeight;
        matrix.scale(scale);
        int color = packColor(state.color);
        int light = light();
        if (state.font == null) {
            minecraftFont.drawInBatch(text, x / scale, y / scale, color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
        } else {
            ResourceLocation font = resource(state.font);
            Component component = Component.literal(text).withStyle(Style.EMPTY.withFont(font));
            minecraftFont.drawInBatch(component, x / scale, y / scale, color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
        }
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
        state.lightU = Math.round(u);
        state.lightV = Math.round(v);
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
    public void drawLine(float x1, float y1, float z1, float x2, float y2, float z2) {
        begin(PrimitiveType.LINES, VertexFormat.POSITION);
        vertex(x1, y1, z1);
        vertex(x2, y2, z2);
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
    public void begin(@NotNull PrimitiveType type, @NotNull VertexFormat format) {
        if (primitive != null) throw new IllegalStateException("Cannot begin a primitive before ending " + primitive);
        if (type == PrimitiveType.POINTS) throw unsupported("point primitives");
        primitive = type;
        this.format = format;
        consumer = buffers.getBuffer(renderType(type));
        firstVertex = null;
        vertexCount = 0;
    }

    @Override
    public void vertex(float @NotNull ... data) {
        if (primitive == null || format == null || consumer == null) {
            throw new IllegalStateException("Cannot add a vertex outside begin/end");
        }
        if (data.length != format.stride()) {
            throw new IllegalArgumentException(format + " needs " + format.stride() + " values, received " + data.length);
        }
        if (firstVertex == null) firstVertex = data.clone();
        emit(data);
        vertexCount++;
    }

    private void emit(float @NotNull [] data) {
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

        float r = state.color[0];
        float g = state.color[1];
        float b = state.color[2];
        float a = state.color[3];
        if (colorOffset >= 0) {
            r *= data[colorOffset];
            g *= data[colorOffset + 1];
            b *= data[colorOffset + 2];
            a *= data[colorOffset + 3];
        }

        VertexConsumer actualConsumer = consumer;
        actualConsumer.addVertex(poses.last(), data[0], data[1], data[2])
                .setColor(r, g, b, a);
        if (state.texture != null) {
            TextureAtlasSprite sprite = state.sprite;
            float u = textureOffset >= 0 ? data[textureOffset] : 0.0F;
            float v = textureOffset >= 0 ? data[textureOffset + 1] : 0.0F;
            actualConsumer.setUv(sprite.getU(u), sprite.getV(v))
                    .setOverlay(packedOverlay)
                    .setLight(light());
            Vector3f normal = normalOffset >= 0
                    ? new Vector3f(data[normalOffset], data[normalOffset + 1], data[normalOffset + 2])
                    : state.normal;
            actualConsumer.setNormal(poses.last(), normal.x(), normal.y(), normal.z());
        }
    }

    @Override
    public void end() {
        if (primitive == null) throw new IllegalStateException("Cannot end without begin");
        if (primitive == PrimitiveType.LINE_LOOP && firstVertex != null) {
            emit(firstVertex);
        }
        validateVertexCount(primitive, vertexCount);
        primitive = null;
        format = null;
        consumer = null;
        firstVertex = null;
        vertexCount = 0;
    }

    private static void validateVertexCount(@NotNull PrimitiveType type, int count) {
        switch (type) {
            case QUADS -> requireMultiple(type, count, 4);
            case TRIANGLES -> requireMultiple(type, count, 3);
            case LINES -> requireMultiple(type, count, 2);
            case TRIANGLE_STRIP, TRIANGLE_FAN -> requireMinimum(type, count, 3);
            case LINE_STRIP, LINE_LOOP -> requireMinimum(type, count, 2);
            case POINTS -> throw unsupported("point primitives");
        }
    }

    private static void requireMultiple(@NotNull PrimitiveType type, int count, int multiple) {
        if (count == 0 || count % multiple != 0) {
            throw new IllegalStateException(type + " needs a non-empty multiple of " + multiple + " vertices");
        }
    }

    private static void requireMinimum(@NotNull PrimitiveType type, int count, int minimum) {
        if (count < minimum) throw new IllegalStateException(type + " needs at least " + minimum + " vertices");
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

    private @NotNull RenderType renderType(@NotNull PrimitiveType type) {
        boolean textured = state.texture != null;
        RenderKey key = new RenderKey(
                mode(type),
                textured,
                state.blendMode,
                state.depthMode,
                state.depthMask,
                state.cullMode,
                state.lineWidth,
                state.minFilter,
                state.magFilter
        );
        return RENDER_TYPES.computeIfAbsent(key, MinecraftGraphics3D::createRenderType);
    }

    private static @NotNull RenderType createRenderType(@NotNull RenderKey key) {
        return NexoRenderState.create(key);
    }

    private static boolean blurred(@NotNull TextureFilter min, @NotNull TextureFilter mag) {
        return min == TextureFilter.LINEAR
                || min == TextureFilter.LINEAR_MIPMAP_NEAREST
                || min == TextureFilter.LINEAR_MIPMAP_LINEAR
                || mag == TextureFilter.LINEAR;
    }

    private static boolean mipmapped(@NotNull TextureFilter min) {
        return min != TextureFilter.NEAREST && min != TextureFilter.LINEAR;
    }

    private static com.mojang.blaze3d.vertex.VertexFormat.@NotNull Mode mode(@NotNull PrimitiveType type) {
        return switch (type) {
            case TRIANGLES -> com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES;
            case TRIANGLE_STRIP -> com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN;
            case LINES -> com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES;
            case LINE_STRIP, LINE_LOOP -> com.mojang.blaze3d.vertex.VertexFormat.Mode.LINE_STRIP;
            case QUADS -> com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS;
            case POINTS -> throw unsupported("point primitives");
        };
    }

    private int light() {
        return state.customLight ? state.lightU | state.lightV << 16 : packedLight;
    }

    private static @NotNull TextureAtlasSprite sprite(@NotNull Location texture) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        return atlas.getSprite(resourceWithoutExtension(texture));
    }

    private static @NotNull ResourceLocation resourceWithoutExtension(@NotNull Location location) {
        String path = location.path();
        int extension = path.lastIndexOf('.');
        if (extension >= 0) path = path.substring(0, extension);
        return ResourceLocation.fromNamespaceAndPath(location.namespace(), path);
    }

    private static @NotNull ResourceLocation resource(@NotNull Location location) {
        return ResourceLocation.fromNamespaceAndPath(location.namespace(), location.path());
    }

    private static int packColor(float @NotNull [] color) {
        return channel(color[3]) << 24 | channel(color[0]) << 16 | channel(color[1]) << 8 | channel(color[2]);
    }

    private static int channel(float value) {
        return Math.round(Math.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

    private void requireOutsidePrimitive(@NotNull String operation) {
        if (primitive != null) throw new IllegalStateException("Cannot " + operation + " inside begin/end");
    }

    private static @NotNull UnsupportedOperationException unsupported(@NotNull String operation) {
        return new UnsupportedOperationException("Minecraft dynamic rendering does not support " + operation);
    }

    private record RenderKey(
            com.mojang.blaze3d.vertex.VertexFormat.@NotNull Mode mode,
            boolean textured,
            @NotNull BlendMode blendMode,
            @NotNull DepthMode depthMode,
            boolean depthMask,
            @NotNull CullMode cullMode,
            float lineWidth,
            @NotNull TextureFilter minFilter,
            @NotNull TextureFilter magFilter
    ) {
    }

    private static final class NexoRenderState extends RenderType {

        private NexoRenderState(@NotNull RenderKey key, @NotNull List<RenderStateShard> states) {
            super(
                    "nexo_dynamic_" + Integer.toUnsignedString(key.hashCode(), 36),
                    key.textured ? DefaultVertexFormat.NEW_ENTITY : DefaultVertexFormat.POSITION_COLOR,
                    key.mode,
                    SMALL_BUFFER_SIZE,
                    false,
                    key.blendMode != BlendMode.DISABLED,
                    () -> states.forEach(RenderStateShard::setupRenderState),
                    () -> states.forEach(RenderStateShard::clearRenderState)
            );
        }

        private static @NotNull RenderType create(@NotNull RenderKey key) {
            List<RenderStateShard> states = new ArrayList<>();
            states.add(key.textured ? RENDERTYPE_ENTITY_TRANSLUCENT_SHADER : POSITION_COLOR_SHADER);
            states.add(transparency(key.blendMode));
            states.add(depthTest(key.depthMode));
            states.add(cullState(key.cullMode));
            states.add(writeMask(key.depthMode, key.depthMask));
            states.add(new LineStateShard(OptionalDouble.of(key.lineWidth)));
            if (key.textured) {
                states.add(new TextureStateShard(
                        InventoryMenu.BLOCK_ATLAS,
                        blurred(key.minFilter, key.magFilter),
                        mipmapped(key.minFilter)
                ));
                states.add(LIGHTMAP);
                states.add(OVERLAY);
            }
            return new NexoRenderState(key, List.copyOf(states));
        }

        private static @NotNull TransparencyStateShard transparency(@NotNull BlendMode mode) {
            return switch (mode) {
                case DISABLED -> NO_TRANSPARENCY;
                case ALPHA -> TRANSLUCENT_TRANSPARENCY;
                case ADD -> ADDITIVE_TRANSPARENCY;
                case MULTIPLY -> customTransparency(mode, GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO, GL14.GL_FUNC_ADD);
                case SUBTRACT -> customTransparency(mode, GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GL14.GL_FUNC_REVERSE_SUBTRACT);
                case PREMUL_ALPHA -> customTransparency(mode, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
                case SCREEN -> customTransparency(mode, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GL14.GL_FUNC_ADD);
            };
        }

        private static @NotNull TransparencyStateShard customTransparency(
                @NotNull BlendMode mode,
                @NotNull GlStateManager.SourceFactor source,
                @NotNull GlStateManager.DestFactor destination,
                int equation
        ) {
            return new TransparencyStateShard(
                    "nexo_" + mode.name().toLowerCase(),
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendEquation(equation);
                        RenderSystem.blendFunc(source, destination);
                    },
                    () -> {
                        RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableBlend();
                    }
            );
        }

        private static @NotNull DepthTestStateShard depthTest(@NotNull DepthMode mode) {
            return switch (mode) {
                case DISABLED -> NO_DEPTH_TEST;
                case READ_ONLY, ENABLED -> LEQUAL_DEPTH_TEST;
                case REVERSED -> GREATER_DEPTH_TEST;
            };
        }

        private static @NotNull RenderStateShard cullState(@NotNull CullMode mode) {
            return switch (mode) {
                case DISABLED -> NO_CULL;
                case BACK -> CULL;
                case FRONT -> new RenderStateShard(
                        "nexo_front_cull",
                        () -> {
                            RenderSystem.enableCull();
                            GL11.glCullFace(GL11.GL_FRONT);
                        },
                        () -> {
                            GL11.glCullFace(GL11.GL_BACK);
                            RenderSystem.disableCull();
                        }
                ) {
                };
            };
        }

        private static @NotNull WriteMaskStateShard writeMask(@NotNull DepthMode mode, boolean depthMask) {
            return (mode == DepthMode.ENABLED || mode == DepthMode.REVERSED) && depthMask
                    ? COLOR_DEPTH_WRITE
                    : COLOR_WRITE;
        }
    }

    private static final class State {
        private float[] color = {1.0F, 1.0F, 1.0F, 1.0F};
        private BlendMode blendMode = BlendMode.DISABLED;
        private float lineWidth = 1.0F;
        private @Nullable Location texture;
        private @Nullable TextureAtlasSprite sprite;
        private TextureFilter minFilter = TextureFilter.NEAREST;
        private TextureFilter magFilter = TextureFilter.NEAREST;
        private TextureWrap wrapS = TextureWrap.CLAMP;
        private TextureWrap wrapT = TextureWrap.CLAMP;
        private @Nullable Location font;
        private float fontSize = 9.0F;
        private DepthMode depthMode = DepthMode.ENABLED;
        private boolean depthMask = true;
        private CullMode cullMode = CullMode.BACK;
        private boolean customLight;
        private int lightU;
        private int lightV;
        private Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);

        private State() {
        }

        private State(@NotNull State other) {
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
            customLight = other.customLight;
            lightU = other.lightU;
            lightV = other.lightV;
            normal = new Vector3f(other.normal);
        }
    }
}
