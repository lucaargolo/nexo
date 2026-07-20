package dev.lucaargolo.nexo.api.render.shader;

import org.jetbrains.annotations.NotNull;

/**
 * Stable GLSL identifiers supplied or understood by every Nexo rendering
 * backend. Uniform names follow ShaderToy's {@code i*} convention; vertex
 * inputs use semantic {@code a*} names and are mapped to backend attributes.
 */
public final class ShaderBuiltins {

    public static final @NotNull String POSITION = "aPosition";
    public static final @NotNull String COLOR = "aColor";
    public static final @NotNull String TEX_COORD_0 = "aTexCoord0";
    public static final @NotNull String NORMAL = "aNormal";

    /** {@code mat4}: transforms Nexo vertex positions into view space. */
    public static final @NotNull String MODEL_VIEW = "iModelView";
    /** {@code mat4}: transforms view-space positions into clip space. */
    public static final @NotNull String PROJECTION = "iProjection";
    /** {@code vec3}: viewport width, height, and pixel aspect ratio. */
    public static final @NotNull String RESOLUTION = "iResolution";
    /** {@code float}: seconds elapsed since the renderer started. */
    public static final @NotNull String TIME = "iTime";
    /** {@code float}: seconds elapsed since the previous rendered frame. */
    public static final @NotNull String TIME_DELTA = "iTimeDelta";
    /** {@code int}: rendered frame index. */
    public static final @NotNull String FRAME = "iFrame";

    public static final @NotNull String CHANNEL_0 = "iChannel0";
    public static final @NotNull String CHANNEL_1 = "iChannel1";
    public static final @NotNull String CHANNEL_2 = "iChannel2";
    public static final @NotNull String CHANNEL_3 = "iChannel3";

    private ShaderBuiltins() {
    }

}
