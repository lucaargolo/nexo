package dev.lucaargolo.nexo.api.render;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class StaticRenderer<G extends Graphics2D, U> extends Renderer<G, U> {

    public abstract @NotNull List<@NotNull DrawCall<G>> calls(@NotNull U unit);

    @Override
    public void render(@NotNull G g, @NotNull U unit) {
        for (DrawCall<G> call : calls(unit)) {
            call.execute(g);
        }
    }
}
