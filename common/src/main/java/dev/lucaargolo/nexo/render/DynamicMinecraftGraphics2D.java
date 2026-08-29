package dev.lucaargolo.nexo.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.Material;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.render.util.VertexLayout;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.render.font.MinecraftText;
import dev.lucaargolo.nexo.render.shader.MinecraftShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayDeque;

public class DynamicMinecraftGraphics2D implements MinecraftGraphics2D, AutoCloseable {

    private static final int CURVE_SEGMENTS = 32;
    private final ArrayDeque<State> states = new ArrayDeque<>();
    private State state = new State();
    private @Nullable PrimitiveType primitive;
    private @Nullable VertexLayout format;

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;

    protected final @NotNull PoseStack poses;
    private final @NotNull MultiBufferSource buffers;
    private final int packedLight;
    private final int packedOverlay;

    private @Nullable VertexConsumer consumer;
    private @Nullable BufferBuilder deferredBuilder;
    private @Nullable ByteBufferBuilder deferredAllocation;
    private @Nullable RenderType activeRenderType;
    private float @Nullable [] firstVertex;
    private int vertexCount;
    private int matrixDepth;
    private boolean finished;

    public DynamicMinecraftGraphics2D(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull PoseStack poses,
            @NotNull MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        this.nexo = nexo;
        this.poses = poses;
        this.buffers = buffers;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        poses.pushPose();
        state.depth = DepthMode.DISABLED;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public @Nullable PrimitiveType primitive() {
        return primitive;
    }

    public void finish() {
        if (finished) {
            return;
        }
        if (primitive != null) {
            throw new IllegalStateException("Renderer ended with an open " + primitive + " primitive");
        }
        if (matrixDepth != 0) {
            throw new IllegalStateException("Renderer ended with " + matrixDepth + " unclosed matrix states");
        }
        if (!states.isEmpty()) {
            throw new IllegalStateException("Renderer ended with " + states.size() + " unclosed render states");
        }
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
        if (matrixDepth == 0) {
            throw new IllegalStateException("Cannot pop an empty matrix stack");
        }
        poses.popPose();
        matrixDepth--;
    }

    @Override
    public void matrixTranslate(float x, float y, float z) {
        poses.translate(x, y, z);
    }

    @Override
    public void matrixRotate(float angle, float axisX, float axisY, float axisZ) {
        poses.mulPose(new Quaternionf().fromAxisAngleDeg(axisX, axisY, axisZ, angle));
    }

    @Override
    public void matrixScale(float x, float y, float z) {
        poses.scale(x, y, z);
    }

    @Override
    public void matrixMul(@NotNull Matrix4f matrix) {
        poses.mulPose(matrix);
    }

    @Override
    public @NotNull Matrix4f matrixGet() {
        return new Matrix4f(poses.last().pose());
    }

    @Override
    public @NotNull CullMode defaultCullMode() {
        return CullMode.DISABLED;
    }

    @Override
    public void pushState() {
        requireOutsidePrimitive("change render state");
        states.push(new State(state));
    }

    @Override
    public void popState() {
        requireOutsidePrimitive("change render state");
        if (states.isEmpty()) {
            throw new IllegalStateException("Cannot pop an empty render-state stack");
        }
        state = states.pop();
    }

    @Override
    public void bindMaterial(@NotNull Material<?> material) {
        var shader = material.shader();
        if (shader != null && !(shader instanceof MinecraftShader)) {
            throw new IllegalArgumentException("Shader was created by another rendering backend");
        }
        if (shader instanceof MinecraftShader minecraftShader && minecraftShader.closed()) {
            throw new IllegalStateException("Cannot bind a material with a closed shader");
        }
        requireOutsidePrimitive("bind a material");
        if (material.wrapS() != TextureWrap.CLAMP || material.wrapT() != TextureWrap.CLAMP) {
            throw MinecraftGraphics2D.unsupported("texture wrapping other than CLAMP");
        }
        state().material = material;
        state().color = material.colorData().clone();
    }

    private @Nullable Location stitchedAtlas(@NotNull Location texture) {
        Location atlas = nexo.getRenderingHandler().atlasHandler().findAtlas(texture);
        if (atlas == null) {
            return null;
        }
        return Minecraft.getInstance().getTextureManager().getTexture(NexoMinecraft.rl(atlas), null) instanceof TextureAtlas
                ? atlas
                : null;
    }

    private @Nullable TextureAtlasSprite sprite(@NotNull Location texture) {
        Location atlas = stitchedAtlas(texture);
        if (atlas == null) {
            return null;
        }
        if (!(Minecraft.getInstance().getTextureManager().getTexture(NexoMinecraft.rl(atlas), null) instanceof TextureAtlas textureAtlas)) {
            return null;
        }
        return textureAtlas.getSprite(NexoMinecraft.rl(texture.withoutExtension()));
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
        if (type == PrimitiveType.TRIANGLE_FAN) {
            shapeVertex(x + width * 0.5F, y + height * 0.5F, x, y, x + width, y + height);
        }
        arcVertices(x + width - actualRadius, y + actualRadius, actualRadius, -90.0F, 0.0F, 8, x, y, x + width, y + height);
        arcVertices(x + width - actualRadius, y + height - actualRadius, actualRadius, 0.0F, 90.0F, 8, x, y, x + width, y + height);
        arcVertices(x + actualRadius, y + height - actualRadius, actualRadius, 90.0F, 180.0F, 8, x, y, x + width, y + height);
        arcVertices(x + actualRadius, y + actualRadius, actualRadius, 180.0F, 270.0F, 8, x, y, x + width, y + height);
        if (type == PrimitiveType.TRIANGLE_FAN) {
            shapeVertex(x + width - actualRadius, y, x, y, x + width, y + height);
        }
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
        for (int i = 0; i < x.length; i++) {
            shapeVertex(x[i], y[i], minX, minY, maxX, maxY);
        }
        if (type == PrimitiveType.TRIANGLE_FAN) {
            shapeVertex(x[0], y[0], minX, minY, maxX, maxY);
        }
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
    public void drawText(@NotNull Text text, float x, float y) {
        requireOutsidePrimitive("draw text");
        Font minecraftFont = Minecraft.getInstance().font;
        int color = packColor(state.color);
        int light = state.light != NO_LIGHT_OVERRIDE ? state.light : packedLight;
        float cursorX = x;
        float cursorY = y;
        float lineHeight = 0.0F;
        for (Text.Run run : MinecraftText.runs(nexo, text)) {
            Text.Style style = run.style();
            float scale = style.size() / minecraftFont.lineHeight;
            String[] lines = run.text().split("\\n", -1);
            lineHeight = Math.max(lineHeight, style.size());
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (!line.isEmpty()) {
                    MutableComponent component = MinecraftText.component(line, style);
                    Matrix4f matrix = matrix();
                    matrix.scale(scale);
                    minecraftFont.drawInBatch(
                            component,
                            cursorX / scale,
                            cursorY / scale,
                            color,
                            false,
                            matrix,
                            buffers,
                            Font.DisplayMode.NORMAL,
                            0,
                            light
                    );
                    cursorX += minecraftFont.width(component) * scale;
                }
                if (i < lines.length - 1) {
                    cursorX = x;
                    cursorY += lineHeight;
                    lineHeight = 0.0F;
                }
            }
        }
    }

    @Override
    public float textWidth(@NotNull Text text) {
        Font minecraftFont = Minecraft.getInstance().font;
        float width = 0.0F;
        float lineWidth = 0.0F;
        for (Text.Run run : MinecraftText.runs(nexo, text)) {
            Text.Style style = run.style();
            float scale = style.size() / minecraftFont.lineHeight;
            String[] lines = run.text().split("\\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (!lines[i].isEmpty()) {
                    MutableComponent component = MinecraftText.component(lines[i], style);
                    lineWidth += minecraftFont.width(component) * scale;
                }
                if (i < lines.length - 1) {
                    width = Math.max(width, lineWidth);
                    lineWidth = 0.0F;
                }
            }
        }
        return Math.max(width, lineWidth);
    }



