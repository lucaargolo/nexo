package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.RendererProvider;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class ScreenBase extends Feature<ScreenBase, ScreenUnit<?>> implements RendererProvider<Graphics2D, ScreenUnit<?>> {

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


    public boolean onInputPressed(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        return false;
    }


    public boolean onInputReleased(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        return false;
    }


    public void onInputMove(@NotNull ScreenUnit<?> screen, @NotNull Input.Axis axis, float delta) {
    }
}
