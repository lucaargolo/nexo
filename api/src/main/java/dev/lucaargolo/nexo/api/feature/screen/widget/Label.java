package dev.lucaargolo.nexo.api.feature.screen.widget;

import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;

import org.jetbrains.annotations.NotNull;
public class Label extends Widget {

    private @NotNull String text;
    private final float @NotNull [] color = {1.0F, 1.0F, 1.0F, 1.0F};

    public Label(float x, float y, @NotNull String text) {
        super(x, y, 0.0F, 0.0F);
        this.text = text;
    }

    @Override
    public void render(@NotNull Graphics2D graphics, @NotNull ScreenUnit<?> screen) {
        Text parsedText = Text.parse(text);
        graphics.pushState();
        graphics.pushMatrix();
        graphics.translate(x(), y());
        graphics.color(color);
        graphics.drawText(parsedText, 0.0F, 0.0F);
        graphics.popMatrix();
        graphics.popState();
    }

    public @NotNull String text() {
        return text;
    }

    public void text(@NotNull String text) {
        this.text = text;
    }

    public void color(float r, float g, float b, float a) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
    }
}
