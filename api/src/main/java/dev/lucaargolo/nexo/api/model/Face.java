package dev.lucaargolo.nexo.api.model;

import dev.lucaargolo.nexo.api.util.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Face(@NotNull String texture, @Nullable Direction cullFace, float @Nullable [] uv, int rotation, int tintIndex) {

    public static Face simple(@NotNull String texture) {
        return new Face(texture, null, null, 0, -1);
    }

}
