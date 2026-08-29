package dev.lucaargolo.nexo.render.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.render.font.Font;
import dev.lucaargolo.nexo.api.util.Location;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

public final class MinecraftFont {

    private static final float MINECRAFT_FONT_SIZE = 9.0F;
    // Vanilla ASCII lowercase glyphs occupy five pixels inside the nine-pixel line.
    private static final float MINECRAFT_X_HEIGHT = 5.0F;
    private static final float OVERSAMPLE = 4.0F;

    private static final Map<Location, Font> REGISTERED_FONTS = new ConcurrentHashMap<>();
    private static final Map<Location, GlyphProvider> REGISTERED_PROVIDERS = new ConcurrentHashMap<>();

    private static volatile FontManager activeFontManager;

    private MinecraftFont() {
    }

    public static void register(Location location, Font font) {
        REGISTERED_FONTS.put(location, font);
        installInActiveFontManager(location);
    }

    public static boolean useLinearFiltering(ResourceLocation fontId) {
        // FontSet stores each registered font atlas as <font-id>/<texture-index>.
        String path = fontId.getPath();
        Font matchedFont = null;
        int matchedPathLength = -1;
        for (Map.Entry<Location, Font> entry : REGISTERED_FONTS.entrySet()) {
            Location location = entry.getKey();
            if (location.namespace().equals(fontId.getNamespace())
                    && (path.equals(location.path()) || path.startsWith(location.path() + "/"))
                    && location.path().length() > matchedPathLength) {
                matchedFont = entry.getValue();
                matchedPathLength = location.path().length();
            }
        }
        return matchedFont != null && matchedFont.linearFiltering();
    }

    public static GlyphRenderTypes linearFilteringRenderTypes(GlyphRenderTypes renderTypes) {
        return new GlyphRenderTypes(
                linearFilteringRenderType(renderTypes.normal()),
                linearFilteringRenderType(renderTypes.seeThrough()),
                linearFilteringRenderType(renderTypes.polygonOffset())
        );
    }

    private static RenderType linearFilteringRenderType(RenderType delegate) {
        // Vanilla text render types force nearest filtering during setup.
        return new RenderType(
                "nexo_linear_" + delegate,
                delegate.format(),
                delegate.mode(),
                delegate.bufferSize(),
                delegate.affectsCrumbling(),
                delegate.sortOnUpload(),
                () -> {
                    delegate.setupRenderState();
                    GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                },
                delegate::clearRenderState
        ) {
        };
    }

    public static void reloadRegisteredFonts(@NotNull FontManager fontManager) {
        activeFontManager = fontManager;
        REGISTERED_PROVIDERS.clear();
        REGISTERED_FONTS.forEach((location, font) -> {
            try {
                installFont(fontManager, location, font);
            } catch (Exception e) {
                NexoMinecraft.LOGGER.error("Failed to register font {} during Minecraft font reload", location, e);
            }
        });
    }

    private static void installInActiveFontManager(Location location) {
        FontManager fontManager = activeFontManager;
        if (fontManager == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            installCurrentFont(fontManager, location);
        } else {
            minecraft.execute(() -> {
                try {
                    installCurrentFont(fontManager, location);
                } catch (Exception e) {
                    NexoMinecraft.LOGGER.error("Failed to register font {} in Minecraft", location, e);
                }
            });
        }
    }

    private static void installCurrentFont(FontManager fontManager, Location location) {
        Font font = REGISTERED_FONTS.get(location);
        if (font == null) {
            return;
        }
        installFont(fontManager, location, font);
    }

