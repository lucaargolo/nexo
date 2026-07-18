package dev.lucaargolo.nexo.api.render;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record Transform(
        @NotNull Vector3f rotation,
        @NotNull Vector3f translation,
        @NotNull Vector3f scale
) {}