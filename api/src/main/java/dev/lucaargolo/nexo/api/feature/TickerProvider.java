package dev.lucaargolo.nexo.api.feature;

import org.jetbrains.annotations.Nullable;

public interface TickerProvider<U> {

    default @Nullable Ticker<U> ticker() {
        return null;
    }

}
