package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class Renderer<G extends Graphics2D, U> {

    public abstract void render(@NotNull G g, @NotNull U u);

    public abstract @NotNull Map<String, Location> textures();

    public @Nullable Location texture(@NotNull String name) {
        Map<String, Location> textures = textures();
        return textures.get(name);
    }

    public abstract @NotNull Transform transform(@NotNull Location location);

    public boolean shaded() {
        return true;
    }

}