    private static void installFont(FontManager fontManager, Location location, Font font) {
        ResourceLocation fontId = NexoMinecraft.rl(location);
        GlyphProvider provider = createProvider(fontId, font);
        FontSet fontSet = new FontSet(fontManager.textureManager, fontId);
        boolean installed = false;

        try {
            fontSet.reload(List.of(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS)), Set.of());

            FontSet previousFontSet = fontManager.fontSets.put(fontId, fontSet);
            GlyphProvider previousProvider = REGISTERED_PROVIDERS.put(location, provider);
            fontManager.providersToClose.add(provider);
            fontManager.lastFontSetCache = null;
            installed = true;
            NexoMinecraft.LOGGER.debug("Registered font {} with Minecraft font manager", location);

            if (previousFontSet != null) {
                previousFontSet.close();
            }
            if (previousProvider != null) {
                fontManager.providersToClose.remove(previousProvider);
                previousProvider.close();
            }
        } finally {
            if (!installed) {
                fontSet.close();
                provider.close();
            }
        }
    }

    private static GlyphProvider createProvider(ResourceLocation fontId, Font font) {
        if (font.trueTypeOutlines()) {
            try {
                return new StbGlyphProvider(font);
            } catch (IllegalArgumentException e) {
                NexoMinecraft.LOGGER.debug("STB could not load font {}, falling back to Minecraft FreeType", fontId, e);
            }
        }

        return loadFreeTypeProvider(fontId, font.vectorData());
    }

    private static GlyphProvider loadFreeTypeProvider(ResourceLocation fontId, byte @NotNull [] data) {
        ByteBuffer nativeData = MemoryUtil.memAlloc(data.length);
        nativeData.put(data).flip();
        FT_Face face = null;
        try {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer facePointer = stack.mallocPointer(1);
                    FreeTypeUtil.assertError(
                            FreeType.FT_New_Memory_Face(
                                    FreeTypeUtil.getLibrary(),
                                    nativeData,
                                    0,
                                    facePointer
                            ),
                            "Initializing font face"
                    );
                    face = FT_Face.create(facePointer.get(0));
                    FreeTypeUtil.assertError(
                            FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE),
                            "Finding Unicode charmap"
                    );
                    float fontSize = normalizedFreeTypeFontSize(face);
                    return new com.mojang.blaze3d.font.TrueTypeGlyphProvider(
                            nativeData,
                            face,
                            fontSize,
                            OVERSAMPLE,
                            0.0F,
                            0.0F,
                            ""
                    );
                }
            }
        } catch (RuntimeException e) {
            if (face != null) {
                synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                    FreeType.FT_Done_Face(face);
                }
            }
            MemoryUtil.memFree(nativeData);
            throw new IllegalArgumentException("Could not load font " + fontId + " with FreeType", e);
        } catch (Error e) {
            if (face != null) {
                synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                    FreeType.FT_Done_Face(face);
                }
            }
            MemoryUtil.memFree(nativeData);
            throw e;
        }
    }

    private static final class StbGlyphProvider implements GlyphProvider {

        private final @NotNull IntSet supportedGlyphs;
        private final float scale;
        private @Nullable ByteBuffer fontData;
        private @Nullable STBTTFontinfo fontInfo;

        private StbGlyphProvider(@NotNull Font font) {
            byte[] data = font.vectorData();
            ByteBuffer nativeData = MemoryUtil.memAlloc(data.length);
            STBTTFontinfo info = null;
            try {
                nativeData.put(data).flip();
                // create() wraps a BufferUtils-owned buffer; only malloc() may be freed explicitly.
                info = STBTTFontinfo.malloc();
                if (!STBTruetype.stbtt_InitFont(info, nativeData)) {
                    throw new IllegalArgumentException("STB rejected the TrueType vector data");
                }

                IntSet supported = new IntOpenHashSet();
                for (int codePoint : font.supportedGlyphs()) {
                    supported.add(codePoint);
                }
                this.supportedGlyphs = IntSets.unmodifiable(supported);
                this.scale = normalizedStbScale(info);
                this.fontData = nativeData;
                this.fontInfo = info;
            } catch (RuntimeException | Error e) {
                if (info != null) {
                    info.free();
                }
                MemoryUtil.memFree(nativeData);
                throw e;
            }
        }

        private static float normalizedStbScale(@NotNull STBTTFontinfo info) {
            float xHeight = unscaledStbGlyphTop(info, 'x');
            if (xHeight <= 0.0F) {
                float capHeight = unscaledStbGlyphTop(info, 'H');
                if (capHeight <= 0.0F) {
                    return STBTruetype.stbtt_ScaleForPixelHeight(info, MINECRAFT_FONT_SIZE * OVERSAMPLE);
                }
                return 7.0F * OVERSAMPLE / capHeight;
            }
            return MINECRAFT_X_HEIGHT * OVERSAMPLE / xHeight;
        }

        @Override
        public @Nullable GlyphInfo getGlyph(int character) {
            STBTTFontinfo info = requireFontInfo();
            if (!supportedGlyphs.contains(character) || STBTruetype.stbtt_FindGlyphIndex(info, character) == 0) {
                return null;
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer advanceBuffer = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointHMetrics(info, character, advanceBuffer, null);

                IntBuffer x0Buffer = stack.mallocInt(1);
                IntBuffer y0Buffer = stack.mallocInt(1);
                IntBuffer x1Buffer = stack.mallocInt(1);
                IntBuffer y1Buffer = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointBitmapBox(
                        info,
                        character,
                        scale,
                        scale,
                        x0Buffer,
                        y0Buffer,
                        x1Buffer,
                        y1Buffer
                );

                int width = x1Buffer.get(0) - x0Buffer.get(0);
                int height = y1Buffer.get(0) - y0Buffer.get(0);
                float advance = advanceBuffer.get(0) * scale / OVERSAMPLE;
                if (width <= 0 || height <= 0) {
                    return (GlyphInfo.SpaceGlyphInfo) () -> advance;
                }
                return new StbGlyphInfo(
                        this,
                        character,
                        width,
                        height,
                        advance,
                        x0Buffer.get(0) / OVERSAMPLE,
                        -y0Buffer.get(0) / OVERSAMPLE
                );
            }
        }

        @Override
        public @NotNull IntSet getSupportedGlyphs() {
            return supportedGlyphs;
        }

        private @NotNull STBTTFontinfo requireFontInfo() {
            STBTTFontinfo info = fontInfo;
            if (info == null) {
                throw new IllegalStateException("Font provider already closed");
            }
            return info;
        }

        private void upload(int codePoint, int xOffset, int yOffset, int width, int height) {
            STBTTFontinfo info = requireFontInfo();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer bitmapWidth = stack.mallocInt(1);
                IntBuffer bitmapHeight = stack.mallocInt(1);
                ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(
                        info,
                        scale,
                        scale,
                        codePoint,
                        bitmapWidth,
                        bitmapHeight,
                        null,
                        null
                );
                if (bitmap == null) {
                    throw new IllegalStateException(
                            "STB returned no bitmap for code point U+" + Integer.toHexString(codePoint)
                    );
                }
                try {
                    if (bitmapWidth.get(0) != width || bitmapHeight.get(0) != height) {
                        throw new IllegalStateException("STB glyph bounds changed while baking");
                    }
                    NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, width, height, false);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            image.setPixelLuminance(x, y, bitmap.get(x + y * width));
                        }
                    }
                    // Keep atlas sampling bilinear without mipmaps; upload owns deferred image cleanup.
                    image.upload(0, xOffset, yOffset, 0, 0, width, height, true, false, false, true);
                } finally {
                    STBTruetype.stbtt_FreeBitmap(bitmap);
                }
            }
        }

        @Override
        public void close() {
            STBTTFontinfo info = fontInfo;
            fontInfo = null;
            if (info != null) {
                info.free();
            }
            ByteBuffer data = fontData;
            fontData = null;
            if (data != null) {
                MemoryUtil.memFree(data);
            }
        }

    }

    private static float normalizedFreeTypeFontSize(@NotNull FT_Face face) {
        float xHeight = unscaledFreeTypeGlyphTop(face, 'x');
        int unitsPerEm = Short.toUnsignedInt(face.units_per_EM());
        if (xHeight <= 0.0F) {
            float capHeight = unscaledFreeTypeGlyphTop(face, 'H');
            if (capHeight <= 0.0F || unitsPerEm <= 0) {
                return MINECRAFT_FONT_SIZE;
            }
            return 7.0F * unitsPerEm / capHeight;
        }
        if (unitsPerEm <= 0) {
            return MINECRAFT_FONT_SIZE;
        }
        return MINECRAFT_X_HEIGHT * unitsPerEm / xHeight;
    }

    private static float unscaledStbGlyphTop(@NotNull STBTTFontinfo info, int codePoint) {
        if (STBTruetype.stbtt_FindGlyphIndex(info, codePoint) == 0) {
            return 0.0F;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            if (!STBTruetype.stbtt_GetCodepointBox(info, codePoint, x0, y0, x1, y1)) {
                return 0.0F;
            }
            return y1.get(0);
        }
    }

    private static float unscaledFreeTypeGlyphTop(@NotNull FT_Face face, int codePoint) {
        long glyphIndex = FreeType.FT_Get_Char_Index(face, codePoint);
        if (glyphIndex == 0L) {
            return 0.0F;
        }

        try {
            FreeTypeUtil.assertError(
                    FreeType.FT_Load_Glyph(face, (int) glyphIndex, FreeType.FT_LOAD_NO_SCALE),
                    "Reading unscaled glyph metrics"
            );
            return face.glyph().metrics().horiBearingY();
        } catch (RuntimeException e) {
            return 0.0F;
        }
    }

    private record StbGlyphInfo(
            @NotNull StbGlyphProvider provider,
            int codePoint,
            int width,
            int height,
            float advance,
            float bearingLeft,
            float bearingTop
    ) implements GlyphInfo {

        @Override
        public float getAdvance() {
            return advance;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> stitcher) {
            return stitcher.apply(new StbSheetGlyphInfo(provider, codePoint, width, height, bearingLeft, bearingTop));
        }

    }

    private record StbSheetGlyphInfo(
            @NotNull StbGlyphProvider provider,
            int codePoint,
            int width,
            int height,
            float bearingLeft,
            float bearingTop
    ) implements SheetGlyphInfo {

        @Override
        public int getPixelWidth() {
            return width;
        }

        @Override
        public int getPixelHeight() {
            return height;
        }

        @Override
        public void upload(int xOffset, int yOffset) {
            provider.upload(codePoint, xOffset, yOffset, width, height);
        }

        @Override
        public boolean isColored() {
            return false;
        }

        @Override
        public float getOversample() {
            return OVERSAMPLE;
        }

        @Override
        public float getBearingLeft() {
            return bearingLeft;
        }

        @Override
        public float getBearingTop() {
            return bearingTop;
        }

    }

}
