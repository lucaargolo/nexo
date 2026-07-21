package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.api.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Material<T>(@NotNull Pair<Location, T> texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode) {

    public Material(@NotNull Pair<Location, T> texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode) {
        this.texture = Objects.requireNonNull(texture, "texture");
        this.color = Objects.requireNonNull(color, "color");
        if (this.color.length != 4) {
            throw new IllegalArgumentException("Material color must contain 4 values");
        }
        this.cullMode = Objects.requireNonNull(cullMode, "cullMode");
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
    }

    public Material(@NotNull Location location, @NotNull T texture, float @NotNull [] color, @NotNull CullMode cullMode, @NotNull BlendMode blendMode) {
        this(new Pair<>(location, texture),  color, cullMode, blendMode);
    }

    public Material(@NotNull Location location, @NotNull T texture) {
        this(location, texture, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, CullMode.BACK, BlendMode.DISABLED);
    }

    public @NotNull Location location() {
        return texture.left();
    }

    public @NotNull T data() {
        return texture.right();
    }

}
