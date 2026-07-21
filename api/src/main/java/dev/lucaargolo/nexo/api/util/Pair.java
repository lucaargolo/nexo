package dev.lucaargolo.nexo.api.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Pair<A, B>(@NotNull A left, @NotNull B right) {

    public Pair(@NotNull A left, @NotNull B right) {
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
    }

}
