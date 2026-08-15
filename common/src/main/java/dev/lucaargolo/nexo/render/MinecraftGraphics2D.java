package dev.lucaargolo.nexo.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.shader.ShaderSource;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.render.util.VertexFormat;
import dev.lucaargolo.nexo.api.resource.Resource;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.font.MinecraftFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MinecraftGraphics2D extends AbstractMinecraftGraphics2D implements Graphics2D, AutoCloseable {

    public static final Location DEFAULT_FONT_LOCATION = Location.of("nexo", "fonts/inter");
    private static final int CURVE_SEGMENTS = 32;
    private static final Map<RenderKey, RenderType> RENDER_TYPES = new ConcurrentHashMap<>();
    private static final Map<Location, MinecraftFont> FONT_CACHE = new ConcurrentHashMap<>();

    protected final @NotNull PoseStack poses;
    private final @NotNull NexoMinecraft nexo;
    private final @NotNull MultiBufferSource buffers;
    private final @NotNull MinecraftShaderRenderer shaderRenderer;
    private final @NotNull Map<RenderKey, RenderType> customRenderTypes = new HashMap<>();
    private final int packedLight;
    private final int packedOverlay;

    private @Nullable VertexConsumer consumer;
    private @Nullable BufferBuilder deferredBuilder;
    private @Nullable ByteBufferBuilder deferredAllocation;
    private @Nullable RenderType activeRenderType;
    private @Nullable ResourceLocation fontAtlasLocation;
    private float @Nullable [] firstVertex;
    private int vertexCount;
    private int matrixDepth;
    private boolean finished;

    public MinecraftGraphics2D(
            @NotNull NexoMinecraft nexo,
            @NotNull PoseStack poses,
            @NotNull MultiBufferSource buffers,
            @NotNull MinecraftShaderRenderer shaderRenderer,
            int packedLight,
            int packedOverlay
    ) {
        this.nexo = nexo;
        this.poses = poses;
        this.buffers = buffers;
        this.shaderRenderer = shaderRenderer;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        poses.pushPose();
        state.depthMode = DepthMode.DISABLED;
        state.cullMode = CullMode.DISABLED;
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
    public void close() {
        finish();
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
    protected void matrixTranslate(float x, float y, float z) {
        poses.translate(x, y, z);
    }

    @Override
    protected void matrixRotate(float angle, float axisX, float axisY, float axisZ) {
        poses.mulPose(new Quaternionf().fromAxisAngleDeg(axisX, axisY, axisZ, angle));
    }

    @Override
    protected void matrixScale(float x, float y, float z) {
        poses.scale(x, y, z);
    }

    @Override
    protected void matrixMul(@NotNull Matrix4f matrix) {
        poses.mulPose(matrix);
    }

    @Override
    protected @NotNull Matrix4f matrixGet() {
        return new Matrix4f(poses.last().pose());
    }


    @Override
    public @NotNull Shader createShader(@NotNull ShaderSource source) {
        requireOutsidePrimitive("create a shader");
        return new MinecraftShader(source, shaderRenderer);
    }

    @Override
    public void bindShader(@Nullable Shader shader) {
        requireOutsidePrimitive("bind a shader");
        if (shader != null && !(shader instanceof MinecraftShader)) {
            throw new IllegalArgumentException("Shader was created by another rendering backend");
        }
        if (shader instanceof MinecraftShader minecraftShader && minecraftShader.closed()) {
            throw new IllegalStateException("Cannot bind a closed shader");
        }
        state.shader = shader;
    }

    @Override
    public @Nullable Shader shader() {
        return state.shader;
    }

    @Override
    public @NotNull Location sceneTexture() {
        return MinecraftShaderRenderer.SCENE_TEXTURE;
    }


    @Override
    public void bindTexture(@NotNull Location texture) {
        super.bindTexture(texture);
        state.sprite = sprite(texture);
    }


    @Override
    public void drawRect(float x, float y, float width, float height) {
        strokePolyline(new float[][]{{x, y}, {x + width, y}, {x + width, y + height}, {x, y + height}}, true);
    }

    @Override
    public void fillRect(float x, float y, float width, float height) {
        beginShape(PrimitiveType.QUADS);
        shapeVertex(x, y + height, x, y, x + width, y + height);
        shapeVertex(x + width, y + height, x, y, x + width, y + height);
        shapeVertex(x + width, y, x, y, x + width, y + height);
        shapeVertex(x, y, x, y, x + width, y + height);
        end();
    }

    @Override
    public void drawEllipse(float x, float y, float width, float height) {
        strokePolyline(ellipsePoints(x, y, width, height), true);
    }

    private float @NotNull [] @NotNull [] ellipsePoints(float x, float y, float width, float height) {
        float radiusX = width * 0.5F;
        float radiusY = height * 0.5F;
        float @NotNull [] @NotNull [] points = new float[CURVE_SEGMENTS][2];
        for (int i = 0; i < CURVE_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / CURVE_SEGMENTS;
            points[i][0] = x + (float) Math.cos(angle) * radiusX;
            points[i][1] = y + (float) Math.sin(angle) * radiusY;
        }
        return points;
    }

    @Override
    public void fillEllipse(float x, float y, float width, float height) {
        beginShape(PrimitiveType.TRIANGLE_FAN);
        shapeVertex(x, y, x - width * 0.5F, y - height * 0.5F, x + width * 0.5F, y + height * 0.5F);
        ellipseVertices(x, y, width, height);
        end();
    }

    private void ellipseVertices(float x, float y, float width, float height) {
        float radiusX = width * 0.5F;
        float radiusY = height * 0.5F;
        float minX = x - radiusX;
        float minY = y - radiusY;
        float maxX = x + radiusX;
        float maxY = y + radiusY;
        for (int i = 0; i <= CURVE_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / CURVE_SEGMENTS;
            shapeVertex(x + (float) Math.cos(angle) * radiusX, y + (float) Math.sin(angle) * radiusY, minX, minY, maxX, maxY);
        }
    }

    @Override
    public void drawRoundedRect(float x, float y, float width, float height, float radius) {
        float actualRadius = clampRadius(radius, width, height);
        strokePolyline(roundedRectPoints(x, y, width, height, actualRadius), true);
    }

    @Override
    public void fillRoundedRect(float x, float y, float width, float height, float radius) {
        roundedRect(x, y, width, height, radius, PrimitiveType.TRIANGLE_FAN);
    }

    private void roundedRect(float x, float y, float width, float height, float radius, PrimitiveType type) {
        float actualRadius = clampRadius(radius, width, height);
        beginShape(type);
        if (type == PrimitiveType.TRIANGLE_FAN) shapeVertex(x + width * 0.5F, y + height * 0.5F, x, y, x + width, y + height);
        arcVertices(x + width - actualRadius, y + actualRadius, actualRadius, -90.0F, 0.0F, 8, x, y, x + width, y + height);
        arcVertices(x + width - actualRadius, y + height - actualRadius, actualRadius, 0.0F, 90.0F, 8, x, y, x + width, y + height);
        arcVertices(x + actualRadius, y + height - actualRadius, actualRadius, 90.0F, 180.0F, 8, x, y, x + width, y + height);
        arcVertices(x + actualRadius, y + actualRadius, actualRadius, 180.0F, 270.0F, 8, x, y, x + width, y + height);
        // Re-emit the first corner vertex to close triangle fans
        if (type == PrimitiveType.TRIANGLE_FAN) shapeVertex(x + width - actualRadius, y, x, y, x + width, y + height);
        end();
    }

    private static float clampRadius(float radius, float width, float height) {
        return Math.max(0.0F, Math.min(radius, Math.min(Math.abs(width), Math.abs(height)) * 0.5F));
    }

    private float @NotNull [] @NotNull [] roundedRectPoints(float x, float y, float width, float height, float radius) {
        int cornerSegments = 8;
        float @NotNull [] @NotNull [] points = new float[cornerSegments * 4 + 4][2];
        int index = 0;
        index = arcPoints(points, index, x + width - radius, y + radius, radius, -90.0F, 0.0F, cornerSegments);
        index = arcPoints(points, index, x + width - radius, y + height - radius, radius, 0.0F, 90.0F, cornerSegments);
        index = arcPoints(points, index, x + radius, y + height - radius, radius, 90.0F, 180.0F, cornerSegments);
        index = arcPoints(points, index, x + radius, y + radius, radius, 180.0F, 270.0F, cornerSegments);
        return points;
    }

    @Override
    public void drawPolygon(float @NotNull [] x, float @NotNull [] y) {
        if (x.length != y.length || x.length < 3) {
            throw new IllegalArgumentException("A polygon needs matching x/y arrays with at least three points");
        }
        float @NotNull [] @NotNull [] points = new float[x.length][2];
        for (int i = 0; i < x.length; i++) {
            points[i][0] = x[i];
            points[i][1] = y[i];
        }
        strokePolyline(points, true);
    }

    @Override
    public void fillPolygon(float @NotNull [] x, float @NotNull [] y) {
        polygon(x, y, PrimitiveType.TRIANGLE_FAN);
    }

    private void polygon(float @NotNull [] x, float @NotNull [] y, @NotNull PrimitiveType type) {
        if (x.length != y.length || x.length < 3) {
            throw new IllegalArgumentException("A polygon needs matching x/y arrays with at least three points");
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < x.length; i++) {
            minX = Math.min(minX, x[i]);
            minY = Math.min(minY, y[i]);
            maxX = Math.max(maxX, x[i]);
            maxY = Math.max(maxY, y[i]);
        }
        beginShape(type);
        for (int i = 0; i < x.length; i++) shapeVertex(x[i], y[i], minX, minY, maxX, maxY);
        // Re-emit the first vertex to close triangle fans
        if (type == PrimitiveType.TRIANGLE_FAN) shapeVertex(x[0], y[0], minX, minY, maxX, maxY);
        end();
    }

    @Override
    public void drawArc(float x, float y, float radius, float startAngle, float endAngle) {
        strokePolyline(arcPoints(x, y, radius, startAngle, endAngle, CURVE_SEGMENTS), false);
    }

    private float @NotNull [] @NotNull [] arcPoints(float x, float y, float radius, float startAngle, float endAngle, int segments) {
        float @NotNull [] @NotNull [] points = new float[segments + 1][2];
        arcPoints(points, 0, x, y, radius, startAngle, endAngle, segments);
        return points;
    }

    private static int arcPoints(
            float @NotNull [] @NotNull [] points,
            int index,
            float x,
            float y,
            float radius,
            float startAngle,
            float endAngle,
            int segments
    ) {
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
            points[index][0] = x + (float) Math.cos(angle) * radius;
            points[index][1] = y + (float) Math.sin(angle) * radius;
            index++;
        }
        return index;
    }

    @Override
    public void fillArc(float x, float y, float radius, float startAngle, float endAngle) {
        beginShape(PrimitiveType.TRIANGLE_FAN);
        shapeVertex(x, y, x - radius, y - radius, x + radius, y + radius);
        arcVertices(x, y, radius, startAngle, endAngle, CURVE_SEGMENTS, x - radius, y - radius, x + radius, y + radius);
        end();
    }

    private void arcVertices(float x, float y, float radius, float startAngle, float endAngle, int segments, float minX, float minY, float maxX, float maxY) {
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
            shapeVertex(x + (float) Math.cos(angle) * radius, y + (float) Math.sin(angle) * radius, minX, minY, maxX, maxY);
        }
    }


    @Override
    public void drawText(@NotNull String text, float x, float y) {
        requireOutsidePrimitive("draw text");
        MinecraftFont font = resolveFont();
        if (font == null) {
            drawTextVanilla(text, x, y, isMinecraftFont());
            return;
        }
        font.ensureGlyphs(text);
        font.upload();
        float scale = state.fontSize / MinecraftFont.ATLAS_SIZE;
        float penX = x;
        float penY = y + font.ascent() * scale;
        int prevCodepoint = 0;
        fontAtlasLocation = font.textureLocation();
        Location prevTexture = state.texture;
        Object prevSprite = state.sprite;
        Shader prevShader = state.shader;
        state.texture = null;
        state.sprite = null;
        state.shader = null;
        try {
            boolean started = false;
            for (int i = 0; i < text.length(); ) {
                int codepoint = text.codePointAt(i);
                i += Character.charCount(codepoint);
                if (prevCodepoint != 0) {
                    penX += font.kern(prevCodepoint, codepoint) * scale;
                }
                MinecraftFont.Glyph glyph = font.glyph(codepoint);
                if (glyph.width() > 0.0F) {
                    if (!started) {
                        begin(PrimitiveType.QUADS, VertexFormat.POSITION_TEX);
                        started = true;
                    }
                    float x0 = penX + glyph.xOffset() * scale;
                    float y0 = penY + glyph.yOffset() * scale;
                    float width = glyph.width() * scale;
                    float height = glyph.height() * scale;
                    vertex(x0, y0 + height, 0.0F, glyph.u0(), glyph.v1());
                    vertex(x0 + width, y0 + height, 0.0F, glyph.u1(), glyph.v1());
                    vertex(x0 + width, y0, 0.0F, glyph.u1(), glyph.v0());
                    vertex(x0, y0, 0.0F, glyph.u0(), glyph.v0());
                }
                penX += glyph.advance() * scale;
                prevCodepoint = codepoint;
            }
            if (started) {
                end();
            }
        } finally {
            fontAtlasLocation = null;
            state.texture = prevTexture;
            state.sprite = prevSprite;
            state.shader = prevShader;
        }
    }

    private @Nullable MinecraftFont resolveFont() {
        return FONT_CACHE.computeIfAbsent(fontLocation(), location -> {
            FontResource.TTF resource = nexo.getResource(Resource.Type.FONT, location);
            if (resource == null) return null;
            byte[] data = resource.data();
            return data != null ? new MinecraftFont(data, resource.location()) : null;
        });
    }

    private boolean isMinecraftFont() {
        return nexo.getResource(Resource.Type.MINECRAFT_FONT, fontLocation()) != null;
    }

    private @NotNull Location fontLocation() {
        return state.font != null ? state.font : DEFAULT_FONT_LOCATION;
    }

    @Override
    public float textWidth(@NotNull String text) {
        MinecraftFont font = resolveFont();
        if (font == null) {
            Font minecraftFont = Minecraft.getInstance().font;
            float scale = state.fontSize / minecraftFont.lineHeight;
            return minecraftFont.width(text) * scale;
        }
        return font.width(text, state.fontSize / MinecraftFont.ATLAS_SIZE);
    }

    private void drawTextVanilla(@NotNull String text, float x, float y, boolean forceDefaultFont) {
        Font minecraftFont = Minecraft.getInstance().font;
        Matrix4f matrix = matrix();
        float scale = state.fontSize / minecraftFont.lineHeight;
        matrix.scale(scale);
        int color = packColor(state.color);
        int light = light();
        ResourceLocation font = null;
        if (!forceDefaultFont && state.font != null) {
            try {
                font = NexoMinecraft.rl(state.font);
            } catch (ResourceLocationException e) {
                NexoMinecraft.LOGGER.debug("Font location {} is not a valid Minecraft resource location", state.font);
            }
        }
        MutableComponent component = Component.literal(text);
        if (font != null) {
            component = component.withStyle(Style.EMPTY.withFont(font));
        }
        minecraftFont.drawInBatch(component, x / scale, y / scale, color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
    }


    public void drawLine(float x1, float y1, float z1, float x2, float y2, float z2) {
        begin(PrimitiveType.LINES, VertexFormat.POSITION);
        vertex(x1, y1, z1);
        vertex(x2, y2, z2);
        end();
    }


    @Override
    public void begin(@NotNull PrimitiveType type, @NotNull VertexFormat format) {
        if (primitive != null) throw new IllegalStateException("Cannot begin a primitive before ending " + primitive);
        if (type == PrimitiveType.POINTS) throw unsupported("point primitives");
        primitive = type;
        this.format = format;
        activeRenderType = renderType(type);
        if (state.shader != null && shaderRenderer.deferred()) {
            deferredAllocation = new ByteBufferBuilder(262144);
            deferredBuilder = new BufferBuilder(deferredAllocation, mode(type), customVertexFormat(format));
            consumer = deferredBuilder;
        } else {
            consumer = buffers.getBuffer(activeRenderType);
        }
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

        float r;
        float g;
        float b;
        float a;
        if (state.texture != null && textureOffset >= 0) {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
            a = 1.0F;
        } else {
            r = state.color[0];
            g = state.color[1];
            b = state.color[2];
            a = state.color[3];
            if (colorOffset >= 0) {
                r *= data[colorOffset];
                g *= data[colorOffset + 1];
                b *= data[colorOffset + 2];
                a *= data[colorOffset + 3];
            }
        }

        VertexConsumer actualConsumer = consumer;
        actualConsumer.addVertex(poses.last(), data[0], data[1], data[2])
                .setColor(r, g, b, a);
        if (textureOffset >= 0) {
            float u = data[textureOffset];
            float v = data[textureOffset + 1];
            if (state.texture != null) {
                TextureAtlasSprite sprite = (TextureAtlasSprite) state.sprite;
                u = sprite.getU(u);
                v = sprite.getV(v);
            }
            actualConsumer.setUv(u, v);
        }
        if (state.shader == null && (state.texture != null || fontAtlasLocation != null)) {
            actualConsumer.setOverlay(packedOverlay).setLight(light());
        }
        if (normalOffset >= 0 || state.shader == null && (state.texture != null || fontAtlasLocation != null)) {
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
        if (deferredBuilder != null && deferredAllocation != null && activeRenderType != null) {
            shaderRenderer.enqueue(
                    activeRenderType,
                    deferredBuilder.buildOrThrow(),
                    deferredAllocation,
                    new Matrix4f(RenderSystem.getModelViewMatrix()),
                    new Matrix4f(RenderSystem.getProjectionMatrix()),
                    RenderSystem.getVertexSorting()
            );
        }
        primitive = null;
        format = null;
        consumer = null;
        deferredBuilder = null;
        deferredAllocation = null;
        activeRenderType = null;
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


    private @NotNull RenderType renderType(@NotNull PrimitiveType type) {
        boolean textured = (state.texture != null && format != VertexFormat.POSITION) || fontAtlasLocation != null;
        MinecraftShader shader = (MinecraftShader) state.shader;
        Map<String, MinecraftShader.UniformValue> uniforms = shader == null ? null : shader.uniforms();
        RenderKey key = new RenderKey(
                mode(type),
                textured,
                state.blendMode,
                state.depthMode,
                state.depthMask,
                state.cullMode,
                state.minFilter,
                state.magFilter,
                shader == null ? null : format,
                shader,
                uniforms,
                fontAtlasLocation
        );
        Map<RenderKey, RenderType> cache = shader == null ? RENDER_TYPES : customRenderTypes;
        return cache.computeIfAbsent(key, MinecraftGraphics2D::createRenderType);
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

    private static @NotNull Mode mode(@NotNull PrimitiveType type) {
        return switch (type) {
            case TRIANGLES -> Mode.TRIANGLES;
            case TRIANGLE_STRIP -> Mode.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> Mode.TRIANGLE_FAN;
            case LINES -> Mode.LINES;
            case LINE_STRIP, LINE_LOOP -> Mode.LINE_STRIP;
            case QUADS -> Mode.QUADS;
            case POINTS -> throw unsupported("point primitives");
        };
    }

    private static com.mojang.blaze3d.vertex.@NotNull VertexFormat customVertexFormat(@NotNull VertexFormat format) {
        return switch (format) {
            case POSITION, POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_TEX, POSITION_COLOR_TEX -> DefaultVertexFormat.POSITION_TEX_COLOR;
            case POSITION_TEX_NORMAL, POSITION_COLOR_TEX_NORMAL -> DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        };
    }

    private int light() {
        return state.customLight ? ((int) state.lightU) | (((int) state.lightV) << 16) : packedLight;
    }

    private static @NotNull TextureAtlasSprite sprite(@NotNull Location texture) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        ResourceLocation location = NexoMinecraft.rl(texture.withoutExtension());
        return atlas.getSprite(location);
    }

    private static int packColor(float @NotNull [] color) {
        return channel(color[3]) << 24 | channel(color[0]) << 16 | channel(color[1]) << 8 | channel(color[2]);
    }

    private record RenderKey(
            @NotNull Mode mode,
            boolean textured,
            @NotNull BlendMode blendMode,
            @NotNull DepthMode depthMode,
            boolean depthMask,
            @NotNull CullMode cullMode,
            @NotNull TextureFilter minFilter,
            @NotNull TextureFilter magFilter,
            @Nullable VertexFormat vertexFormat,
            @Nullable MinecraftShader shader,
            @Nullable Map<String, MinecraftShader.UniformValue> uniforms,
            @Nullable ResourceLocation textureLocation
    ) {
    }

    private static final class NexoRenderState extends RenderType {

        private NexoRenderState(@NotNull RenderKey key, @NotNull List<RenderStateShard> states) {
            super(
                    "nexo_dynamic_" + Integer.toUnsignedString(key.hashCode(), 36),
                    minecraftFormat(key),
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
            if (key.shader == null) {
                states.add(key.textured ? new ShaderStateShard(GameRenderer::getRendertypeTextShader) : POSITION_COLOR_SHADER);
            } else {
                states.add(new ShaderStateShard(() -> {
                    ShaderInstance instance = key.shader.instance(minecraftFormat(key));
                    key.shader.apply(instance, key.uniforms);
                    return instance;
                }));
            }
            states.add(transparency(key.blendMode));
            states.add(depthTest(key.depthMode));
            states.add(cullState(key.cullMode));
            states.add(writeMask(key.depthMode, key.depthMask));
            if (key.textured) {
                if (key.textureLocation != null) {
                    states.add(new TextureStateShard(key.textureLocation, true, false));
                } else {
                    states.add(new TextureStateShard(
                            InventoryMenu.BLOCK_ATLAS,
                            blurred(key.minFilter, key.magFilter),
                            mipmapped(key.minFilter)
                    ));
                }
                states.add(LIGHTMAP);
            }
            return new NexoRenderState(key, List.copyOf(states));
        }

        private static com.mojang.blaze3d.vertex.@NotNull VertexFormat minecraftFormat(@NotNull RenderKey key) {
            if (key.shader == null) {
                if (key.textured) return DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
                return DefaultVertexFormat.POSITION_COLOR;
            }
            if (key.vertexFormat == null) throw new IllegalStateException("Custom shader render type has no vertex format");
            return customVertexFormat(key.vertexFormat);
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
}
