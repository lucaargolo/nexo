package dev.lucaargolo.nexo.api.render.model.loader;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.model.Model;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ModelLoader {

    @Nullable
    public abstract Model tryLoad(@NotNull Nexo nexo, @NotNull Location path, @NotNull byte[] data);

}
