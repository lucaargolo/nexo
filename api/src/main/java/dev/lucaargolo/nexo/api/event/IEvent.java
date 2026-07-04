package dev.lucaargolo.nexo.api.event;

import org.jetbrains.annotations.Nullable;

public interface IEvent<T> {

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
