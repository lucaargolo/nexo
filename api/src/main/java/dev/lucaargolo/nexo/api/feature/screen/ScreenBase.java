package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.Feature;
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

public abstract class ScreenBase extends Feature<ScreenBase, ScreenUnit<?>> implements Renderer<Graphics2D, ScreenUnit<?>> {

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

    public void build(@NotNull ScreenUnit<?> unit) {

    }

    @Override
    public void render(@NotNull ScreenUnit<?> unit, @NotNull Graphics2D graphics2D) {

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

    @Override
    public @NotNull Transform transform(@NotNull Location location) {
        return new Transform(
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1.0F, 1.0F, 1.0F)
        );
    }


}
