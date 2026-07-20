package dev.lucaargolo.nexo.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.lucaargolo.nexo.api.NexoException;
import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.shader.ShaderBuiltins;
import dev.lucaargolo.nexo.api.render.shader.ShaderSource;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix2f;
import org.joml.Matrix2fc;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MinecraftShader implements Shader {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//.*?$", Pattern.MULTILINE);
    private static final Pattern VERSION = Pattern.compile(
            "(?s)^(?:\\s|//[^\\r\\n]*(?:\\R|$)|/\\*.*?\\*/)*#version\\b"
    );
    private static final Pattern UNIFORM = Pattern.compile(
            "(?m)(?:layout\\s*\\([^)]*\\)\\s*)?uniform\\s+(?:(?:lowp|mediump|highp)\\s+)?" +
                    "([A-Za-z_]\\w*)\\s+([A-Za-z_]\\w*)\\s*(\\[[^]]*])?\\s*;"
    );
    private static final Map<String, String> ATTRIBUTE_NAMES = Map.of(
            ShaderBuiltins.POSITION, "Position",
            ShaderBuiltins.COLOR, "Color",
            ShaderBuiltins.TEX_COORD_0, "UV0",
            ShaderBuiltins.NORMAL, "Normal"
    );

    private final @NotNull String name = "nexo/runtime_" + NEXT_ID.incrementAndGet();
    private final byte @NotNull [] vertexSource;
    private final byte @NotNull [] fragmentSource;
    private final @NotNull Map<String, String> uniformTypes;
    private final @NotNull MinecraftShaderRenderer renderer;
    private final @NotNull ConcurrentHashMap<VertexFormat, ShaderInstance> variants = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentHashMap<String, UniformValue> values = new ConcurrentHashMap<>();
    private volatile boolean closed;

    MinecraftShader(@NotNull ShaderSource source, @NotNull MinecraftShaderRenderer renderer) {
        Objects.requireNonNull(source, "source");
        this.renderer = renderer;
        this.uniformTypes = discoverUniforms(source);
        requireBuiltinType(ShaderBuiltins.MODEL_VIEW, "mat4");
        requireBuiltinType(ShaderBuiltins.PROJECTION, "mat4");
        requireBuiltinType(ShaderBuiltins.RESOLUTION, "vec3");
        requireBuiltinType(ShaderBuiltins.TIME, "float");
        requireBuiltinType(ShaderBuiltins.TIME_DELTA, "float");
        requireBuiltinType(ShaderBuiltins.FRAME, "int");
        this.vertexSource = minecraftSource(mapVertexAttributes(source.vertexSource()));
        this.fragmentSource = minecraftSource(source.fragmentSource());
    }

    boolean closed() {
        return closed;
    }

    @NotNull Map<String, UniformValue> uniforms() {
        requireOpen();
        return Map.copyOf(values);
    }

    @NotNull ShaderInstance instance(@NotNull VertexFormat format) {
        requireOpen();
        return variants.computeIfAbsent(format, this::compile);
    }

    void apply(@NotNull ShaderInstance instance, @NotNull Map<String, UniformValue> uniforms) {
        uniforms.forEach((uniformName, value) -> value.apply(instance, uniformName));
        set(instance, ShaderBuiltins.MODEL_VIEW, RenderSystem.getModelViewMatrix());
        set(instance, ShaderBuiltins.PROJECTION, RenderSystem.getProjectionMatrix());

        Uniform resolution = instance.getUniform(ShaderBuiltins.RESOLUTION);
        if (resolution != null) {
            var target = Minecraft.getInstance().getMainRenderTarget();
            resolution.set((float) target.viewWidth, (float) target.viewHeight, 1.0F);
        }
        set(instance, ShaderBuiltins.TIME, renderer.time());
        set(instance, ShaderBuiltins.TIME_DELTA, renderer.timeDelta());
        set(instance, ShaderBuiltins.FRAME, renderer.frame());
    }

    private @NotNull ShaderInstance compile(@NotNull VertexFormat format) {
        String variant = name + "_" + Integer.toUnsignedString(format.hashCode(), 36);
        String vertex = variant + "_vertex";
        String fragment = variant + "_fragment";
        Map<ResourceLocation, byte[]> resources = Map.of(
                shaderResource(variant, ".json"), metadata(vertex, fragment),
                shaderResource(vertex, ".vsh"), vertexSource,
                shaderResource(fragment, ".fsh"), fragmentSource
        );
        ResourceProvider fallback = Minecraft.getInstance().getResourceManager();
        ResourceProvider provider = location -> {
            byte[] data = resources.get(location);
            if (data == null) return fallback.getResource(location);
            Resource coreShader = fallback.getResource(ResourceLocation.withDefaultNamespace("shaders/core/position.json"))
                    .orElseThrow(() -> new IllegalStateException("Minecraft core shaders are unavailable"));
            return java.util.Optional.of(new Resource(coreShader.source(), () -> new ByteArrayInputStream(data)));
        };
        try {
            return new ShaderInstance(provider, variant, format);
        } catch (IOException | RuntimeException exception) {
            throw new NexoException("Failed to compile shader " + name, exception);
        }
    }

    private byte @NotNull [] metadata(@NotNull String vertex, @NotNull String fragment) {
        JsonObject root = new JsonObject();
        root.addProperty("vertex", vertex);
        root.addProperty("fragment", fragment);
        JsonArray samplers = new JsonArray();
        JsonArray uniforms = new JsonArray();
        uniformTypes.forEach((uniformName, type) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", uniformName);
            if (type.equals("sampler2D")) {
                samplers.add(entry);
                return;
            }
            entry.addProperty("type", minecraftType(type));
            entry.addProperty("count", componentCount(type));
            JsonArray defaults = new JsonArray();
            defaults.add(0.0F);
            entry.add("values", defaults);
            uniforms.add(entry);
        });
        root.add("samplers", samplers);
        root.add("uniforms", uniforms);
        return bytes(root.toString());
    }

    @Override
    public void uniform(@NotNull String name, boolean value) {
        integers(name, "bool", value ? 1 : 0);
    }

    @Override
    public void uniform(@NotNull String name, boolean x, boolean y) {
        integers(name, "bvec2", x ? 1 : 0, y ? 1 : 0);
    }

    @Override
    public void uniform(@NotNull String name, boolean x, boolean y, boolean z) {
        integers(name, "bvec3", x ? 1 : 0, y ? 1 : 0, z ? 1 : 0);
    }

    @Override
    public void uniform(@NotNull String name, boolean x, boolean y, boolean z, boolean w) {
        integers(name, "bvec4", x ? 1 : 0, y ? 1 : 0, z ? 1 : 0, w ? 1 : 0);
    }

    @Override
    public void uniform(@NotNull String name, int value) {
        integers(name, "int", value);
    }

    @Override
    public void uniform(@NotNull String name, int x, int y) {
        integers(name, "ivec2", x, y);
    }

    @Override
    public void uniform(@NotNull String name, int x, int y, int z) {
        integers(name, "ivec3", x, y, z);
    }

    @Override
    public void uniform(@NotNull String name, int x, int y, int z, int w) {
        integers(name, "ivec4", x, y, z, w);
    }

    @Override
    public void uniform(@NotNull String name, float value) {
        floats(name, "float", value);
    }

    @Override
    public void uniform(@NotNull String name, float x, float y) {
        floats(name, "vec2", x, y);
    }

    @Override
    public void uniform(@NotNull String name, float x, float y, float z) {
        floats(name, "vec3", x, y, z);
    }

    @Override
    public void uniform(@NotNull String name, float x, float y, float z, float w) {
        floats(name, "vec4", x, y, z, w);
    }

    @Override
    public void uniform(@NotNull String name, @NotNull Matrix2fc value) {
        Matrix2f copy = new Matrix2f(value);
        put(name, "mat2", (shader, uniformName) -> {
            Uniform uniform = shader.getUniform(uniformName);
            if (uniform != null) uniform.setMat2x2(copy.m00(), copy.m01(), copy.m10(), copy.m11());
        });
    }

    @Override
    public void uniform(@NotNull String name, @NotNull Matrix3fc value) {
        Matrix3f copy = new Matrix3f(value);
        put(name, "mat3", (shader, uniformName) -> set(shader, uniformName, copy));
    }

    @Override
    public void uniform(@NotNull String name, @NotNull Matrix4fc value) {
        Matrix4f copy = new Matrix4f(value);
        put(name, "mat4", (shader, uniformName) -> set(shader, uniformName, copy));
    }

    @Override
    public void uniform(@NotNull String name, @NotNull Location texture) {
        put(name, "sampler2D", (shader, uniformName) -> {
            if (texture.equals(MinecraftShaderRenderer.SCENE_TEXTURE)) {
                shader.setSampler(uniformName, renderer.sceneTexture());
                return;
            }
            ResourceLocation resource = ResourceLocation.fromNamespaceAndPath(texture.namespace(), texture.path());
            AbstractTexture minecraftTexture = Minecraft.getInstance().getTextureManager().getTexture(resource);
            shader.setSampler(uniformName, minecraftTexture);
        });
    }

    private void integers(@NotNull String name, @NotNull String type, int @NotNull ... data) {
        put(name, type, (shader, uniformName) -> {
            Uniform uniform = shader.getUniform(uniformName);
            if (uniform == null) return;
            switch (data.length) {
                case 1 -> uniform.set(data[0]);
                case 2 -> uniform.set(data[0], data[1]);
                case 3 -> uniform.set(data[0], data[1], data[2]);
                case 4 -> uniform.set(data[0], data[1], data[2], data[3]);
                default -> throw new IllegalStateException("Invalid integer uniform size");
            }
        });
    }

    private void floats(@NotNull String name, @NotNull String type, float @NotNull ... data) {
        put(name, type, (shader, uniformName) -> {
            Uniform uniform = shader.getUniform(uniformName);
            if (uniform == null) return;
            switch (data.length) {
                case 1 -> uniform.set(data[0]);
                case 2 -> uniform.set(data[0], data[1]);
                case 3 -> uniform.set(data[0], data[1], data[2]);
                case 4 -> uniform.set(data[0], data[1], data[2], data[3]);
                default -> throw new IllegalStateException("Invalid float uniform size");
            }
        });
    }

    private void put(@NotNull String uniformName, @NotNull String suppliedType, @NotNull UniformValue value) {
        requireOpen();
        String declaredType = uniformTypes.get(uniformName);
        if (declaredType == null) throw new IllegalArgumentException("Shader has no uniform named " + uniformName);
        if (!declaredType.equals(suppliedType)) {
            throw new IllegalArgumentException(
                    "Uniform " + uniformName + " is " + declaredType + ", not " + suppliedType
            );
        }
        values.put(uniformName, value);
    }

    private void requireBuiltinType(@NotNull String name, @NotNull String expectedType) {
        String declaredType = uniformTypes.get(name);
        if (declaredType != null && !declaredType.equals(expectedType)) {
            throw new IllegalArgumentException("Builtin uniform " + name + " must be declared as " + expectedType);
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Shader is closed");
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        variants.values().forEach(ShaderInstance::close);
        variants.clear();
        values.clear();
    }

    private static @NotNull Map<String, String> discoverUniforms(@NotNull ShaderSource source) {
        Map<String, String> result = new LinkedHashMap<>();
        discoverUniforms(source.vertexSource(), result);
        discoverUniforms(source.fragmentSource(), result);
        return Map.copyOf(result);
    }

    private static void discoverUniforms(@NotNull String source, @NotNull Map<String, String> result) {
        String uncommented = LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(source).replaceAll("")).replaceAll("");
        Matcher matcher = UNIFORM.matcher(uncommented);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if (matcher.group(3) != null)
                throw new IllegalArgumentException("Uniform arrays are not supported yet: " + name);
            String previous = result.putIfAbsent(name, type);
            if (previous != null && !previous.equals(type)) {
                throw new IllegalArgumentException("Uniform " + name + " has conflicting shader-stage types");
            }
        }
    }

    private static @NotNull String minecraftType(@NotNull String type) {
        return switch (type) {
            case "bool", "bvec2", "bvec3", "bvec4", "int", "ivec2", "ivec3", "ivec4" -> "int";
            case "float", "vec2", "vec3", "vec4" -> "float";
            case "mat2" -> "matrix2x2";
            case "mat3" -> "matrix3x3";
            case "mat4" -> "matrix4x4";
            case "sampler2D" -> "sampler";
            default -> throw new IllegalArgumentException("Unsupported GLSL uniform type: " + type);
        };
    }

    private static int componentCount(@NotNull String type) {
        return switch (type) {
            case "bool", "int", "float" -> 1;
            case "bvec2", "ivec2", "vec2" -> 2;
            case "bvec3", "ivec3", "vec3" -> 3;
            case "bvec4", "ivec4", "vec4", "mat2" -> 4;
            case "mat3" -> 9;
            case "mat4" -> 16;
            default -> throw new IllegalArgumentException("Unsupported non-sampler GLSL uniform type: " + type);
        };
    }

    private static void set(@NotNull ShaderInstance shader, @NotNull String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void set(@NotNull ShaderInstance shader, @NotNull String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void set(@NotNull ShaderInstance shader, @NotNull String name, @NotNull Matrix3f value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void set(@NotNull ShaderInstance shader, @NotNull String name, @NotNull Matrix4f value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static @NotNull ResourceLocation shaderResource(@NotNull String name, @NotNull String extension) {
        return ResourceLocation.withDefaultNamespace("shaders/core/" + name + extension);
    }

    private static byte @NotNull [] minecraftSource(@NotNull String source) {
        return bytes(VERSION.matcher(source).find() ? source : "#version 150\n\n" + source);
    }

    private static @NotNull String mapVertexAttributes(@NotNull String source) {
        String mapped = source;
        for (Map.Entry<String, String> attribute : ATTRIBUTE_NAMES.entrySet()) {
            mapped = mapped.replaceAll("\\b" + Pattern.quote(attribute.getKey()) + "\\b", attribute.getValue());
        }
        return mapped;
    }

    private static byte @NotNull [] bytes(@NotNull String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface UniformValue {
        void apply(@NotNull ShaderInstance shader, @NotNull String name);
    }

}
