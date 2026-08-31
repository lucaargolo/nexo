package dev.lucaargolo.nexo.api.feature.screen.widget;

import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Text;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;

import org.jetbrains.annotations.NotNull;
public class Label extends Widget<ScreenBase> {

    private @NotNull Text text;
    private final float @NotNull [] color = {1.0F, 1.0F, 1.0F, 1.0F};

    public Label(@NotNull ScreenBase parent, float x, float y, @NotNull Text text) {
        super(parent, x, y, 0.0F, 0.0F);
        this.text = text;
    }

    @Override
    public void render(@NotNull Graphics2D graphics, @NotNull ScreenUnit<?> screen) {
        graphics.pushState();
        graphics.pushMatrix();
        graphics.translate(x(), y());
        graphics.color(color);
        graphics.drawText(text, 0.0F, 0.0F);
        graphics.popMatrix();
        graphics.popState();
    }

    public @NotNull Text text() {
        return text;
    }

    public void text(@NotNull Text text) {
        this.text = text;
    }

    public void color(float r, float g, float b, float a) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
    }
}
