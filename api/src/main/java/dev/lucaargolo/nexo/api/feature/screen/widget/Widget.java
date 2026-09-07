package dev.lucaargolo.nexo.api.feature.screen.widget;

import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import org.jetbrains.annotations.NotNull;

public abstract class Widget {

    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public Widget(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public void render(@NotNull ScreenUnit<?> unit, @NotNull Graphics2D graphics) {
    }

    public boolean inputPressed(@NotNull ScreenUnit<?> unit, @NotNull Input input) {
        return false;
    }

    public boolean inputReleased(@NotNull ScreenUnit<?> unit, @NotNull Input input) {
        return false;
    }

    public boolean inputMove(@NotNull ScreenUnit<?> unit, @NotNull Input.Axis axis, float delta) {
        return false;
    }
}
