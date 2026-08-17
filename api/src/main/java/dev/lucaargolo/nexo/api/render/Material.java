package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.render.shader.Shader;
import dev.lucaargolo.nexo.api.render.util.*;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Objects;

public final class Material<T> {

    private final @Nullable Pair<Location, T> texture;
    private final float @NotNull [] color;
    private @NotNull CullMode cullMode;
    private @NotNull BlendMode blendMode;

    private @NotNull LayerMode layerMode;
    private @Nullable Shader shader;
    private @NotNull TextureFilter minFilter;
    private @NotNull TextureFilter magFilter;
    private @NotNull TextureWrap wrapS;
    private @NotNull TextureWrap wrapT;

    public Material(
            @Nullable Pair<Location, T> texture,
            float @NotNull [] color,
            @NotNull CullMode cullMode,
            @NotNull BlendMode blendMode,
            @NotNull LayerMode layerMode,
            @Nullable Shader shader,
            @NotNull TextureFilter minFilter,
            @NotNull TextureFilter magFilter,
            @NotNull TextureWrap wrapS,
            @NotNull TextureWrap wrapT
    ) {
        this.texture = texture;
        this.color = Objects.requireNonNull(color, "color").clone();
        if (this.color.length != 4) {
            throw new IllegalArgumentException("Material color must contain 4 values");
        }
        this.cullMode = Objects.requireNonNull(cullMode, "cullMode");
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
        this.layerMode = Objects.requireNonNull(layerMode, "layerMode");
        this.shader = shader;
        this.minFilter = Objects.requireNonNull(minFilter, "minFilter");
        this.magFilter = Objects.requireNonNull(magFilter, "magFilter");
        this.wrapS = Objects.requireNonNull(wrapS, "wrapS");
        this.wrapT = Objects.requireNonNull(wrapT, "wrapT");
    }

    public Material(@Nullable Pair<Location, T> texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode, @NotNull LayerMode layerMode) {
        this(texture, color, cullMode, blendMode, layerMode, null, TextureFilter.NEAREST, TextureFilter.NEAREST, TextureWrap.CLAMP, TextureWrap.CLAMP);
    }

    public Material(@Nullable Pair<Location, T> texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode) {
        this(texture, color, cullMode, blendMode, LayerMode.SOLID);
    }

    public Material(@NotNull Location location, @NotNull T texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode) {
        this(new Pair<>(location, texture), color, cullMode, blendMode);
    }

    public Material(@NotNull Location location, @NotNull T texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode, @NotNull LayerMode layerMode) {
        this(new Pair<>(location, texture), color, cullMode, blendMode, layerMode);
    }

    private Material(@NotNull Location location, @NotNull T texture) {
        this(location, texture, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, CullMode.BACK, BlendMode.DISABLED);
    }

    private Material() {
        this(null, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, CullMode.BACK, BlendMode.DISABLED);
    }

    public @Nullable Pair<Location, T> texture() {
        return texture;
    }

    public float @NotNull [] color() {
        return color.clone();
    }

    @ApiStatus.Internal
    public float @NotNull [] colorData() {
        return color;
    }

    public @NotNull CullMode cullMode() {
        return cullMode;
    }

    public @NotNull Material<T> withCullMode(@NotNull CullMode cullMode) {
        this.cullMode = Objects.requireNonNull(cullMode, "cullMode");
        return this;
    }

    public @NotNull BlendMode blendMode() {
        return blendMode;
    }

    public @NotNull Material<T> withBlendMode(@NotNull BlendMode blendMode) {
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
        return this;
    }

    public @NotNull LayerMode layerMode() {
        return layerMode;
    }

    public @NotNull Material<T> withLayerMode(@NotNull LayerMode layerMode) {
        this.layerMode = Objects.requireNonNull(layerMode, "layerMode");
        return this;
    }

    public @Nullable Shader shader() {
        return shader;
    }

    public @NotNull Material<T> withShader(@Nullable Shader shader) {
        this.shader = shader;
        return this;
    }

    public @NotNull TextureFilter minFilter() {
        return minFilter;
    }

    public @NotNull TextureFilter magFilter() {
        return magFilter;
    }

    public @NotNull Material<T> withTextureFilter(@NotNull TextureFilter minFilter, @NotNull TextureFilter magFilter) {
        this.minFilter = Objects.requireNonNull(minFilter, "minFilter");
        this.magFilter = Objects.requireNonNull(magFilter, "magFilter");
        return this;
    }

    public @NotNull TextureWrap wrapS() {
        return wrapS;
    }

    public @NotNull TextureWrap wrapT() {
        return wrapT;
    }

    public @NotNull Material<T> withTextureWrap(@NotNull TextureWrap wrapS, @NotNull TextureWrap wrapT) {
        this.wrapS = Objects.requireNonNull(wrapS, "wrapS");
        this.wrapT = Objects.requireNonNull(wrapT, "wrapT");
        return this;
    }

    public @Nullable Location location() {
        return texture != null ? texture.left() : null;
    }

    public @Nullable T data() {
        return texture != null ? texture.right() : null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Material<?>) obj;
        return Objects.equals(this.texture, that.texture) &&
                Arrays.equals(this.color, that.color) &&
                Objects.equals(this.cullMode, that.cullMode) &&
                Objects.equals(this.blendMode, that.blendMode) &&
                Objects.equals(this.layerMode, that.layerMode) &&
                Objects.equals(this.shader, that.shader) &&
                Objects.equals(this.minFilter, that.minFilter) &&
                Objects.equals(this.magFilter, that.magFilter) &&
                Objects.equals(this.wrapS, that.wrapS) &&
                Objects.equals(this.wrapT, that.wrapT);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture, Arrays.hashCode(color), cullMode, blendMode, layerMode, shader, minFilter, magFilter, wrapS, wrapT);
    }

    @Override
    public @NotNull String toString() {
        return "Material[" +
                "texture=" + texture + ", " +
                "color=" + Arrays.toString(color) + ", " +
                "cullMode=" + cullMode + ", " +
                "blendMode=" + blendMode +
                ", layerMode=" + layerMode +
                ", shader=" + shader +
                ", minFilter=" + minFilter +
                ", magFilter=" + magFilter +
                ", wrapS=" + wrapS +
                ", wrapT=" + wrapT + ']';
    }

    public static @NotNull Material<Location> of(@NotNull Location location) {
        return new Material<>(location, location);
    }

    public static @NotNull Material<Void> untextured() {
        return new Material<>();
    }

}
