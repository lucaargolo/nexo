package dev.lucaargolo.nexo.api.feature.screen.widget;

import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Text;

import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

public class Button extends Widget {

    private static final float @NotNull [] BASE_COLOR = {0.25F, 0.25F, 0.35F, 0.85F};
    private static final float @NotNull [] HOVER_COLOR = {0.35F, 0.35F, 0.5F, 0.9F};
    private static final float @NotNull [] TEXT_COLOR = {1.0F, 1.0F, 1.0F, 1.0F};

    private final @NotNull Runnable action;

    private @NotNull Text text;
    private boolean hovered = false;

    public Button(float x, float y, float width, float height, @NotNull Text text, @NotNull Runnable action) {
        super(x, y, width, height);
        this.text = text;
        this.action = action;
    }

    public @NotNull Text text() {
        return text;
    }

    public void text(@NotNull Text text) {
        this.text = text;
    }

    @Override
    public void render(@NotNull ScreenUnit<?> unit, @NotNull Graphics2D graphics) {
        this.hovered = contains(unit.mouse().x, unit.mouse().y);
        graphics.pushState();
        graphics.pushMatrix();
        graphics.translate(x(), y());
        graphics.color(this.hovered ? HOVER_COLOR : BASE_COLOR);
        graphics.fillRoundedRect(0.0F, 0.0F, width(), height(), 4.0F);
        graphics.color(TEXT_COLOR);
        graphics.drawRoundedRect(0.0F, 0.0F, width(), height(), 4.0F);
        graphics.drawText(text, 8.0F, Math.round((height() - text.maxSize()) * 0.5F));
        graphics.popMatrix();
        graphics.popState();
    }

    @Override
    public boolean inputPressed(@NotNull ScreenUnit<?> unit, @NotNull Input input) {
        if (input.type() == Input.Type.MOUSE && this.hovered) {
            action.run();
            return true;
        }
        return false;
    }

}