    @Override
    public void begin(@NotNull PrimitiveType type, @NotNull VertexLayout format) {
        if (primitive != null) {
            throw new IllegalStateException("Cannot begin a primitive before ending " + primitive);
        }
        if (type == PrimitiveType.POINTS) {
            throw MinecraftGraphics2D.unsupported("point primitives");
        }
        primitive = type;
        this.format = format;
        activeRenderType = renderType(type);
        if (shader() != null && nexo.getRenderingHandler().shaderHandler().deferred()) {
            deferredAllocation = new ByteBufferBuilder(262144);
            deferredBuilder = new BufferBuilder(deferredAllocation, mode(type), NexoRenderType.customVertexFormat(format));
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
        if (primitive == PrimitiveType.LINE_LOOP && firstVertex == null) {
            firstVertex = data.clone();
        }
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
        Location texture = texture();
        TextureAtlasSprite sprite = texture == null ? null : sprite(texture);
        boolean textured = texture != null;
        boolean defaultTexturedShader = shader() == null && textured;
        actualConsumer.addVertex(poses.last(), data[0], data[1], data[2]).setColor(r, g, b, a);
        if (textureOffset >= 0) {
            float u = data[textureOffset];
            float v = data[textureOffset + 1];
            if (textured && sprite != null) {
                u = sprite.getU(u);
                v = sprite.getV(v);
            }
            actualConsumer.setUv(u, v);
        }
        if (defaultTexturedShader) {
            int light = state.light != NO_LIGHT_OVERRIDE ? state.light : packedLight;
            actualConsumer.setOverlay(packedOverlay).setLight(light);
        }
        if (normalOffset >= 0 || defaultTexturedShader) {
            Vector3f normal = normalOffset >= 0 ? new Vector3f(data[normalOffset], data[normalOffset + 1], data[normalOffset + 2]) : state.normal;
            actualConsumer.setNormal(poses.last(), normal.x(), normal.y(), normal.z());
        }
    }

    @Override
    public void end() {
        if (primitive == null) {
            throw new IllegalStateException("Cannot end without begin");
        }
        if (primitive == PrimitiveType.LINE_LOOP && firstVertex != null) {
            emit(firstVertex);
        }
        validateVertexCount(primitive, vertexCount);
        if (deferredBuilder != null && deferredAllocation != null && activeRenderType != null) {
            nexo.getRenderingHandler().shaderHandler().enqueue(
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

    private static void requireMultiple(@NotNull PrimitiveType type, int count, int multiple) {
        if (count == 0 || count % multiple != 0) {
            throw new IllegalStateException(type + " needs a non-empty multiple of " + multiple + " vertices");
        }
    }

    private static void requireMinimum(@NotNull PrimitiveType type, int count, int minimum) {
        if (count < minimum) {
            throw new IllegalStateException(type + " needs at least " + minimum + " vertices");
        }
    }
    private static void validateVertexCount(@NotNull PrimitiveType type, int count) {
        switch (type) {
            case QUADS -> requireMultiple(type, count, 4);
            case TRIANGLES -> requireMultiple(type, count, 3);
            case LINES -> requireMultiple(type, count, 2);
            case TRIANGLE_STRIP, TRIANGLE_FAN -> requireMinimum(type, count, 3);
            case LINE_STRIP, LINE_LOOP -> requireMinimum(type, count, 2);
            case POINTS -> throw MinecraftGraphics2D.unsupported("point primitives");
        }
    }

    private @NotNull RenderType renderType(@NotNull PrimitiveType type) {
        VertexLayout layout = java.util.Objects.requireNonNull(format);
        Location texture = texture();
        Location stitchedLocation = texture == null ? null : stitchedAtlas(texture);
        Location textureLocation = stitchedLocation != null ? stitchedLocation : texture;
        return NexoRenderType.create(
                mode(type),
                texture != null && layout.hasTextureCoordinates(),
                textureLocation,
                state.material,
                state.depth,
                cullMode(),
                layout
        );
    }

    private static @NotNull Mode mode(@NotNull PrimitiveType type) {
        return switch (type) {
            case TRIANGLES -> Mode.TRIANGLES;
            case TRIANGLE_STRIP -> Mode.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> Mode.TRIANGLE_FAN;
            case LINES -> Mode.LINES;
            case LINE_STRIP, LINE_LOOP -> Mode.LINE_STRIP;
            case QUADS -> Mode.QUADS;
            case POINTS -> throw MinecraftGraphics2D.unsupported("point primitives");
        };
    }

    public static int packColor(float @NotNull [] color) {
        int red = channel(color[0]);
        int green = channel(color[1]);
        int blue = channel(color[2]);
        int alpha = channel(color[3]);
        return red | green << 8 | blue << 16 | alpha << 24;
    }

    private static int channel(float value) {
        return Math.round(Math.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

}
