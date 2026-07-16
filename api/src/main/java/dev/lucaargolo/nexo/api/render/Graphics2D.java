package dev.lucaargolo.nexo.api.render;

import dev.lucaargolo.nexo.api.render.util.BlendMode;
import dev.lucaargolo.nexo.api.render.util.TextureFilter;
import dev.lucaargolo.nexo.api.render.util.TextureWrap;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public interface Graphics2D {

    void pushMatrix();
    void popMatrix();
    void pushState();
    void popState();

    void translate(float x, float y);
    void rotate(float angle);
    void scale(float x, float y);
    void mulMatrix(@NotNull Matrix4f matrix);
    @NotNull Matrix4f matrix();

    void color(float r, float g, float b, float a);
    void color(float @NotNull [] rgba);
    float @NotNull [] color();

    void blendMode(@NotNull BlendMode mode);
    @NotNull BlendMode blendMode();

    void lineWidth(float width);
    float lineWidth();

    void clip(float x, float y, float width, float height);
    void disableClip();
    void scissor(int x, int y, int width, int height);
    void disableScissor();

    void drawLine(float x1, float y1, float x2, float y2);
    void drawRect(float x, float y, float width, float height);
    void fillRect(float x, float y, float width, float height);
    void drawCircle(float x, float y, float radius);
    void fillCircle(float x, float y, float radius);
    void drawEllipse(float x, float y, float width, float height);
    void fillEllipse(float x, float y, float width, float height);
    void drawRoundedRect(float x, float y, float width, float height, float radius);
    void fillRoundedRect(float x, float y, float width, float height, float radius);
    void drawPolygon(float @NotNull [] x, float @NotNull [] y);
    void fillPolygon(float @NotNull [] x, float @NotNull [] y);
    void drawArc(float x, float y, float radius, float startAngle, float endAngle);
    void fillArc(float x, float y, float radius, float startAngle, float endAngle);

    void bindTexture(@NotNull Location texture);
    void drawTexture(float x, float y, float width, float height);
    void drawTextureRegion(float x, float y, float width, float height,
                           float u0, float v0, float u1, float v1);
    void textureFilter(@NotNull TextureFilter min, @NotNull TextureFilter mag);
    void textureWrap(@NotNull TextureWrap wrapS, @NotNull TextureWrap wrapT);

    void drawText(@NotNull String text, float x, float y);
    void font(@Nullable Location font);
    @Nullable Location font();
    void fontSize(float size);
    float fontSize();
}
