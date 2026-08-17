package dev.lucaargolo.nexo.api.render.font.loader;

import dev.lucaargolo.nexo.api.render.font.Glyph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class AwtGlyphRasterizer {

    private static final @NotNull FontRenderContext FONT_CONTEXT = new FontRenderContext(null, true, true);

    private static final float FONT_SIZE = 10.0F;
    private static final float OVERSAMPLE = 4.0F;

    @NotNull
    private final Font font;

    public AwtGlyphRasterizer(byte @NotNull [] data) throws IOException, FontFormatException {
        this.font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(data)).deriveFont(FONT_SIZE * OVERSAMPLE);
    }

    public @Nullable Glyph rasterize(int codePoint) {
        String text = new String(Character.toChars(codePoint));
        GlyphVector vector = font.createGlyphVector(FONT_CONTEXT, text);
        if (vector.getNumGlyphs() != 1 || vector.getGlyphCode(0) == font.getMissingGlyphCode()) {
            return null;
        }

        float advance = vector.getGlyphMetrics(0).getAdvanceX() / OVERSAMPLE;
        Rectangle bounds = vector.getGlyphPixelBounds(0, FONT_CONTEXT, 0.0F, 0.0F);
        if (bounds.width == 0 || bounds.height == 0) {
            return new Glyph(advance);
        }

        BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setColor(Color.WHITE);
            graphics.drawGlyphVector(vector, -bounds.x, -bounds.y);
        } finally {
            graphics.dispose();
        }

        byte[] luminance = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        return new Glyph(
                luminance,
                bounds.width,
                bounds.height,
                advance,
                bounds.x / OVERSAMPLE,
                -bounds.y / OVERSAMPLE,
                OVERSAMPLE
        );
    }

}