package dev.lucaargolo.nexo.api.render;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface StaticRenderer<G extends Graphics2D, U> extends Renderer<G, U> {

    @NotNull List<@NotNull DrawCall<G>> calls(@NotNull U unit);

    @Override
    default void render(@NotNull U unit, @NotNull G graphics) {
        for (DrawCall<G> call : calls(unit)) {
            call.execute(graphics);
        }
    }
}
