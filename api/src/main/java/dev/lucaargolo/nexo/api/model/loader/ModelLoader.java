package dev.lucaargolo.nexo.api.model.loader;

import dev.lucaargolo.nexo.api.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ModelLoader {

    @Nullable
    public abstract Model tryLoad(@NotNull String path, byte @NotNull [] data);

}
