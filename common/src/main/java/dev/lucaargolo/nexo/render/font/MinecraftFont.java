package dev.lucaargolo.nexo.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class MinecraftFont {

    public static final int ATLAS_SIZE = 32;
    private static final int ATLAS_WIDTH = 2048;
    private static final int ATLAS_HEIGHT = 1024;

    private final @NotNull ByteBuffer fontData;
    private final @NotNull STBTTFontinfo fontInfo;
    private final float scale;
    private final float ascent;

    private final @NotNull NativeImage atlas;
    private final @NotNull DynamicTexture texture;
    private final @NotNull ResourceLocation textureLocation;
    private final @NotNull Map<Integer, Glyph> glyphs = new HashMap<>();

    private int atlasX;
    private int atlasY;
    private int rowHeight;
    private boolean dirty;

    public MinecraftFont(@NotNull byte[] data, @NotNull Location location) {
        this.fontData = ByteBuffer.allocateDirect(data.length).put(data).flip();
        this.fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontData)) {
            throw new IllegalArgumentException("Could not initialize font from resource " + location + ": unsupported font file format");
        }
        this.scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, ATLAS_SIZE);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);
            this.ascent = ascent.get(0) * scale;
        }
        this.atlas = new NativeImage(NativeImage.Format.RGBA, ATLAS_WIDTH, ATLAS_HEIGHT, false);
        this.texture = new DynamicTexture(atlas);
        String suffix = Integer.toHexString(System.identityHashCode(this));
        this.textureLocation = NexoMinecraft.rl(Location.of("nexo", "dynamic/font_" + suffix));
        Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
    }

    public float ascent() {
        return ascent;
    }

    public @NotNull ResourceLocation textureLocation() {
        return textureLocation;
    }

    public void ensureGlyphs(@NotNull String text) {
        for (int i = 0; i < text.length(); ) {
            int codepoint = text.codePointAt(i);
            glyph(codepoint);
            i += Character.charCount(codepoint);
        }
    }

    public void upload() {
        if (dirty) {
            texture.upload();
            dirty = false;
        }
    }

    public float kern(int prevCodepoint, int codepoint) {
        return STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prevCodepoint, codepoint) * scale;
    }

    public float width(@NotNull String text, float scale) {
        float width = 0.0F;
        int prevCodepoint = 0;
        for (int i = 0; i < text.length(); ) {
            int codepoint = text.codePointAt(i);
            i += Character.charCount(codepoint);
            if (prevCodepoint != 0) {
                width += kern(prevCodepoint, codepoint) * scale;
            }
            width += glyph(codepoint).advance() * scale;
            prevCodepoint = codepoint;
        }
        return width;
    }

    public @NotNull Glyph glyph(int codepoint) {
        return glyphs.computeIfAbsent(codepoint, this::rasterize);
    }

    private @NotNull Glyph rasterize(int codepoint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceBuffer = stack.mallocInt(1);
            IntBuffer bearingBuffer = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codepoint, advanceBuffer, bearingBuffer);
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer xOffsetBuffer = stack.mallocInt(1);
            IntBuffer yOffsetBuffer = stack.mallocInt(1);
            ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(
                    fontInfo, scale, scale, codepoint, widthBuffer, heightBuffer, xOffsetBuffer, yOffsetBuffer
            );
            try {
                int width = widthBuffer.get(0);
                int height = heightBuffer.get(0);
                float advance = advanceBuffer.get(0) * scale;
                if (bitmap == null || width == 0 || height == 0) {
                    return Glyph.blank(advance);
                }
                if (atlasX + width + 1 > ATLAS_WIDTH) {
                    atlasX = 0;
                    atlasY += rowHeight + 1;
                    rowHeight = 0;
                }
                if (atlasY + height + 1 > ATLAS_HEIGHT) {
                    NexoMinecraft.LOGGER.warn("Font atlas is full, cannot rasterize codepoint {}", codepoint);
                    return Glyph.blank(advance);
                }
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int alpha = Byte.toUnsignedInt(bitmap.get(y * width + x));
                        atlas.setPixelRGBA(atlasX + x, atlasY + y, (alpha << 24) | 0xFFFFFF);
                    }
                }
                float u0 = (float) atlasX / ATLAS_WIDTH;
                float v0 = (float) atlasY / ATLAS_HEIGHT;
                float u1 = (float) (atlasX + width) / ATLAS_WIDTH;
                float v1 = (float) (atlasY + height) / ATLAS_HEIGHT;
                Glyph glyph = new Glyph(u0, v0, u1, v1, xOffsetBuffer.get(0), yOffsetBuffer.get(0), width, height, advance);
                atlasX += width + 1;
                rowHeight = Math.max(rowHeight, height + 1);
                dirty = true;
                return glyph;
            } finally {
                if (bitmap != null) {
                    STBTruetype.stbtt_FreeBitmap(bitmap);
                }
            }
        }
    }

    public record Glyph(float u0, float v0, float u1, float v1, float xOffset, float yOffset, float width, float height, float advance) {

        private static @NotNull Glyph blank(float advance) {
            return new Glyph(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, advance);
        }
    }
}
