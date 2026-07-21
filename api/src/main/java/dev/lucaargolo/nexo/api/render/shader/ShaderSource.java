package dev.lucaargolo.nexo.api.render.shader;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record ShaderSource(
        @NotNull String vertexSource,
        @NotNull String fragmentSource
) {

    public ShaderSource {
        Objects.requireNonNull(vertexSource, "vertexSource");
        Objects.requireNonNull(fragmentSource, "fragmentSource");
    }

}
