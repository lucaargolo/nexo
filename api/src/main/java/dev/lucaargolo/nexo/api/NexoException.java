package dev.lucaargolo.nexo.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NexoException extends RuntimeException {

    public NexoException(@NotNull String message) {
        super(message);
    }

    public NexoException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public NexoException(@Nullable Throwable cause) {
        super(cause);
    }
}
