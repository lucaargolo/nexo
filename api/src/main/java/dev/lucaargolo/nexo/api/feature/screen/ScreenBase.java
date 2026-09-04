package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Supplier;

public abstract class ScreenBase<D> extends Feature<ScreenBase<D>, ScreenUnit<D>> implements Renderer<Graphics2D, ScreenUnit<D>> {

    private final @NotNull DataBase<D> data;

    public ScreenBase(@NotNull DataBase<D> data) {
        this.data = data;
    }

    public ScreenBase(@NotNull Supplier<Role> role, @NotNull DataBase<D> data) {
        super(role);
        this.data = data;
    }

    @Override
    public final @NotNull Type<ScreenBase<D>, ScreenUnit<D>> type() {
        return Type.screen();
    }

    public final @NotNull DataBase<D> data() {
        return data;
    }

    public void build(@NotNull ScreenUnit<D> unit) {

    }

    @Override
    public void render(@NotNull ScreenUnit<D> unit, @NotNull Graphics2D graphics2D) {

    }

    public boolean inputPressed(@NotNull ScreenUnit<D> unit, @NotNull Input input) {
        return false;
    }

    public boolean inputReleased(@NotNull ScreenUnit<D> unit, @NotNull Input input) {
        return false;
    }

    public boolean inputMove(@NotNull ScreenUnit<D> unit, @NotNull Input.Axis axis, float delta) {
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
