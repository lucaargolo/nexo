package dev.lucaargolo.nexo.api.render.font;

import org.jetbrains.annotations.NotNull;

public final class Glyph {

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
