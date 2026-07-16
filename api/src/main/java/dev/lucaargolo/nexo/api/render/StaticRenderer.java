package dev.lucaargolo.nexo.api.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class StaticRenderer<G extends Graphics2D, U> extends Renderer<G, U> {

    public abstract @NotNull List<@NotNull DrawCall<G>> calls(@Nullable U u);

    @Override
    public void render(@NotNull G g, @Nullable U u) {
        for (DrawCall<G> call : calls(u)) {
            call.execute(g);
        }
    }
}
