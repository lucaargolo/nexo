package dev.lucaargolo.nexo.api.feature;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Ticker<U> {

    void tick(@NotNull U unit);

}
