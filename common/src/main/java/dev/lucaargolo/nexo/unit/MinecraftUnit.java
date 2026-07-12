package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import org.jetbrains.annotations.NotNull;

public interface MinecraftUnit<T> {

    @NotNull NexoMinecraft nexo();

    @NotNull T get();

}
