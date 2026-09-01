package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface Renderer<G extends Graphics2D, U> {

    void render(@NotNull U u, @NotNull G g);

    @NotNull Map<String, Material<?>> materials();

    default @Nullable Material<?> material(@NotNull String name) {
        return materials().get(name);
    }

    @NotNull Transform transform(@NotNull Location location);

    default boolean resolved() {
        return true;
    }

    default boolean shaded() {
        return true;
    }

}
