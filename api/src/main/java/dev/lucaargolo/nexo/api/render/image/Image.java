package dev.lucaargolo.nexo.api.render.image;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.image.loader.ImageLoader;
import dev.lucaargolo.nexo.api.render.image.loader.JpegImageLoader;
import dev.lucaargolo.nexo.api.render.image.loader.PngImageLoader;
import dev.lucaargolo.nexo.api.render.image.loader.WebpImageLoader;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public record Image(byte @NotNull [] data) {

    private static final @NotNull List<ImageLoader> LOADERS = new CopyOnWriteArrayList<>();

    public Image(byte @NotNull [] data) {
        this.data = data.clone();
    }

    @Override
    public byte @NotNull [] data() {
        return data.clone();
    }

    public static @Nullable Image load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        for (ImageLoader loader : LOADERS) {
            if (!loader.supports(path)) {
                continue;
            }
            Image image = load(loader, nexo, path, data);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    public static @Nullable Image load(@NotNull Nexo nexo, @NotNull Location path) {
        Image image = loadResource(nexo, path);
        if (image != null) {
            return image;
        }

        if (!path.path().contains("textures/")) {
            image = loadResource(nexo, path.withPathPrefix("textures/"));
            if (image != null) {
                return image;
            }
        }

        nexo.getLogger().debug("Could not find image for location {}", path);
        return null;
    }

    private static @Nullable Image loadResource(@NotNull Nexo nexo, @NotNull Location path) {
        for (ImageLoader loader : LOADERS) {
            for (Location resolvedPath : loader.resolve(path)) {
                byte[] data = nexo.loadResource(resolvedPath);
                if (data == null) {
                    continue;
                }
                Image image = load(loader, nexo, resolvedPath, data);
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    private static @Nullable Image load(
            @NotNull ImageLoader loader,
            @NotNull Nexo nexo,
            @NotNull Location path,
            byte @NotNull [] data
    ) {
        try {
            return loader.load(nexo, path, data);
        } catch (Exception e) {
            nexo.getLogger().error("Failed to parse image {} with {}", path, loader.getClass().getSimpleName(), e);
            return null;
        }
    }

    public static void registerLoader(@NotNull ImageLoader loader) {
        LOADERS.add(loader);
    }

    static {
        registerLoader(new PngImageLoader());
        registerLoader(new JpegImageLoader());
        registerLoader(new WebpImageLoader());
    }

}
