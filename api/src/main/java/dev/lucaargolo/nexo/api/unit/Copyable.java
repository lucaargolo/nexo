package dev.lucaargolo.nexo.api.unit;

import org.jetbrains.annotations.NotNull;

public interface Copyable<T> {

    @NotNull T copy();

}
