package dev.lucaargolo.nexo.api.event;

import org.jetbrains.annotations.Nullable;

public interface Event<T> {

    @Nullable T value();

    boolean cancelable();

    enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

}
