package dev.lucaargolo.nexo.api.render.image.loader;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.image.Image;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import javax.imageio.spi.IIORegistry;
import java.util.List;
import java.util.Locale;

public final class WebpImageLoader implements ImageLoader {

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
    }

    @Override
    public boolean supports(@NotNull Location path) {
        return path.path().toLowerCase(Locale.ROOT).endsWith(".webp");
    }

    @Override
    public @NotNull List<@NotNull String> extensions() {
        return List.of(".webp");
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
