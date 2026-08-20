package dev.lucaargolo.nexo.api.render.font;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.render.font.loader.FontLoader;
import dev.lucaargolo.nexo.api.render.font.loader.OtfFontLoader;
import dev.lucaargolo.nexo.api.render.font.loader.TtfFontLoader;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Font {

    private static final @NotNull List<FontLoader> LOADERS = new CopyOnWriteArrayList<>();

    private final byte @NotNull [] vectorData;
    private final int @NotNull [] supportedGlyphs;
    private final boolean trueTypeOutlines;
    private final boolean linearFiltering;

    public Font(
            byte @NotNull [] vectorData,
            int @NotNull [] supportedGlyphs,
            boolean trueTypeOutlines
    ) {
        this(vectorData, supportedGlyphs, trueTypeOutlines, true);
    }

    public Font(
            byte @NotNull [] vectorData,
            int @NotNull [] supportedGlyphs,
            boolean trueTypeOutlines,
            boolean linearFiltering
    ) {
        if (vectorData.length == 0) {
            throw new IllegalArgumentException("Font vector data must not be empty");
        }
        this.supportedGlyphs = Arrays.stream(supportedGlyphs)
                .filter(Character::isValidCodePoint)
                .distinct()
                .sorted()
                .toArray();
        this.vectorData = vectorData.clone();
        this.trueTypeOutlines = trueTypeOutlines;
        this.linearFiltering = linearFiltering;
    }

    public Font(byte @NotNull [] vectorData, int @NotNull [] supportedGlyphs) {
        this(vectorData, supportedGlyphs, true);
    }

    public byte @NotNull [] vectorData() {
        return vectorData.clone();
    }

    public boolean trueTypeOutlines() {
        return trueTypeOutlines;
    }

    public boolean linearFiltering() {
        return linearFiltering;
    }

    public int @NotNull [] supportedGlyphs() {
        return supportedGlyphs.clone();
    }

    public static @Nullable Font load(@NotNull Nexo nexo, @NotNull Location path, byte @NotNull [] data) {
        for (FontLoader loader : LOADERS) {
            if (!loader.supports(path)) {
                continue;
            }
            Font font = load(loader, nexo, path, data);
            if (font != null) {
                return font;
            }
        }
        return null;
    }

    public static @Nullable Font load(@NotNull Nexo nexo, @NotNull Location path) {
        Font font = loadResource(nexo, path);
        if (font != null) {
            return font;
        }

        if (!path.path().contains("font/")) {
            font = loadResource(nexo, path.withPathPrefix("font/"));
            if (font != null) {
                return font;
            }
        }

        nexo.getLogger().debug("Could not find font for location {}", path);
        return null;
    }

    private static @Nullable Font loadResource(@NotNull Nexo nexo, @NotNull Location path) {
        for (FontLoader loader : LOADERS) {
            for (Location resolvedPath : loader.resolve(path)) {
                byte[] data = nexo.loadResource(resolvedPath);
                if (data == null) {
                    continue;
                }
                Font font = load(loader, nexo, resolvedPath, data);
                if (font != null) {
                    return font;
                }
            }
        }
        return null;
    }

    private static @Nullable Font load(
            @NotNull FontLoader loader,
            @NotNull Nexo nexo,
            @NotNull Location path,
            byte @NotNull [] data
    ) {
        try {
            return loader.load(nexo, path, data);
        } catch (Exception e) {
            nexo.getLogger().error("Failed to parse font {} with {}", path, loader.getClass().getSimpleName(), e);
            return null;
        }
    }

    public static void registerLoader(@NotNull FontLoader loader) {
        LOADERS.add(loader);
    }

    static {
        registerLoader(new TtfFontLoader());
        registerLoader(new OtfFontLoader());
    }

}
