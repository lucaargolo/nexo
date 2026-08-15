package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.screen.widget.Widget;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public abstract class ScreenBase extends Feature<ScreenBase, ScreenUnit<?>> implements Renderer<Graphics2D, ScreenUnit<?>> {

    @NotNull
    private final CopyOnWriteArrayList<@NotNull Widget> widgets = new CopyOnWriteArrayList<>();

    public ScreenBase(@NotNull Location location) {
        super(location);
    }

    public ScreenBase(@NotNull Location location, @NotNull Supplier<Role> role) {
        super(location, role);
    }

    @Override
    public final @NotNull Type<ScreenBase, ScreenUnit<?>> type() {
        return Type.SCREEN;
    }

    @Override
    public void render(@NotNull Graphics2D graphics2D, @NotNull ScreenUnit<?> screenUnit) {
        for (Widget widget : widgets) {
            widget.render(graphics2D, screenUnit);
        }
    }

    public void addWidget(@NotNull Widget widget) {
        widgets.add(widget);
    }

    public void removeWidget(@NotNull Widget widget) {
        widgets.remove(widget);
    }

    public @NotNull List<@NotNull Widget> widgets() {
        return widgets;
    }

    public boolean onInputPressed(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).onInputPressed(screen, input)) {
                return true;
            }
        }
        return false;
    }

    public boolean onInputReleased(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).onInputReleased(screen, input)) {
                return true;
            }
        }
        return false;
    }

    public void onInputMove(@NotNull ScreenUnit<?> screen, @NotNull Input.Axis axis, float delta) {
        for (Widget widget : widgets) {
            widget.onInputMove(screen, axis, delta);
        }
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
