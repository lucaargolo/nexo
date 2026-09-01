package dev.lucaargolo.nexo.api.unit.screen;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.screen.widget.Widget;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ScreenUnit<C extends Role> extends Unit<ScreenBase, C> {

    @NotNull
    private final CopyOnWriteArrayList<@NotNull Widget> widgets = new CopyOnWriteArrayList<>();

    protected ScreenUnit(@NotNull Nexo nexo, @NotNull ScreenBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

    public abstract @NotNull Vector2f mouse();

    public abstract int width();

    public abstract int height();

    public abstract void open();

    public abstract void close();

    public void render(@NotNull Graphics2D graphics) {
        for (Widget widget : widgets) {
            widget.render(this, graphics);
        }
    }

    public void build(float width, float height) {
        widgets.clear();
        feature.build(this);
    }

    public boolean inputPressed(@NotNull Input input) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).inputPressed(this, input)) {
                return true;
            }
        }
        return false;
    }

    public boolean inputReleased(@NotNull Input input) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).inputReleased(this, input)) {
                return true;
            }
        }
        return false;
    }

    public boolean inputMove(@NotNull Input.Axis axis, float delta) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).inputMove(this, axis, delta)) {
                return true;
            }
        }
        return feature.inputMove(this, axis, delta);
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


}
