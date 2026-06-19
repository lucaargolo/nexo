package dev.lucaargolo.nexo.api.model;

import dev.lucaargolo.nexo.api.util.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;

public record Cube(
    float fromX, float fromY, float fromZ,
    float toX, float toY, float toZ,
    @NotNull Map<Orientation, Face> faces,
    @Nullable Rotation rotation,
    boolean shade,
    boolean emissive
) {

    public Cube(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, @NotNull Map<Orientation, Face> faces) {
        this(fromX, fromY, fromZ, toX, toY, toZ, faces, null, true, false);
    }

    public Cube(
        float fromX, float fromY, float fromZ,
        float toX, float toY, float toZ,
        @NotNull Map<Orientation, Face> faces,
        @Nullable Rotation rotation,
        boolean shade,
        boolean emissive
    ) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
        this.faces = Collections.unmodifiableMap(faces);
        this.rotation = rotation;
        this.shade = shade;
        this.emissive = emissive;
    }

    /**
     * Element rotation supporting all three Minecraft model formats:
     * <ul>
     *   <li><b>Format 1 (1.9–1.21.5):</b> single-axis {@code axis} + {@code angle}</li>
     *   <li><b>Format 2 (1.21.6–1.21.10):</b> single-axis {@code axis} + {@code angle}, unbounded</li>
     *   <li><b>Format 3 (1.21.11+):</b> multi-axis {@code x}/{@code y}/{@code z} independent angles</li>
     * </ul>
     */
    public record Rotation(
        @NotNull Vector3f origin,
        @Nullable String axis,   // "x", "y", "z" — formats 1 & 2
        float angle,             // single-axis angle (formats 1 & 2; ignored when axis is null)
        @Nullable Float x,       // format 3: degrees around X
        @Nullable Float y,       // format 3: degrees around Y
        @Nullable Float z,       // format 3: degrees around Z
        boolean rescale
    ) {
        public static Rotation axisAngle(@NotNull Vector3f origin, @NotNull String axis, float angle, boolean rescale) {
            return new Rotation(new Vector3f(origin), axis, angle, null, null, null, rescale);
        }

        public static Rotation euler(@NotNull Vector3f origin, float x, float y, float z, boolean rescale) {
            return new Rotation(new Vector3f(origin), null, 0, x, y, z, rescale);
        }
    }
}
