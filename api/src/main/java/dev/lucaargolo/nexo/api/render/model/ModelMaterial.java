package dev.lucaargolo.nexo.api.render.model;

import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.CullMode;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Material state shared by one or more model meshes.
 */
public final class ModelMaterial {

    private final @Nullable Location texture;
    private final float @NotNull [] color;
    private final @NotNull CullMode cullMode;
    private final @NotNull BlendMode blendMode;

    public ModelMaterial(
            @Nullable Location texture,
            float @NotNull [] color,
            @NotNull CullMode cullMode,
            @NotNull BlendMode blendMode
    ) {
        Objects.requireNonNull(color, "color");
        if (color.length != 4) {
            throw new IllegalArgumentException("Material color must contain 4 values");
        }
        this.texture = texture;
        this.color = color.clone();
        this.cullMode = Objects.requireNonNull(cullMode, "cullMode");
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
    }

    public ModelMaterial(@Nullable Location texture) {
        this(texture, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, CullMode.BACK, BlendMode.DISABLED);
    }

    public @Nullable Location texture() {
        return texture;
    }

    public float @NotNull [] color() {
        return color.clone();
    }

    float @NotNull [] colorData() {
        return color;
    }

    public @NotNull CullMode cullMode() {
        return cullMode;
    }

    public @NotNull BlendMode blendMode() {
        return blendMode;
    }
}
