package dev.lucaargolo.nexo.api.feature.screen;

import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Renderer;
import dev.lucaargolo.nexo.api.unit.screen.ScreenUnit;
import dev.lucaargolo.nexo.api.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleScreen extends ScreenBase {

    private final @Nullable Renderer<Graphics2D, ScreenUnit<?>> renderer;

    public SimpleScreen(@NotNull Location location) {
        this(location, null);
    }

    public SimpleScreen(@NotNull Location location, @Nullable Renderer<Graphics2D, ScreenUnit<?>> renderer) {
        super(location);
        this.renderer = renderer;
    }

    @Override
    public @Nullable Renderer<Graphics2D, ScreenUnit<?>> renderer() {
        return this.renderer;
    }
}
