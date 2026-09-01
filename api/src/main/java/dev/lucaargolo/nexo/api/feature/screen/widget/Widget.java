package dev.lucaargolo.nexo.api.feature.screen.widget;

import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

public abstract class Widget {

    @NotNull
    private final ScreenBase parent;

    private float x;
    private float y;
    private final float width;
    private final float height;

    public Widget(@NotNull ScreenBase parent, float x, float y, float width, float height) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public @NotNull ScreenBase parent() {
        return parent;
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

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public void render(@NotNull Graphics2D graphics, @NotNull Vector2f mouse) {
    }

    public boolean inputPressed(@NotNull Input input) {
        return false;
    }

    public boolean inputReleased(@NotNull Input input) {
        return false;
    }

    public boolean inputMove(@NotNull Input.Axis axis, float delta) {
        return false;
    }
}
