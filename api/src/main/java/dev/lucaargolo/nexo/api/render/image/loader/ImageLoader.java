package dev.lucaargolo.nexo.api.render.image.loader;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.image.Image;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ImageLoader {

    boolean supports(@NotNull Location path);

    default @NotNull List<@NotNull String> extensions() {
        return List.of();
    }

    default @NotNull List<@NotNull Location> resolve(@NotNull Location path) {
        if (supports(path)) {
            return List.of(path);
        }

        int slash = path.path().lastIndexOf('/');
        if (path.path().indexOf('.', slash + 1) >= 0) {
            return List.of();
        }
        return extensions().stream().map(path::withPathSuffix).toList();
    }

    @NotNull Image load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) throws Exception;

}
