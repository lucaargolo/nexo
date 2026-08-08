package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.RendererProvider;
import dev.lucaargolo.nexo.api.input.Axis;
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

    /**
     * Called when a discrete input is pressed (keyboard keys, mouse buttons, joypad buttons).
     * Return {@code false} to let the underlying platform handle the input (e.g. Esc closes the screen).
     */
    public boolean onInputPressed(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        return false;
    }

    /**
     * Called when a discrete input is released (keyboard keys, mouse buttons, joypad buttons).
     * Return {@code false} to let the underlying platform handle the input.
     */
    public boolean onInputReleased(@NotNull ScreenUnit<?> screen, @NotNull Input input) {
        return false;
    }

    /**
     * Called when an analogic input changes (mouse motion, mouse scrolling, joypad sticks and triggers).
     * The {@code delta} is the change since the last event (e.g. pixels moved, scroll steps, normalized axis change).
     */
    public void onInputMove(@NotNull ScreenUnit<?> screen, @NotNull Axis axis, float delta) {
    }
}
