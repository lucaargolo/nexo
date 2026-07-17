package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class Renderer<G extends Graphics2D, U> {

    public abstract @NotNull Set<@NotNull Location> textures();

    public abstract void render(@NotNull G g, @NotNull U u);
}
