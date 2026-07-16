package dev.lucaargolo.nexo.api.render;

import org.jetbrains.annotations.NotNull;

public abstract class Renderer<G extends Graphics2D, U> {

    public abstract void render(@NotNull G g, @NotNull U u);

}
