package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.screen.widget.Widget;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public abstract class ScreenBase extends Feature<ScreenBase, Unit<ScreenBase, ?>> implements Renderer<Graphics2D, Vector2f> {

    @NotNull
    private final CopyOnWriteArrayList<@NotNull Widget> widgets = new CopyOnWriteArrayList<>();

    private float width;
    private float height;

    public ScreenBase(@NotNull Location location) {
        super(location);
    }

    public ScreenBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<ScreenBase, Unit<ScreenBase, ?>> type() {
        return Type.SCREEN;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    @Override
    public void render(@NotNull Graphics2D graphics2D, @NotNull Vector2f mouse) {
        for (Widget widget : widgets) {
            widget.render(graphics2D, mouse);
        }
    }

    public @NotNull List<@NotNull Widget> widgets() {
        return widgets;
    }

    public void addWidget(@NotNull Widget widget) {
        widgets.add(widget);
    }

    public void removeWidget(@NotNull Widget widget) {
        widgets.remove(widget);
    }

    public void build(float width, float height) {
        this.width = width;
        this.height = height;
        widgets.clear();
    }

    public boolean inputPressed(@NotNull Input input) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).inputPressed(input)) {
                return true;
            }
        }
        return false;
    }

    public boolean inputReleased(@NotNull Input input) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).inputReleased(input)) {
                return true;
            }
        }
        return false;
    }

    public boolean inputMove(@NotNull Input.Axis axis, float delta) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).inputMove(axis, delta)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Transform transform(@NotNull Location location) {
        return new Transform(
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1.0F, 1.0F, 1.0F)
        );
    }


}
