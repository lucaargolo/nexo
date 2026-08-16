package dev.lucaargolo.nexo.render;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

public final class MinecraftFont {

    private static final Map<Location, Font> REGISTERED_FONTS = new ConcurrentHashMap<>();
    private static final Map<Location, GlyphProvider> REGISTERED_PROVIDERS = new ConcurrentHashMap<>();

    private static volatile FontManager activeFontManager;

    private MinecraftFont() {
    }

    public static void register(Location location, Font font) {
        REGISTERED_FONTS.put(location, font);
        installInActiveFontManager(location);
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
        GlyphProvider provider = new LinearFilteringGlyphProvider(new ParsedGlyphProvider(font));
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

    private record ParsedGlyphProvider(Font font) implements GlyphProvider {

        @Override
        public @Nullable GlyphInfo getGlyph(int character) {
            Font.Glyph glyph = font.glyph(character);
            if (glyph == null) {
                return null;
            }
            if (!glyph.visible()) {
                return (GlyphInfo.SpaceGlyphInfo) glyph::advance;
            }
            return new ParsedGlyphInfo(glyph);
        }

        @Override
        public IntSet getSupportedGlyphs() {
            IntSet supported = new IntOpenHashSet();
            for (int codePoint : font.supportedGlyphs()) {
                supported.add(codePoint);
            }
            return supported;
        }

    }

    private record ParsedGlyphInfo(Font.Glyph glyph) implements GlyphInfo {

        @Override
        public float getAdvance() {
            return glyph.advance();
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> stitcher) {
            return stitcher.apply(new ParsedSheetGlyphInfo(glyph));
        }

    }

    private record ParsedSheetGlyphInfo(Font.Glyph glyph) implements SheetGlyphInfo {

        @Override
        public int getPixelWidth() {
            return glyph.width();
        }

        @Override
        public int getPixelHeight() {
            return glyph.height();
        }

        @Override
        public void upload(int xOffset, int yOffset) {
            NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, glyph.width(), glyph.height(), false);
            for (int y = 0; y < glyph.height(); y++) {
                for (int x = 0; x < glyph.width(); x++) {
                    image.setPixelLuminance(x, y, glyph.luminance(x, y));
                }
            }
            image.upload(0, xOffset, yOffset, 0, 0, glyph.width(), glyph.height(), false, true);
        }

        @Override
        public boolean isColored() {
            return false;
        }

        @Override
        public float getOversample() {
            return glyph.oversample();
        }

        @Override
        public float getBearingLeft() {
            return glyph.bearingLeft();
        }

        @Override
        public float getBearingTop() {
            return glyph.bearingTop();
        }

    }

    private record LinearFilteringGlyphProvider(GlyphProvider delegate) implements GlyphProvider {

        @Override
        public @Nullable GlyphInfo getGlyph(int character) {
            GlyphInfo glyph = delegate.getGlyph(character);
            if (glyph == null || glyph instanceof GlyphInfo.SpaceGlyphInfo) {
                return glyph;
            }
            return new LinearFilteringGlyphInfo(glyph);
        }

        @Override
        public IntSet getSupportedGlyphs() {
            return delegate.getSupportedGlyphs();
        }

        @Override
        public void close() {
            delegate.close();
        }

    }

    private record LinearFilteringGlyphInfo(GlyphInfo delegate) implements GlyphInfo {

        @Override
        public float getAdvance() {
            return delegate.getAdvance();
        }

        @Override
        public float getBoldOffset() {
            return delegate.getBoldOffset();
        }

        @Override
        public float getShadowOffset() {
            return delegate.getShadowOffset();
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> stitcher) {
            return delegate.bake(glyph -> stitcher.apply(new LinearFilteringSheetGlyphInfo(glyph)));
        }

    }

    private record LinearFilteringSheetGlyphInfo(SheetGlyphInfo delegate) implements SheetGlyphInfo {

        @Override
        public int getPixelWidth() {
            return delegate.getPixelWidth();
        }

        @Override
        public int getPixelHeight() {
            return delegate.getPixelHeight();
        }

        @Override
        public void upload(int xOffset, int yOffset) {
            delegate.upload(xOffset, yOffset);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }

        @Override
        public boolean isColored() {
            return delegate.isColored();
        }

        @Override
        public float getOversample() {
            return delegate.getOversample();
        }

        @Override
        public float getLeft() {
            return delegate.getLeft();
        }

        @Override
        public float getRight() {
            return delegate.getRight();
        }

        @Override
        public float getTop() {
            return delegate.getTop();
        }

        @Override
        public float getBottom() {
            return delegate.getBottom();
        }

        @Override
        public float getBearingLeft() {
            return delegate.getBearingLeft();
        }

        @Override
        public float getBearingTop() {
            return delegate.getBearingTop();
        }

    }

}
