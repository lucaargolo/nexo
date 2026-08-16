package dev.lucaargolo.nexo.api.render.image.loader;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.image.Image;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class JpegImageLoader implements ImageLoader {

    @Override
    public boolean supports(@NotNull Location path) {
        String value = path.path().toLowerCase(Locale.ROOT);
        return value.endsWith(".jpg") || value.endsWith(".jpeg");
    }

    @Override
    public @NotNull List<@NotNull String> extensions() {
        return List.of(".jpg", ".jpeg");
    }

    @Override
    public @NotNull Image load(
            @NotNull Nexo nexo,
            @NotNull Location path,
            byte @NotNull [] data
    ) throws Exception {
        return ImageIoDecoder.convertToPng(data);
    }

}
