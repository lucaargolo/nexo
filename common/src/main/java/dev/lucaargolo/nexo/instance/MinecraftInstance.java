package dev.lucaargolo.nexo.instance;

import dev.lucaargolo.nexo.NexoMinecraft;
import org.jetbrains.annotations.NotNull;

public interface MinecraftInstance<T> {

    @NotNull NexoMinecraft nexo();

    @NotNull T get();

}
