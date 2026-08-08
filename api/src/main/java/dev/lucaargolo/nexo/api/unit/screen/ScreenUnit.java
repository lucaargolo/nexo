package dev.lucaargolo.nexo.api.unit.screen;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public abstract class ScreenUnit<C extends Role> extends Unit<ScreenBase, C> {

    protected ScreenUnit(@NotNull Nexo nexo, @NotNull ScreenBase feature, @Nullable C role) {
        super(nexo, feature, role);
    }

    /**
     * The current mouse position on the screen, in client pixels.
     */
    public abstract @NotNull Vector2f mouse();

    /**
     * The width of the screen, in client pixels.
     */
    public abstract int width();

    /**
     * The height of the screen, in client pixels.
     */
    public abstract int height();

    /**
     * Opens this screen, making it the current screen on the client.
     * Must be called on the render thread.
     */
    public abstract void open();

    /**
     * Closes this screen if it is currently open.
     */
    public abstract void close();
}
