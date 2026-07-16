package dev.lucaargolo.nexo.api.render.util;

import org.jetbrains.annotations.NotNull;

public enum VertexFormat {
    POSITION(3),
    POSITION_COLOR(7),
    POSITION_TEX(5),
    POSITION_COLOR_TEX(9),
    POSITION_TEX_NORMAL(8),
    POSITION_COLOR_TEX_NORMAL(12);

    private final int stride;

    VertexFormat(@NotNull int stride) {
        this.stride = stride;
    }

    public int stride() {
        return stride;
    }
}
