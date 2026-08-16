package dev.lucaargolo.nexo.api.render.image.loader;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.image.Image;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class PngImageLoader implements ImageLoader {

    @Override
    public boolean supports(@NotNull Location path) {
        return path.path().toLowerCase(Locale.ROOT).endsWith(".png");
    }

    @Override
    public @NotNull List<@NotNull String> extensions() {
        return List.of(".png");
    }

    @Override
    public @NotNull Image load(
            @NotNull Nexo nexo,
            @NotNull Location path,
            byte @NotNull [] data
    ) throws Exception {
        ImageIoDecoder.read(data);
        return new Image(data);
    }

}
