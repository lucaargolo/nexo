package dev.lucaargolo.nexo.api.feature.screen.widget;

import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import org.jetbrains.annotations.NotNull;

public class Button extends Widget {

    private static final float @NotNull [] BASE_COLOR = {0.25F, 0.25F, 0.35F, 0.85F};
    private static final float @NotNull [] HOVER_COLOR = {0.35F, 0.35F, 0.5F, 0.9F};
    private static final float @NotNull [] TEXT_COLOR = {1.0F, 1.0F, 1.0F, 1.0F};

    private @NotNull String text;
    private final @NotNull Runnable action;

    public Button(float x, float y, float width, float height, @NotNull String text, @NotNull Runnable action) {
        super(x, y, width, height);
        this.text = text;
        this.action = action;
    }

    public void text(@NotNull String text) {
        this.text = text;
    }

    public @NotNull String text() {
        return text;
    }

    @Override
    public void render(@NotNull Graphics2D graphics, @NotNull ScreenUnit<?> screen) {
        boolean hovered = contains(screen.mouse().x(), screen.mouse().y());
        graphics.pushState();
        graphics.pushMatrix();
        graphics.translate(x(), y());
        graphics.color(hovered ? HOVER_COLOR : BASE_COLOR);
        graphics.fillRoundedRect(0.0F, 0.0F, width(), height(), 4.0F);
        graphics.color(TEXT_COLOR);
        graphics.drawRoundedRect(0.0F, 0.0F, width(), height(), 4.0F);
        graphics.drawText(text, 8.0F, Math.round((height() - graphics.fontSize()) * 0.5F));
        graphics.popMatrix();
        graphics.popState();
    }

    @Override
    public boolean onInputPressed(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        if (input.type() == Input.Type.MOUSE && contains(screen.mouse().x(), screen.mouse().y())) {
            action.run();
            return true;
        }
        return false;
    }
}
