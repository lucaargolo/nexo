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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntFunction;

public final class Font {

    private static final @NotNull List<FontLoader> LOADERS = new CopyOnWriteArrayList<>();

    private final int @NotNull [] supportedGlyphs;
    private final @NotNull IntFunction<@Nullable Glyph> glyphLoader;
    private final @NotNull Map<Integer, Glyph> loadedGlyphs = new ConcurrentHashMap<>();

    public Font(int @NotNull [] supportedGlyphs, @NotNull IntFunction<@Nullable Glyph> glyphLoader) {
        this.supportedGlyphs = Arrays.stream(supportedGlyphs)
                .filter(Character::isValidCodePoint)
                .distinct()
                .sorted()
                .toArray();
        this.glyphLoader = glyphLoader;
    }

    public Font(@NotNull Map<Integer, Glyph> glyphs) {
        Map<Integer, Glyph> immutableGlyphs = Map.copyOf(glyphs);
        this.supportedGlyphs = immutableGlyphs.keySet().stream()
                .mapToInt(Integer::intValue)
                .filter(Character::isValidCodePoint)
                .sorted()
                .toArray();
        this.glyphLoader = immutableGlyphs::get;
        this.loadedGlyphs.putAll(immutableGlyphs);
    }

    public int @NotNull [] supportedGlyphs() {
        return supportedGlyphs.clone();
    }

    public @Nullable Glyph glyph(int codePoint) {
        if (Arrays.binarySearch(supportedGlyphs, codePoint) < 0) {
            return null;
        }
        return loadedGlyphs.computeIfAbsent(codePoint, glyphLoader::apply);
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

    public static final class Glyph {

        private final byte @NotNull [] luminance;
        private final int width;
        private final int height;
        private final float advance;
        private final float bearingLeft;
        private final float bearingTop;
        private final float oversample;

        public Glyph(
                byte @NotNull [] luminance,
                int width,
                int height,
                float advance,
                float bearingLeft,
                float bearingTop,
                float oversample
        ) {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Glyph dimensions must not be negative");
            }
            if (luminance.length != width * height) {
                throw new IllegalArgumentException("Glyph luminance length does not match its dimensions");
            }
            if (!Float.isFinite(advance) || !Float.isFinite(bearingLeft) || !Float.isFinite(bearingTop)) {
                throw new IllegalArgumentException("Glyph metrics must be finite");
            }
            if (!Float.isFinite(oversample) || oversample <= 0.0F) {
                throw new IllegalArgumentException("Glyph oversample must be finite and positive");
            }

            this.luminance = luminance.clone();
            this.width = width;
            this.height = height;
            this.advance = advance;
            this.bearingLeft = bearingLeft;
            this.bearingTop = bearingTop;
            this.oversample = oversample;
        }

        public Glyph(float advance) {
            this(new byte[0], 0, 0, advance, 0.0F, 0.0F, 1.0F);
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public float advance() {
            return advance;
        }

        public float bearingLeft() {
            return bearingLeft;
        }

        public float bearingTop() {
            return bearingTop;
        }

        public float oversample() {
            return oversample;
        }

        public byte luminance(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                throw new IndexOutOfBoundsException("Glyph pixel outside " + width + "x" + height);
            }
            return luminance[x + y * width];
        }

        public boolean visible() {
            return width > 0 && height > 0;
        }

    }

}
