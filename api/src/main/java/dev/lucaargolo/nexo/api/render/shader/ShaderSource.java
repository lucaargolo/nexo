package dev.lucaargolo.nexo.api.render.shader;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * GLSL source for a vertex/fragment shader program. Sources may omit a
 * {@code #version} directive so each rendering backend can select its
 * compatible GLSL target. Portable shaders should use the attributes and
 * uniforms defined by {@link ShaderBuiltins} instead of backend identifiers.
 */
public record ShaderSource(
        @NotNull String vertexSource,
        @NotNull String fragmentSource
) {

    public ShaderSource {
        Objects.requireNonNull(vertexSource, "vertexSource");
        Objects.requireNonNull(fragmentSource, "fragmentSource");
    }

}
