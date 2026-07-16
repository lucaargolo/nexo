package dev.lucaargolo.nexo.api.render;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface DrawCall<G extends Graphics2D> {

    void execute(@NotNull G g);

}
